package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aryanpaode.portfolioforgeai.data.model.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY category, name")
    fun observeAll(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills")
    suspend fun getAll(): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(skills: List<SkillEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(skill: SkillEntity)

    @Delete
    suspend fun delete(skill: SkillEntity)

    @Query("DELETE FROM skills")
    suspend fun clearAll()
}
