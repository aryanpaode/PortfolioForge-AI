package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.ui.AppTextField
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun ProfileScreen(vm: AppViewModel, padding: PaddingValues) {
    val workspace by vm.data.collectAsState()
    val stored = workspace.profile
    var name by remember(stored?.name) { mutableStateOf(stored?.name.orEmpty()) }
    var email by remember(stored?.email) { mutableStateOf(stored?.email.orEmpty()) }
    var phone by remember(stored?.phone) { mutableStateOf(stored?.phone.orEmpty()) }
    var branch by remember(stored?.branch) { mutableStateOf(stored?.branch.orEmpty()) }
    var semester by remember(stored?.semester) { mutableStateOf(stored?.semester.orEmpty()) }
    var cgpa by remember(stored?.cgpa) { mutableStateOf(stored?.cgpa.orEmpty()) }
    var github by remember(stored?.githubUrl) { mutableStateOf(stored?.githubUrl.orEmpty()) }
    var linkedin by remember(stored?.linkedInUrl) { mutableStateOf(stored?.linkedInUrl.orEmpty()) }
    var portfolio by remember(stored?.portfolioUrl) { mutableStateOf(stored?.portfolioUrl.orEmpty()) }
    var role by remember(stored?.targetRole) { mutableStateOf(stored?.targetRole.orEmpty()) }
    var summary by remember(stored?.summary) { mutableStateOf(stored?.summary.orEmpty()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Student profile", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("Academic fields are entered manually so the final resume uses information you explicitly verify.")
        AppTextField(name, { name = it }, "Full name")
        AppTextField(email, { email = it }, "Email")
        AppTextField(phone, { phone = it }, "Phone")
        AppTextField(branch, { branch = it }, "Branch / Program")
        AppTextField(semester, { semester = it }, "Semester")
        AppTextField(cgpa, { cgpa = it }, "CGPA")
        AppTextField(github, { github = it }, "GitHub URL", allowPaste = true)
        AppTextField(linkedin, { linkedin = it }, "LinkedIn URL", allowPaste = true)
        AppTextField(portfolio, { portfolio = it }, "Portfolio / website URL (optional)", allowPaste = true)
        AppTextField(role, { role = it }, "Target role")
        AppTextField(summary, { summary = it }, "Professional summary", singleLine = false, minLines = 4)
        Button(
            onClick = {
                vm.saveProfile(ProfileEntity(
                    name = name.trim(), email = email.trim(), phone = phone.trim(), branch = branch.trim(), semester = semester.trim(),
                    cgpa = cgpa.trim(), githubUrl = github.trim(), linkedInUrl = linkedin.trim(), targetRole = role.trim(), summary = summary.trim(), portfolioUrl = portfolio.trim()
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save profile") }
    }
}
