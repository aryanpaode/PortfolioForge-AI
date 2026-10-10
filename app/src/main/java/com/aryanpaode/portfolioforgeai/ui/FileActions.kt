package com.aryanpaode.portfolioforgeai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.service.ShareService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileActions(path: String, onMessage: (String) -> Unit, allowWhatsApp: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = File(path)
    val mime = if (file.extension.equals("pdf", true)) "application/pdf" else "text/html"
    var saving by remember { mutableStateOf(false) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri ->
        if (uri != null) scope.launch {
            saving = true
            try {
                withContext(Dispatchers.IO) {
                    file.inputStream().use { input ->
                        requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "The selected location could not be opened." }.use { output -> input.copyTo(output) }
                    }
                }
                onMessage("File saved to your selected location.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { onMessage(e.localizedMessage ?: "The file could not be saved.") }
            finally { saving = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (allowWhatsApp) Button(onClick = { ShareService.shareFile(context, path, whatsapp = true) }, modifier = Modifier.fillMaxWidth()) { Text("Share to WhatsApp") }
        OutlinedButton(onClick = { saver.launch(if (mime == "application/pdf") "PortfolioForge_Resume.pdf" else "PortfolioForge_Portfolio.html") }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else "Save to device") }
        OutlinedButton(onClick = { ShareService.shareFile(context, path) }, modifier = Modifier.fillMaxWidth()) { Text("More apps") }
    }
}
