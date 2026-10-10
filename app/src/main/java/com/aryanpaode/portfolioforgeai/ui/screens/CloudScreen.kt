package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun CloudScreen(vm: AppViewModel, onBack: () -> Unit) {
    val auth by vm.authState.collectAsState()
    val network by vm.networkState.collectAsState()
    val workspace by vm.data.collectAsState()
    var action by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Account & cloud", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(network.displayText)
        if (!auth.configured) FirebaseSetupCard()
        else if (!auth.signedIn) AuthenticationForm(vm)
        else {
            SectionCard("Your account") {
                Text(auth.email.orEmpty())
                Text(if (auth.emailVerified) "Email verified" else "Email verification required", color = if (auth.emailVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                if (!auth.emailVerified) {
                    Text("Open the verification email link, then press the button below. Local work remains available; cloud sync and public publishing require a verified email.")
                    Button(onClick = vm::refreshUser, enabled = network.online, modifier = Modifier.fillMaxWidth()) { Text("I have verified my email") }
                    OutlinedButton(onClick = vm::sendVerification, enabled = network.online, modifier = Modifier.fillMaxWidth()) { Text("Resend verification email") }
                }
                OutlinedButton(onClick = { vm.resetPassword(auth.email.orEmpty()) }, enabled = network.online, modifier = Modifier.fillMaxWidth()) { Text("Send password-reset email") }
                OutlinedButton(onClick = { action = "signout" }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            }
            SectionCard("Private cloud backup") {
                Text("${workspace.items.size} career entries • ${workspace.skills.size} skills • ${workspace.documents.size} documents")
                Button(onClick = { action = "sync" }, enabled = auth.emailVerified && network.online, modifier = Modifier.fillMaxWidth()) { Text("Save current workspace to Firebase") }
                OutlinedButton(onClick = { action = "restore" }, enabled = auth.emailVerified && network.online, modifier = Modifier.fillMaxWidth()) { Text("Restore / merge from Firebase") }
                Text("Sync includes profile details, career entries, approved skills, document metadata and verified text. Original evidence files are not uploaded. Restore merges without deleting your current entries.", style = MaterialTheme.typography.bodySmall)
            }
            SectionCard("Existing local-mode data") {
                Text("Data created before signing in is kept in the local workspace, not automatically assigned to this account. Move it only when it belongs to you.")
                OutlinedButton(onClick = { action = "move" }, modifier = Modifier.fillMaxWidth()) { Text("Move local workspace to this account") }
                Text("The destination account must be empty. A successful move removes the source from signed-out local mode.", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
    action?.let { requested ->
        val title = when (requested) { "sync" -> "Replace this account's cloud snapshot?"; "restore" -> "Restore and merge cloud data?"; "move" -> "Move local data to ${auth.email}?"; else -> "Sign out?" }
        val explanation = when (requested) {
            "sync" -> "This uploads the current account workspace and replaces its previous cloud snapshot. Private document text is included, but files and images stay local."
            "restore" -> "Missing records are added. Existing local values are kept; the original PDFs/images cannot be downloaded because they were never uploaded."
            "move" -> "Only proceed if the local documents/profile belong to you. The account must be empty. Data and evidence files are copied first, then removed from the local workspace."
            else -> "The account workspace will be hidden. Its data stays on this device for your next sign-in. Shared/exported files are not revoked."
        }
        AlertDialog(onDismissRequest = { action = null }, title = { Text(title) }, text = { Text(explanation) },
            confirmButton = { Button(onClick = { action = null; when (requested) { "sync" -> vm.syncCloud(); "restore" -> vm.restoreCloud(); "move" -> vm.moveLocalToAccount(); else -> vm.signOut() } }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { action = null }) { Text("Cancel") } })
    }
}
