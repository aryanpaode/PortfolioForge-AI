package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.ui.AppTextField
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun SkillsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val workspace by vm.data.collectAsState()
    val skills = workspace.skills
    var newSkill by remember { mutableStateOf("") }
    val grouped = skills.groupBy { it.category }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Skill Intelligence", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Only skills selected in document review are added. You can add missed skills manually or remove an unsupported suggestion.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(newSkill, { newSkill = it }, "Add skill", modifier = Modifier.weight(1f))
            Button(onClick = { vm.addSkill(newSkill); newSkill = "" }) { Text("Add") }
        }

        grouped.forEach { (category, values) ->
            Text(category, style = MaterialTheme.typography.titleMedium)
            values.forEach { skill ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.name)
                            Text(skill.source, style = MaterialTheme.typography.labelSmall)
                            if (skill.evidence.isNotBlank()) Text(skill.evidence, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.deleteSkill(skill) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
