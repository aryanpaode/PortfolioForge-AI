package com.aryanpaode.portfolioforgeai.ui.screens

import androidx.compose.runtime.Composable
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel

@Composable
fun SmartImportScreen(vm: AppViewModel, id: String, onBack: () -> Unit) = VerifyDocumentScreen(vm, id, onBack)
