package com.aryanpaode.portfolioforgeai.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aryanpaode.portfolioforgeai.data.model.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clearAll()
}
