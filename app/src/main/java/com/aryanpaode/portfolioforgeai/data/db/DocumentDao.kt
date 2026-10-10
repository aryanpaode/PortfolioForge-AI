package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.aryanpaode.portfolioforgeai.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    suspend fun getAll(): List<DocumentEntity>

    @Upsert
    suspend fun upsert(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE contentHash = :hash AND contentHash != '' LIMIT 1")
    suspend fun findByHash(hash: String): DocumentEntity?

    @Query("DELETE FROM documents")
    suspend fun clearAll()

    @Upsert
    suspend fun upsertAll(documents: List<DocumentEntity>)
}
