package com.aryanpaode.portfolioforgeai.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "career_items")
data class CareerItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val date: String = "",
    val link: String = "",
    @ColumnInfo(defaultValue = "''") val sourceDocumentId: String = ""
)
