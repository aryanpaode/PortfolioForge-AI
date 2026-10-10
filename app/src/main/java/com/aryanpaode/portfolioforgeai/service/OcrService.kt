package com.aryanpaode.portfolioforgeai.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class OcrResult(val text: String, val warning: String = "", val pagesRead: Int = 1, val totalPages: Int = 1)

/** Text-layer extraction first; bundled ML Kit OCR is the fallback for scanned pages/images. */
class OcrService(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    init { PDFBoxResourceLoader.init(context.applicationContext) }

    suspend fun extractText(file: File, mimeType: String): String = extract(file, mimeType).text

    suspend fun extract(file: File, mimeType: String, forceOcr: Boolean = false): OcrResult = withContext(Dispatchers.IO) {
        require(file.isFile) { "The original document is not available on this device. Upload it again or edit its saved text." }
        if (mimeType.equals("application/pdf", true) || file.extension.equals("pdf", true)) extractPdf(file, forceOcr)
        else {
            val text = recognizer.process(InputImage.fromFilePath(context, Uri.fromFile(file))).await().text.trim()
            OcrResult(text, if (text.isBlank()) "No readable text was found. Try a sharper image or enter the text manually." else "")
        }
    }

    private suspend fun extractPdf(file: File, forceOcr: Boolean): OcrResult {
        val warnings = mutableListOf<String>()
        // A failed text layer still permits local raster OCR; errors are shown, not silently swallowed.
        val pdf = if (forceOcr) null else try { PDDocument.load(file) } catch (e: Exception) { null }
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val total = renderer.pageCount
                    val count = minOf(total, MAX_PAGES)
                    if (total > count) warnings += "Read $count of $total pages (safety limit). Split this PDF to import the remaining pages; they have NOT been analyzed."
                    val output = mutableListOf<String>()
                    for (index in 0 until count) {
                        currentCoroutineContext().ensureActive()
                        val layer = if (pdf == null) "" else try {
                            PDFTextStripper().apply { startPage = index + 1; endPage = index + 1; sortByPosition = true }.getText(pdf).trim()
                        } catch (e: Exception) { "" }
                        // A very small/noisy text layer often contains only a logo or footer.
                        var pageText = layer
                        if (forceOcr || layer.count { it.isLetterOrDigit() } < 50) {
                            renderer.openPage(index).use { page ->
                                val scale = minOf(2.5f, 2200f / page.width.coerceAtLeast(1), 3000f / page.height.coerceAtLeast(1))
                                val bitmap = Bitmap.createBitmap((page.width * scale).roundToInt().coerceAtLeast(1), (page.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                                try {
                                    bitmap.eraseColor(Color.WHITE)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val recognized = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
                                    pageText = if (recognized.isNotBlank()) recognized else layer
                                } finally { bitmap.recycle() }
                            }
                        }
                        if (pageText.isBlank()) warnings += "Page ${index + 1}: no readable text found."
                        else output += pageText
                    }
                    if (output.isEmpty()) warnings += "Nothing was extracted. The PDF may be password-protected, damaged or too blurry."
                    return OcrResult(output.joinToString("\n\n"), warnings.joinToString("\n"), count, total)
                }
            }
        } finally { pdf?.close() }
    }
    companion object { const val MAX_PAGES = 60 }
}
