package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.ui.SectionCard

@Composable
fun BuildScreen(padding: PaddingValues, onCareer: () -> Unit, onPublications: () -> Unit, onConferences: () -> Unit,
    onSkills: () -> Unit, onResume: () -> Unit, onPortfolio: () -> Unit, onCloud: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Build & share", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        SectionCard("Review your sections") {
            OutlinedButton(onClick = onCareer, modifier = Modifier.fillMaxWidth()) { Text("All career details") }
            OutlinedButton(onClick = onPublications, modifier = Modifier.fillMaxWidth()) { Text("Publications") }
            OutlinedButton(onClick = onConferences, modifier = Modifier.fillMaxWidth()) { Text("Conferences & seminars") }
            OutlinedButton(onClick = onSkills, modifier = Modifier.fillMaxWidth()) { Text("Skills") }
        }
        SectionCard("Generate & share") {
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Resume PDF · WhatsApp · Save") }
            Button(onClick = onPortfolio, modifier = Modifier.fillMaxWidth()) { Text("Portfolio HTML / public link") }
            Text("PDF sharing does not require Firebase, Gmail or Drive. WhatsApp must be installed on the Android device.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onCloud, modifier = Modifier.fillMaxWidth()) { Text("Account, verification & cloud") }
    }
}
