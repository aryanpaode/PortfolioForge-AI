package com.aryanpaode.portfolioforgeai.service

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** User chooses the recipient and presses Send inside WhatsApp. No email/Drive account is needed. */
object ShareService {
    private const val WHATSAPP = "com.whatsapp"
    private const val BUSINESS = "com.whatsapp.w4b"

    fun shareFile(context: Context, path: String, whatsapp: Boolean = false): Boolean = try {
        val file = File(path).canonicalFile
        val allowed = File(context.filesDir, "exports").canonicalFile
        require(file.isFile && file.path.startsWith(allowed.path + File.separator)) { "Generate the export again before sharing." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension.lowercase()) { "pdf" -> "application/pdf"; "html", "htm" -> "text/html"; else -> "application/octet-stream" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, intent, whatsapp)
    } catch (e: Exception) { message(context, e.localizedMessage ?: "The file could not be shared."); false }

    fun shareLink(context: Context, link: String, whatsapp: Boolean = false): Boolean {
        val safe = UrlPolicy.normalize(link) ?: return false
        return try { launch(context, Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, safe) }, whatsapp) }
        catch (e: Exception) { message(context, "A sharing app could not be opened."); false }
    }

    private fun installed(context: Context, packageName: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) { false }

    private fun launch(context: Context, base: Intent, whatsapp: Boolean): Boolean {
        val launchIntent = if (!whatsapp) Intent.createChooser(base, "Share using") else {
            val targets = listOf(WHATSAPP, BUSINESS).filter { installed(context, it) }
            if (targets.isEmpty()) {
                message(context, "WhatsApp is not installed on this Android device/emulator. Save the file or use More apps; WhatsApp on your PC/phone is a separate installation.")
                return false
            }
            val primary = Intent(base).setPackage(targets.first())
            if (targets.size == 1) primary else Intent.createChooser(primary, "Choose WhatsApp").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(Intent(base).setPackage(targets[1])))
            }
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // The content URI is present in EXTRA_STREAM AND ClipData, with a temporary read grant.
        context.startActivity(launchIntent)
        return true
    }
    private fun message(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
