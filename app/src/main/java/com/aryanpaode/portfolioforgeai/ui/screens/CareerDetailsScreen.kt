package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel
import java.util.UUID

@Composable
fun CareerDetailsScreen(vm: AppViewModel, onBack: () -> Unit, initialType: String = "All") {
    val workspace by vm.data.collectAsState()
    var filter by remember { mutableStateOf(initialType) }
    var editing by remember { mutableStateOf<CareerItemEntity?>(null) }
    var deleting by remember { mutableStateOf<CareerItemEntity?>(null) }
    val context = LocalContext.current
    val itemsState = workspace.items.filter { filter == "All" || it.type == filter }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (filter == "All") "Career details" else CareerType.heading(filter), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        ChoicePicker("Show", filter, listOf("All") + CareerType.values().map { it.label }) { filter = it }
        Button(onClick = { editing = CareerItemEntity(UUID.randomUUID().toString(), if (filter == "All") "Project" else filter, "") }, modifier = Modifier.fillMaxWidth()) { Text(if (filter == "Publication") "Add publication" else if (filter == "Conference") "Add conference / seminar" else "Add entry") }
        if (itemsState.isEmpty()) Text("No entries in this section yet. Add one here or review an uploaded document.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(itemsState, key = { it.id }) { item ->
                SectionCard(item.title) {
                    Text(CareerType.heading(item.type), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    if (item.subtitle.isNotBlank()) Text(item.subtitle)
                    if (item.date.isNotBlank()) Text(item.date)
                    if (item.description.isNotBlank()) Text(item.description)
                    if (item.link.isNotBlank()) TextButton(onClick = { openUrl(context, item.link) }) { Text("Open ${if (item.type == "Publication") "publication" else "link"}") }
                    Row {
                        OutlinedButton(onClick = { editing = item }) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit") }
                        Spacer(Modifier.width(10.dp))
                        IconButton(onClick = { deleting = item }) { Icon(Icons.Default.Delete, "Delete entry") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
    editing?.let { initial ->
        var draft by remember(initial.id) { mutableStateOf(initial) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text(if (workspace.items.any { it.id == initial.id }) "Edit entry" else "Add entry") },
            text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) { CareerFields(draft, { draft = it }) } },
            confirmButton = { Button(onClick = { vm.saveCareerItem(draft) { editing = null } }, enabled = draft.title.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this entry?") }, text = { Text(item.title + "\nThe uploaded evidence document and approved skills will remain.") },
            confirmButton = { Button(onClick = { vm.deleteCareerItem(item); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
