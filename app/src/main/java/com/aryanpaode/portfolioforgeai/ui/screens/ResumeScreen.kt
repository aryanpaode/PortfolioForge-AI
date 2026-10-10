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
fun ResumeScreen(vm: AppViewModel, onBack: () -> Unit) {
    val workspace by vm.data.collectAsState()
    val pdf by vm.pdfPath.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Resume PDF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("All populated sections appear independently. Review the content, generate the PDF, then share it directly.")
        Button(onClick = vm::generateResumePdf, modifier = Modifier.fillMaxWidth()) { Text("Generate current resume PDF") }
        pdf?.let { SectionCard("Your PDF is ready") { FileActions(it, vm::showMessage); Text("WhatsApp must be installed on this Android device. You choose the chat and press Send in WhatsApp.", style = MaterialTheme.typography.bodySmall) } }
        ProfilePreview(workspace)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
