package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.data.model.*
import com.aryanpaode.portfolioforgeai.service.*
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel
import java.util.UUID

@Composable
fun VerifyDocumentScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val workspace by vm.data.collectAsState()
    val doc = workspace.documents.firstOrNull { it.id == id }
    if (doc == null) { Column(Modifier.padding(16.dp)) { Text("Document not found in this workspace."); OutlinedButton(onClick = onBack) { Text("Back") } }; return }
    var text by remember(doc.id, doc.verifiedText) { mutableStateOf(doc.verifiedText.ifBlank { doc.extractedText }) }
    var category by remember(doc.id) { mutableStateOf("Auto detect") }
    var review by remember(doc.id, doc.verifiedText) { mutableStateOf(vm.previewDocument(doc.id, text, doc.fileName, DocumentClassifier().classify(text, doc.fileName, doc.category))) }
    val previous = remember(doc.id, workspace.items) { workspace.items.filter { it.sourceDocumentId == doc.id } }
    // Initial review retains prior user edits. Re-extraction replaces only the draft after explicit confirmation.
    var entries by remember(doc.id, doc.verifiedText) { mutableStateOf(if (previous.isNotEmpty()) previous else review.items) }
    var selectedItems by remember(doc.id, doc.verifiedText) { mutableStateOf(entries.map { it.id }.toSet()) }
    var selectedSkills by remember(doc.id, doc.verifiedText) { mutableStateOf(if (review.skillsAreTopics) emptySet<String>() else review.skills.map { it.id }.toSet()) }
    var skills by remember(doc.id, doc.verifiedText) { mutableStateOf(review.skills) }
    var profileDraft by remember(doc.id, doc.verifiedText) { mutableStateOf(review.profile) }
    var includeProfile by remember(doc.id) { mutableStateOf(false) }
    var newSkill by remember { mutableStateOf("") }
    var showSource by remember { mutableStateOf(false) }
    var askReextract by remember { mutableStateOf(false) }
    var askReread by remember { mutableStateOf(false) }
    val textChanged = text != review.text
    fun reextract() {
        val fresh = vm.previewDocument(doc.id, text, doc.fileName, category)
        review = fresh; entries = fresh.items; skills = fresh.skills; profileDraft = fresh.profile
        selectedItems = fresh.items.map { it.id }.toSet()
        selectedSkills = if (fresh.skillsAreTopics) emptySet() else fresh.skills.map { it.id }.toSet()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review extracted information", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(doc.fileName)
        Text("Detected: ${review.category}. Confirming saves the selected section entries AND selected skills. A section does not need any skills to be saved.")
        if (doc.ocrWarning.isNotBlank()) Text(doc.ocrWarning, color = MaterialTheme.colorScheme.error)
        if (previous.isNotEmpty()) Text("Previously saved entries are shown with your edits. Unchecking an existing entry does not delete it; use Career details to remove it.", style = MaterialTheme.typography.bodySmall)
        ChoicePicker("Re-extract as", category, CareerType.documentCategories) { category = it }
        OutlinedButton(onClick = { askReextract = true }, modifier = Modifier.fillMaxWidth()) { Text("Re-extract fields from text") }
        review.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        entries.forEach { item ->
            key(item.id) {
                SectionCard(CareerType.heading(item.type)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.id in selectedItems, onCheckedChange = { checked -> selectedItems = if (checked) selectedItems + item.id else selectedItems - item.id })
                        Text("Include this entry")
                    }
                    CareerFields(item, { edited ->
                        entries = entries.map { if (it.id == edited.id) edited else it }
                        if (edited.type == "Conference" && item.type != "Conference") selectedSkills = emptySet()
                        review = review.copy(skillsAreTopics = entries.any { it.type == "Conference" })
                    })
                }
            }
        }
        if (entries.isEmpty()) {
            SectionCard("Choose a destination") {
                Text("No structured entry was found. You can add one manually without losing the verified document text.")
                Button(onClick = {
                    val item = CareerItemEntity(DocumentExtractionService.stableId(doc.id, entries.size), "Certification", "", sourceDocumentId = doc.id)
                    entries = entries + item; selectedItems = selectedItems + item.id
                }) { Text("Add a section entry") }
            }
        }
        SectionCard(if (review.skillsAreTopics) "Topic / skill suggestions" else "Skills to add (${skills.size})") {
            Text(if (review.skillsAreTopics) "Attendance is not skill proficiency. Select only the skills you can substantiate." else "Select supported skills. The evidence snippet shows why each was suggested.", style = MaterialTheme.typography.bodySmall)
            if (skills.isEmpty()) Text("No skills detected. Your section entry will still be saved.")
            skills.forEach { skill ->
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = skill.id in selectedSkills, onCheckedChange = { checked -> selectedSkills = if (checked) selectedSkills + skill.id else selectedSkills - skill.id })
                    Column(Modifier.weight(1f).padding(top = 10.dp)) {
                        Text(skill.name, style = MaterialTheme.typography.titleSmall)
                        if (skill.evidence.isNotBlank()) Text(skill.evidence, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            AppTextField(newSkill, { newSkill = it }, "Add a missed skill")
            OutlinedButton(onClick = {
                val skill = SkillEntity(UUID.randomUUID().toString(), newSkill.trim(), "Other", "Manual review", "Added manually during document review")
                skills = skills + skill; selectedSkills = selectedSkills + skill.id; newSkill = ""
            }, enabled = newSkill.isNotBlank()) { Text("Include skill") }
        }
        profileDraft?.let { p ->
            SectionCard("Profile suggestions from résumé") {
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeProfile, { includeProfile = it }); Text("Fill empty profile fields only") }
                AppTextField(p.name, { profileDraft = p.copy(name = it) }, "Full name")
                AppTextField(p.email, { profileDraft = p.copy(email = it) }, "Email")
                AppTextField(p.phone, { profileDraft = p.copy(phone = it) }, "Phone")
                AppTextField(p.branch, { profileDraft = p.copy(branch = it) }, "Branch / qualification")
                AppTextField(p.githubUrl, { profileDraft = p.copy(githubUrl = it) }, "GitHub URL", allowPaste = true)
                AppTextField(p.linkedInUrl, { profileDraft = p.copy(linkedInUrl = it) }, "LinkedIn URL", allowPaste = true)
            }
        }
        OutlinedButton(onClick = { showSource = !showSource }, modifier = Modifier.fillMaxWidth()) { Text(if (showSource) "Hide source text" else "View / correct source text") }
        if (showSource) {
            AppTextField(text, { text = it }, "Extracted / corrected text", singleLine = false, minLines = 9)
            if (textChanged) Text("Source text changed. Re-extract fields to refresh suggestions, or keep your manually edited fields when saving.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { askReread = true }, modifier = Modifier.fillMaxWidth()) { Text("Read original file again using OCR") }
        }
        Button(onClick = {
            val selected = entries.filter { it.id in selectedItems }
            val savedCategory = if (review.category in listOf("Resume", "LinkedIn Profile")) review.category else selected.singleOrNull()?.type ?: review.category
            vm.saveDocumentReview(review.copy(text = text, category = savedCategory, skillsAreTopics = selected.any { it.type == "Conference" }), selected, skills.filter { it.id in selectedSkills }, if (includeProfile) profileDraft else null, onBack)
        }, modifier = Modifier.fillMaxWidth()) { Text("Confirm & save selected information") }
        Text("Existing skills and entries are not deleted when unchecked. Cancel leaves the saved profile unchanged.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
    if (askReextract) AlertDialog(onDismissRequest = { askReextract = false }, title = { Text("Replace this review draft?") },
        text = { Text("Re-extraction uses the text and category above. It replaces unsaved draft edits, not previously saved profile records.") },
        confirmButton = { Button(onClick = { reextract(); askReextract = false }) { Text("Re-extract") } },
        dismissButton = { TextButton(onClick = { askReextract = false }) { Text("Cancel") } })
    if (askReread) AlertDialog(onDismissRequest = { askReread = false }, title = { Text("Read the original file again?") },
        text = { Text("This replaces the draft text with fresh OCR and may take time. Approved profile entries remain unchanged until you confirm new edits.") },
        confirmButton = { Button(onClick = { vm.rereadDocument(doc.id); askReread = false }) { Text("Run OCR") } },
        dismissButton = { TextButton(onClick = { askReread = false }) { Text("Cancel") } })
}
