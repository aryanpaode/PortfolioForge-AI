package com.aryanpaode.portfolioforgeai.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.aryanpaode.portfolioforgeai.data.model.CareerType
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot

@Composable
fun ProfilePreview(snapshot: Snapshot) {
    val p = snapshot.profile
    val context = LocalContext.current
    SectionCard("Preview") {
        Text(p?.name?.ifBlank { "Add your name in Profile" } ?: "Add your name in Profile", style = MaterialTheme.typography.headlineSmall)
        if (!p?.targetRole.isNullOrBlank()) Text(p?.targetRole.orEmpty(), style = MaterialTheme.typography.titleMedium)
        if (!p?.summary.isNullOrBlank()) Text(p?.summary.orEmpty())
        val academic = listOf(p?.branch.orEmpty(), p?.semester?.takeIf { it.isNotBlank() }?.let { "Semester $it" }.orEmpty(), p?.cgpa?.takeIf { it.isNotBlank() }?.let { "CGPA $it" }.orEmpty()).filter { it.isNotBlank() }
        if (academic.isNotEmpty()) Text(academic.joinToString(" · "))
        if (snapshot.skills.isNotEmpty()) {
            Text("Skills", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            snapshot.skills.groupBy { it.category }.forEach { (group, values) -> Text("$group: ${values.joinToString(", ") { it.name }}") }
        }
        CareerType.values().forEach { type ->
            val entries = snapshot.items.filter { it.type == type.label }
            if (entries.isNotEmpty()) {
                Text(type.heading, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                entries.forEach { entry ->
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    if (entry.subtitle.isNotBlank()) Text(entry.subtitle)
                    if (entry.date.isNotBlank()) Text(entry.date, style = MaterialTheme.typography.labelMedium)
                    if (entry.description.isNotBlank()) Text(entry.description)
                    if (entry.link.isNotBlank()) TextButton(onClick = { openUrl(context, entry.link) }) { Text(if (type == CareerType.PUBLICATION) "Open publication" else "Open link") }
                }
            }
        }
    }
}
