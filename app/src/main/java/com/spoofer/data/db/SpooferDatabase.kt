package com.spoofer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedLocationEntity::class, SpoofHistoryEntity::class],
    version = 1,
)
abstract class SpooferDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao

    abstract fun spoofHistoryDao(): SpoofHistoryDao
}
