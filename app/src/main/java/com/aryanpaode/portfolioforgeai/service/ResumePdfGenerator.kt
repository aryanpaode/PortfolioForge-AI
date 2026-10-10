package com.aryanpaode.portfolioforgeai.service

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.aryanpaode.portfolioforgeai.data.db.WorkspaceDatabases
import com.aryanpaode.portfolioforgeai.data.model.CareerType
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import java.io.File
import java.util.UUID

private data class PdfLink(val page: Int, val x: Float, val baseline: Float, val width: Float, val height: Float, val url: String)

class ResumePdfGenerator(private val context: Context) {
    fun generate(snapshot: Snapshot): File {
        val p = requireNotNull(snapshot.profile) { "Complete your profile first." }
        require(p.name.isNotBlank()) { "Add your name before generating a résumé." }
        val directory = File(context.filesDir, "exports/${WorkspaceDatabases.folder(snapshot.workspaceId)}").apply { mkdirs() }
        val name = "PortfolioForge_Resume_${UUID.randomUUID().toString().take(8)}"
        val raw = File(directory, "$name.raw.pdf")
        val output = File(directory, "$name.pdf")
        val pdf = PdfDocument()
        val links = mutableListOf<PdfLink>()
        val width = 595; val height = 842; val margin = 45f; val bottom = 784f
        var number = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, number).create())
        var canvas = page.canvas
        var y = 54f
        fun paint(size: Float, bold: Boolean = false, color: Int = Color.rgb(43, 32, 32)) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val titlePaint = paint(28f, true)
        val headingPaint = paint(14f, true, Color.rgb(183, 28, 28))
        val itemPaint = paint(12.5f, true)
        val bodyPaint = paint(12f)
        val metaPaint = paint(11f, false, Color.rgb(101, 79, 79))
        val linkPaint = paint(11f, false, Color.rgb(150, 22, 22))
        val footerPaint = paint(9f, false, Color.GRAY)
        fun finishPage() {
            canvas.drawText("${p.name.take(45)}  ·  $number", margin, 813f, footerPaint)
            pdf.finishPage(page)
        }
        fun ensureSpace(amount: Float) {
            if (y + amount > bottom) {
                finishPage(); number++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, number).create()); canvas = page.canvas; y = 54f
            }
        }
        fun wrapped(value: String, style: Paint = bodyPaint, lineHeight: Float = 18f, href: String? = null) {
            val maxWidth = width - 2 * margin
            value.lines().forEach { paragraph ->
                var remaining = paragraph.trim()
                if (remaining.isBlank()) { y += lineHeight / 2; return@forEach }
                while (remaining.isNotEmpty()) {
                    var count = style.breakText(remaining, true, maxWidth, null).coerceAtLeast(1).coerceAtMost(remaining.length)
                    if (count < remaining.length) {
                        val space = remaining.lastIndexOf(' ', count - 1)
                        if (space > count / 3) count = space
                    }
                    val line = remaining.substring(0, count).trimEnd()
                    ensureSpace(lineHeight)
                    canvas.drawText(line, margin, y, style)
                    if (href != null) links += PdfLink(number - 1, margin, y, style.measureText(line), style.textSize, href)
                    y += lineHeight
                    remaining = remaining.substring(count).trimStart()
                }
            }
        }
        fun section(title: String) {
            ensureSpace(70f); y += 12f
            wrapped(title.uppercase(), headingPaint, 20f)
            val line = Paint().apply { color = Color.rgb(183, 28, 28); strokeWidth = 1f }
            canvas.drawLine(margin, y - 8f, width - margin, y - 8f, line); y += 9f
        }
        fun drawLink(rawUrl: String) {
            val safe = UrlPolicy.normalize(rawUrl) ?: return
            wrapped(safe, linkPaint, 15f, safe)
        }
        try {
            wrapped(p.name, titlePaint, 34f)
            if (p.targetRole.isNotBlank()) wrapped(p.targetRole, headingPaint, 20f)
            val contact = listOf(p.email, p.phone).filter { it.isNotBlank() }.joinToString("  |  ")
            if (contact.isNotBlank()) wrapped(contact, metaPaint, 16f)
            listOf(p.githubUrl, p.linkedInUrl, p.portfolioUrl).filter { it.isNotBlank() }.forEach(::drawLink)
            val academic = listOf(p.branch, p.semester.takeIf { it.isNotBlank() }?.let { "Semester $it" }.orEmpty(), p.cgpa.takeIf { it.isNotBlank() }?.let { "CGPA $it" }.orEmpty()).filter { it.isNotBlank() }.joinToString("  |  ")
            if (academic.isNotBlank()) wrapped(academic, metaPaint, 16f)
            if (p.summary.isNotBlank()) { section("Profile"); wrapped(p.summary) }
            if (snapshot.skills.isNotEmpty()) {
                section("Skills")
                snapshot.skills.groupBy { it.category }.forEach { (category, values) -> wrapped("$category: ${values.joinToString(", ") { it.name }}") }
            }
            val order = CareerType.values().map { it.label } + snapshot.items.map { it.type }.distinct().filter { t -> CareerType.values().none { it.label == t } }
            order.forEach { type ->
                val items = snapshot.items.filter { it.type == type }
                if (items.isNotEmpty()) {
                    section(CareerType.heading(type))
                    items.forEach { item ->
                        ensureSpace(70f)
                        wrapped(item.title, itemPaint, 18f)
                        val meta = listOf(item.subtitle, item.date).filter { it.isNotBlank() }.joinToString("  |  ")
                        if (meta.isNotBlank()) wrapped(meta, metaPaint, 16f)
                        if (item.description.isNotBlank()) wrapped(item.description)
                        if (item.link.isNotBlank()) drawLink(item.link)
                        y += 10f
                    }
                }
            }
            finishPage()
            raw.outputStream().use { pdf.writeTo(it) }
        } finally { pdf.close() }
        // Add genuine PDF link annotations; printing URLs alone does not guarantee clickability.
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(raw).use { document ->
                links.forEach { item ->
                    val annotation = PDAnnotationLink().apply {
                        rectangle = PDRectangle(item.x, height - item.baseline - 3f, item.width.coerceAtLeast(1f), item.height + 5f)
                        action = PDActionURI().apply { uri = item.url }
                        borderStyle = PDBorderStyleDictionary().apply { setWidth(0f) }
                    }
                    document.getPage(item.page).annotations.add(annotation)
                }
                document.save(output)
            }
        } catch (e: Exception) { output.delete(); throw IllegalStateException("PDF link generation failed. No incomplete PDF was shared. ${e.localizedMessage}", e) }
        finally { raw.delete() }
        return output
    }
}
