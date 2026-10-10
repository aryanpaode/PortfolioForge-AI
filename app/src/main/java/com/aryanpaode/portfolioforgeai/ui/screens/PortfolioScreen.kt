package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.service.ShareService
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun PortfolioScreen(vm: AppViewModel, onBack: () -> Unit) {
    val workspace by vm.data.collectAsState()
    val auth by vm.authState.collectAsState()
    val html by vm.htmlPath.collectAsState()
    val pdf by vm.pdfPath.collectAsState()
    val link by vm.publishedLink.collectAsState()
    val network by vm.networkState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var publish by remember { mutableStateOf(false) }
    var unpublish by remember { mutableStateOf(false) }
    var includeEmail by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Portfolio & sharing", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        SectionCard("Local portfolio file") {
            Button(onClick = vm::generatePortfolioHtml, modifier = Modifier.fillMaxWidth()) { Text("Generate portfolio HTML") }
            html?.let { FileActions(it, vm::showMessage, allowWhatsApp = false) }
            Text("HTML is a file, not a public link. Some messaging apps cannot preview HTML; use the PDF below or a published HTTPS link for WhatsApp.", style = MaterialTheme.typography.bodySmall)
        }
        SectionCard("Easiest WhatsApp option: PDF") {
            Button(onClick = vm::generateResumePdf, modifier = Modifier.fillMaxWidth()) { Text("Generate profile as resume PDF") }
            pdf?.let { FileActions(it, vm::showMessage) }
        }
        SectionCard("Public website link (optional)") {
            Text("Uses your Firebase project and the included Hosting site. Gmail/Drive linking is not required.")
            Button(onClick = { publish = true }, enabled = auth.signedIn && auth.emailVerified && network.online, modifier = Modifier.fillMaxWidth()) { Text("Publish / update public portfolio") }
            if (!auth.configured) Text("Add google-services.json first. PDF/HTML export still works.")
            else if (!auth.signedIn) Text("Sign in from Account & cloud first.")
            else if (!auth.emailVerified) Text("Verify your email in Account & cloud first.")
            if (auth.signedIn) {
                OutlinedButton(onClick = vm::revealPortfolioLink, modifier = Modifier.fillMaxWidth()) { Text("Show account's portfolio link") }
                OutlinedButton(onClick = { unpublish = true }, enabled = auth.emailVerified && network.online, modifier = Modifier.fillMaxWidth()) { Text("Unpublish current portfolio") }
            }
            link?.let { value ->
                Text(value, style = MaterialTheme.typography.bodySmall)
                Text("The link works only after publishing data AND deploying hosting/public to Firebase Hosting.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { ShareService.shareLink(context, value, whatsapp = true) }, modifier = Modifier.fillMaxWidth()) { Text("Share link to WhatsApp") }
                OutlinedButton(onClick = { openUrl(context, value) }, modifier = Modifier.fillMaxWidth()) { Text("Open portfolio link") }
                TextButton(onClick = { clipboard.setText(AnnotatedString(value)); vm.showMessage("Portfolio link copied.") }) { Text("Copy link") }
            }
        }
        ProfilePreview(workspace)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
    if (publish) AlertDialog(onDismissRequest = { publish = false }, title = { Text("Make your portfolio public?") }, text = {
        Column {
            Text("Your name, profile summary, academic details, links, selected skills and career entries will be readable by anyone with the link. Raw evidence, private OCR text and phone number are not published.")
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeEmail, { includeEmail = it }); Text("Also publish my profile email") }
        }
    }, confirmButton = { Button(onClick = { publish = false; vm.publishPortfolio(includeEmail) }) { Text("Publish") } }, dismissButton = { TextButton(onClick = { publish = false }) { Text("Cancel") } })
    if (unpublish) AlertDialog(onDismissRequest = { unpublish = false }, title = { Text("Unpublish this portfolio?") }, text = { Text("This removes the current public record. It cannot recall PDFs/HTML files or screenshots you already shared. Older links created by previous app versions may need to be removed separately in Firebase.") },
        confirmButton = { Button(onClick = { unpublish = false; vm.unpublishPortfolio() }) { Text("Unpublish") } }, dismissButton = { TextButton(onClick = { unpublish = false }) { Text("Cancel") } })
}
