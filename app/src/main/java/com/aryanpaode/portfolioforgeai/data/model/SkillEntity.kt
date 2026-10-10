package com.aryanpaode.portfolioforgeai.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "skills", indices = [Index(value = ["name"], unique = true)])
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val source: String = "Manual",
    @ColumnInfo(defaultValue = "''") val evidence: String = ""
)
