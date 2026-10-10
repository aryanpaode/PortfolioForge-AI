package com.aryanpaode.portfolioforgeai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aryanpaode.portfolioforgeai.ui.PortfolioForgeApp
import com.aryanpaode.portfolioforgeai.ui.theme.PortfolioForgeTheme
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModel
import com.aryanpaode.portfolioforgeai.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as PortfolioForgeApplication
            val vm: AppViewModel = viewModel(factory = AppViewModelFactory(app.container))
            PortfolioForgeTheme {
                PortfolioForgeApp(vm)
            }
        }
    }
}
