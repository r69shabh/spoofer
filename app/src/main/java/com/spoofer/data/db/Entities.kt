package com.spoofer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "spoof_history")
data class SpoofHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val targetLatitude: Double,
    val targetLongitude: Double,
    val destinationName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceTraveled: Float? = null,
)
