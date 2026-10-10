package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE career_items ADD COLUMN sourceDocumentId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE documents ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE documents ADD COLUMN ocrWarning TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE documents ADD COLUMN importedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE skills ADD COLUMN evidence TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE profile ADD COLUMN portfolioUrl TEXT NOT NULL DEFAULT ''")
        }
    }
}
