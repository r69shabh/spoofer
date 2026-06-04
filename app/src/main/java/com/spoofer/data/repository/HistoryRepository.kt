package com.spoofer.data.repository

import com.spoofer.data.db.SpoofHistoryDao
import com.spoofer.data.db.SpoofHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: SpoofHistoryDao,
) {
    suspend fun startSession(
        mode: String,
        lat: Double,
        lng: Double,
        name: String? = null,
    ): Long {
        return dao.insert(
            SpoofHistoryEntity(
                mode = mode,
                targetLatitude = lat,
                targetLongitude = lng,
                destinationName = name,
                startTime = System.currentTimeMillis(),
            )
        )
    }

    suspend fun endSession(id: Long, distance: Float?) {
        dao.endSession(id, System.currentTimeMillis(), distance)
    }

    fun getAll(): Flow<List<SpoofHistoryEntity>> = dao.getAll()

    fun getByMode(mode: String): Flow<List<SpoofHistoryEntity>> = dao.getByMode(mode)

    suspend fun clearAll() = dao.deleteAll()
}
