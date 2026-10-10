package com.aryanpaode.portfolioforgeai

import android.app.Application
import com.aryanpaode.portfolioforgeai.di.AppContainer

class PortfolioForgeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
