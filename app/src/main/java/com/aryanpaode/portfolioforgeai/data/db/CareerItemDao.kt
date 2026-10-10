package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.aryanpaode.portfolioforgeai.data.model.CareerItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerItemDao {
    @Query("SELECT * FROM career_items ORDER BY type, title")
    fun observeAll(): Flow<List<CareerItemEntity>>

    @Query("SELECT * FROM career_items")
    suspend fun getAll(): List<CareerItemEntity>

    @Upsert
    suspend fun upsert(item: CareerItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<CareerItemEntity>)

    @Query("DELETE FROM career_items")
    suspend fun clearAll()

    @Delete
    suspend fun delete(item: CareerItemEntity)

    @Query("SELECT * FROM career_items WHERE sourceDocumentId = :documentId")
    suspend fun getByDocument(documentId: String): List<CareerItemEntity>
}
