package com.aryanpaode.portfolioforgeai.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val localPath: String,
    val mimeType: String,
    val category: String = "Other",
    val extractedText: String = "",
    val verifiedText: String = "",
    val verified: Boolean = false,
    val cloudUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val contentHash: String = "",
    @ColumnInfo(defaultValue = "''") val ocrWarning: String = "",
    @ColumnInfo(defaultValue = "0") val importedAt: Long = 0
)
