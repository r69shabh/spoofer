package com.spoofer.data.repository

import com.spoofer.data.db.SavedLocationDao
import com.spoofer.data.db.SavedLocationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: SavedLocationDao,
) {
    fun getAll(): Flow<List<SavedLocationEntity>> = dao.getAll()

    fun search(query: String): Flow<List<SavedLocationEntity>> = dao.search(query)

    suspend fun save(name: String, latitude: Double, longitude: Double): Long {
        return dao.insert(
            SavedLocationEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
            )
        )
    }

    suspend fun delete(location: SavedLocationEntity) = dao.delete(location)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
