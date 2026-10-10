package com.aryanpaode.portfolioforgeai.service

import android.content.Context
import com.aryanpaode.portfolioforgeai.data.db.WorkspaceDatabases
import com.aryanpaode.portfolioforgeai.data.repository.Snapshot
import java.io.File
import java.util.UUID

class PortfolioHtmlGenerator(private val context: Context) {
    fun generate(snapshot: Snapshot): File {
        val dir = File(context.filesDir, "exports/${WorkspaceDatabases.folder(snapshot.workspaceId)}").apply { mkdirs() }
        return File(dir, "PortfolioForge_Portfolio_${UUID.randomUUID().toString().take(8)}.html").also { it.writeText(PortfolioRenderer.render(snapshot)) }
    }
}
