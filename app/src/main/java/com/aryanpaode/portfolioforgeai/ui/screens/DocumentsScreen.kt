package com.aryanpaode.portfolioforgeai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel
import java.io.File

@Composable
fun DocumentsScreen(vm: AppViewModel, padding: PaddingValues, onVerify: (String) -> Unit) {
    val workspace by vm.data.collectAsState()
    val context = LocalContext.current
    var hint by rememberSaveable { mutableStateOf("Auto detect") }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<DocumentEntity?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importDocument(uri, hint, onVerify)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) vm.importDocument(Uri.parse(uri), hint, onVerify)
        else vm.showMessage("Camera capture cancelled.")
        cameraUri = null
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Documents & extraction", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Upload → review section details + skills → confirm. PDFs use their text layer first; scanned pages use local OCR.")
        ChoicePicker("Upload category hint", hint, CareerType.documentCategories) { hint = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(arrayOf("application/pdf", "image/*")) }) { Text("Upload") }
            OutlinedButton(onClick = {
                try {
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    cameraUri = uri.toString(); camera.launch(uri)
                } catch (e: Exception) { vm.showMessage(e.localizedMessage ?: "No camera app is available on this device.") }
            }) { Text("Camera scan") }
        }
        if (workspace.documents.isEmpty()) Text("No documents yet. Existing uploaded certificates can be reviewed again without deleting them.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(workspace.documents, key = { it.id }) { doc ->
                SectionCard(doc.fileName) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.category, color = MaterialTheme.colorScheme.primary)
                            Text(if (doc.importedAt > 0L) "Section entries reviewed" else if (doc.verified) "Text verified — review section details" else "Awaiting review", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { deleting = doc }) { Icon(Icons.Default.Delete, "Delete evidence") }
                    }
                    if (doc.ocrWarning.isNotBlank()) Text(doc.ocrWarning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Text(doc.verifiedText.ifBlank { doc.extractedText }.ifBlank { "No text found. You can enter the details manually." }, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onVerify(doc.id) }, modifier = Modifier.fillMaxWidth()) { Text(if (doc.verified || doc.importedAt > 0L) "Review / reprocess" else "Review & add to profile") }
                }
            }
        }
    }
    deleting?.let { doc ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete evidence document?") },
            text = { Text("${doc.fileName}\nApproved profile entries and skills will be kept. Delete those separately in Career details or Skills.") },
            confirmButton = { Button(onClick = { vm.deleteDocument(doc.id); deleting = null }) { Text("Delete evidence") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
