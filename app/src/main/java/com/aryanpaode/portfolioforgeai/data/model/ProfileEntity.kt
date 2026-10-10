package com.aryanpaode.portfolioforgeai.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val branch: String = "",
    val semester: String = "",
    val cgpa: String = "",
    val githubUrl: String = "",
    val linkedInUrl: String = "",
    val targetRole: String = "",
    val summary: String = "",
    @ColumnInfo(defaultValue = "''") val portfolioUrl: String = ""
)
