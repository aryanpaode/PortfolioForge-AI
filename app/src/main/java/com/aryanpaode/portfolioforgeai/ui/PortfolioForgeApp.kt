@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aryanpaode.portfolioforgeai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.*
import com.aryanpaode.portfolioforgeai.ui.screens.*
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun PortfolioForgeApp(vm: AppViewModel) {
    val account by vm.authState.collectAsState()
    val workspace by vm.data.collectAsState()
    val busy by vm.busy.collectAsState()
    val busyText by vm.busyText.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    // Disposing this tree clears navigation, text fields and previews belonging to the previous account.
    key(account.workspaceId) {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val destinations = listOf(BottomDestination("home", "Home", Icons.Default.Home), BottomDestination("profile", "Profile", Icons.Default.AccountCircle),
            BottomDestination("documents", "Docs", Icons.Default.Description), BottomDestination("build", "Build", Icons.Default.Build))
        Scaffold(modifier = Modifier.systemBarsPadding().imePadding(), snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (route in destinations.map { it.route }) NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(selected = route == destination.route, onClick = {
                            nav.navigate(destination.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                        }, icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) })
                    }
                }
            }) { padding ->
            if (!workspace.loaded || workspace.workspaceId != account.workspaceId) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else NavHost(navController = nav, startDestination = if (account.signedIn) "home" else "auth", modifier = Modifier.fillMaxSize().padding(padding)) {
                composable("auth") { AuthScreen(vm) { nav.navigate("home") { popUpTo("auth") { inclusive = true } } } }
                composable("home") { HomeScreen(vm, PaddingValues(0.dp), { nav.navigate(it) }, mapOf("profile" to "profile", "documents" to "documents", "career" to "career", "skills" to "skills", "build" to "build", "cloud" to "cloud", "publications" to "publications", "conferences" to "conferences")) }
                composable("profile") { ProfileScreen(vm, PaddingValues(0.dp)) }
                composable("documents") { DocumentsScreen(vm, PaddingValues(0.dp)) { nav.navigate("verify/$it") } }
                composable("verify/{id}") { VerifyDocumentScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() } }
                composable("smartImport/{id}") { SmartImportScreen(vm, it.arguments?.getString("id").orEmpty()) { nav.popBackStack() } }
                composable("career") { CareerDetailsScreen(vm, { nav.popBackStack() }) }
                composable("publications") { CareerDetailsScreen(vm, { nav.popBackStack() }, "Publication") }
                composable("conferences") { CareerDetailsScreen(vm, { nav.popBackStack() }, "Conference") }
                composable("skills") { SkillsScreen(vm) { nav.popBackStack() } }
                composable("build") { BuildScreen(PaddingValues(0.dp), { nav.navigate("career") }, { nav.navigate("publications") }, { nav.navigate("conferences") },
                    { nav.navigate("skills") }, { nav.navigate("resume") }, { nav.navigate("portfolio") }, { nav.navigate("cloud") }) }
                composable("resume") { ResumeScreen(vm) { nav.popBackStack() } }
                composable("portfolio") { PortfolioScreen(vm) { nav.popBackStack() } }
                composable("cloud") { CloudScreen(vm) { nav.popBackStack() } }
            }
        }
    }
    if (busy) Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(); Text(busyText)
            }
        }
    }
}
