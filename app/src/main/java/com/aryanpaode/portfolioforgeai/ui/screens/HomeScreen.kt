package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.ui.SectionCard
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun HomeScreen(vm: AppViewModel, padding: PaddingValues, onNavigate: (String) -> Unit, routes: Map<String, String>) {
    val workspace by vm.data.collectAsState()
    val profile = workspace.profile
    val docs = workspace.documents
    val items = workspace.items
    val skills = workspace.skills
    val account by vm.authState.collectAsState()
    val readiness by vm.readiness.collectAsState()
    val network by vm.networkState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "PortfolioForge",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Welcome${profile?.name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text("Build from verified data, not scattered files.", color = Color.White.copy(alpha = 0.9f))
                Text(network.displayText, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }

        SectionCard(if (account.signedIn) "Account workspace" else "Local workspace") {
            Text(account.email ?: "Not signed in · stored only on this device")
            if (account.signedIn && !account.emailVerified) Text("Verify your email to enable cloud features.")
            OutlinedButton(onClick = { onNavigate(routes.getValue("cloud")) }, modifier = Modifier.fillMaxWidth()) { Text("Account & verification") }
        }

        SectionCard("Resume readiness: ${readiness.score}%") {
            LinearProgressIndicator(progress = readiness.score / 100f, modifier = Modifier.fillMaxWidth())
            if (readiness.suggestions.isEmpty()) {
                Text("Your profile is ready for generation and publishing.", color = MaterialTheme.colorScheme.primary)
            } else {
                readiness.suggestions.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }

        SectionCard("Your workspace") {
            Text("${docs.size} documents  •  ${skills.size} skills  •  ${items.size} career entries")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onNavigate(routes.getValue("profile")) }) { Text("Profile") }
                OutlinedButton(onClick = { onNavigate(routes.getValue("documents")) }) { Text("Documents") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(routes.getValue("career")) }) { Text("Career details") }
                OutlinedButton(onClick = { onNavigate(routes.getValue("skills")) }) { Text("Skills") }
            }
        }

        OutlinedButton(onClick = { onNavigate(routes.getValue("publications")) }, modifier = Modifier.fillMaxWidth()) { Text("Publications") }
        OutlinedButton(onClick = { onNavigate(routes.getValue("conferences")) }, modifier = Modifier.fillMaxWidth()) { Text("Conferences & seminars") }
        Button(onClick = { onNavigate(routes.getValue("build")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Build resume & portfolio")
        }
    }
}
