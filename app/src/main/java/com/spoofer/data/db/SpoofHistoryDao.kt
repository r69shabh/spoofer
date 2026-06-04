package com.spoofer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpoofHistoryDao {
    @Query("SELECT * FROM spoof_history ORDER BY startTime DESC")
    fun getAll(): Flow<List<SpoofHistoryEntity>>

    @Query("SELECT * FROM spoof_history WHERE mode = :mode ORDER BY startTime DESC")
    fun getByMode(mode: String): Flow<List<SpoofHistoryEntity>>

    @Insert
    suspend fun insert(entry: SpoofHistoryEntity): Long

    @Query("UPDATE spoof_history SET endTime = :endTime, distanceTraveled = :distance WHERE id = :id")
    suspend fun endSession(
        id: Long,
        endTime: Long,
        distance: Float?,
    )

    @Query("DELETE FROM spoof_history")
    suspend fun deleteAll()
}
