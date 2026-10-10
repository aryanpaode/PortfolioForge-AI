package com.aryanpaode.portfolioforgeai.data.db

import android.content.Context
import androidx.room.Room
import java.security.MessageDigest

/** Separate Room files per Firebase UID; the original database remains the local workspace. */
class WorkspaceDatabases(private val context: Context) {
    private val opened = mutableMapOf<String, AppDatabase>()
    @Synchronized
    fun get(workspaceId: String): AppDatabase = opened.getOrPut(workspaceId) {
        val fileName = if (workspaceId == LOCAL) "portfolioforge.db" else "portfolioforge_${folder(workspaceId)}.db"
        Room.databaseBuilder(context, AppDatabase::class.java, fileName)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2).build()
    }
    companion object {
        const val LOCAL = "local"
        fun folder(workspaceId: String): String = if (workspaceId == LOCAL) LOCAL else
            MessageDigest.getInstance("SHA-256").digest(workspaceId.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
