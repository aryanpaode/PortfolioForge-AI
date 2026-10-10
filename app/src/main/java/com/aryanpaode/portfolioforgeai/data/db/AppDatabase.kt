package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import com.aryanpaode.portfolioforgeai.data.model.DocumentEntity
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity

@Database(
    entities = [ProfileEntity::class, DocumentEntity::class, CareerItemEntity::class, SkillEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun documentDao(): DocumentDao
    abstract fun careerItemDao(): CareerItemDao
    abstract fun skillDao(): SkillDao
}
