@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.ui.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun AuthScreen(vm: AppViewModel, onOffline: () -> Unit) {
    val auth by vm.authState.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("PortfolioForge AI", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Turn verified career evidence into your résumé and portfolio.")
        if (auth.configured) AuthenticationForm(vm)
        else FirebaseSetupCard()
        OutlinedButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) { Text("Continue in local workspace") }
        Text("Local mode works without an account. Account workspaces are separate; existing local data can be explicitly moved to an empty account after signing in.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FirebaseSetupCard() {
    SectionCard("Firebase configuration required") {
        Text("The authentication code is included, but this installation has no Firebase Android configuration.")
        Text("1. Register com.aryanpaode.portfolioforgeai in your Firebase project.\n2. Put its google-services.json inside app/.\n3. Enable Email/Password under Authentication.\n4. Sync Gradle and run again.")
        Text("See FIREBASE_SETUP.md in the project. Offline extraction and exports remain available.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AuthenticationForm(vm: AppViewModel) {
    var createAccount by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    // Passwords deliberately stay in transient UI state, never a database, preferences or saved-state bundle.
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val network by vm.networkState.collectAsState()
    SectionCard(if (createAccount) "Create account" else "Sign in") {
        AppTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (showPassword) "Hide password" else "Show password") } })
        if (createAccount) {
            OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation())
            Text("At least 6 characters; your Firebase project may require a stronger password. A verification email will be sent.", style = MaterialTheme.typography.bodySmall)
            if (confirm.isNotBlank() && confirm != password) Text("Passwords do not match.", color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = { if (createAccount) vm.register(email, password) else vm.signIn(email, password) },
            enabled = !busy && network.online && email.isNotBlank() && password.isNotEmpty() && (!createAccount || (password.length >= 6 && confirm == password)), modifier = Modifier.fillMaxWidth()) {
            Text(if (createAccount) "Create account & send verification" else "Sign in")
        }
        TextButton(onClick = { createAccount = !createAccount; password = ""; confirm = ""; showPassword = false }) { Text(if (createAccount) "Already registered? Sign in" else "New here? Create account") }
        TextButton(onClick = { vm.resetPassword(email) }, enabled = !busy && network.online && email.isNotBlank()) { Text("Forgot password? Send reset email") }
        if (!network.online) Text("Internet is required for Firebase authentication. Local mode is still available.", color = MaterialTheme.colorScheme.error)
    }
}
