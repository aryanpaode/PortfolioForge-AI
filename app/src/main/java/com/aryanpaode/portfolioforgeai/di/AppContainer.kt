package com.aryanpaode.portfolioforgeai.di

import android.content.Context
import com.aryanpaode.portfolioforgeai.data.db.WorkspaceDatabases
import com.aryanpaode.portfolioforgeai.data.repository.LocalRepository
import com.aryanpaode.portfolioforgeai.service.*

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val databases = WorkspaceDatabases(appContext)
    val cloudService = CloudService(appContext)
    val skillIntelligence = SkillIntelligence()
    val classifier = DocumentClassifier()
    val ocrService = OcrService(appContext)
    val structuredImportService = StructuredImportService(skillIntelligence)
    val documentExtraction = DocumentExtractionService(skillIntelligence, classifier, structuredImportService)
    val networkMonitor = NetworkMonitor(appContext)
    val localRepository = LocalRepository(appContext, databases, cloudService.state, ocrService, classifier, skillIntelligence)
    val readinessEngine = ReadinessEngine()
    val resumePdfGenerator = ResumePdfGenerator(appContext)
    val portfolioHtmlGenerator = PortfolioHtmlGenerator(appContext)
}
