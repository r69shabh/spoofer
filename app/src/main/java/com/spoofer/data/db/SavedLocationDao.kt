package com.spoofer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<SavedLocationEntity>>

    @Insert
    suspend fun insert(location: SavedLocationEntity): Long

    @Delete
    suspend fun delete(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
