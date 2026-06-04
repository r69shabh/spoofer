package com.spoofer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val gpsUpdateInterval: Flow<Long> = dataStore.data.map { it[KEY_GPS_INTERVAL] ?: 1000L }
    val jitterEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_JITTER_ENABLED] ?: true }
    val jitterIntensity: Flow<Float> = dataStore.data.map { it[KEY_JITTER_INTENSITY] ?: 2f }
    val defaultTransportMode: Flow<String> = dataStore.data.map { it[KEY_TRANSPORT_MODE] ?: "CYCLE" }
    val darkTheme: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_THEME] ?: true }

    suspend fun setGpsUpdateInterval(ms: Long) {
        dataStore.edit { it[KEY_GPS_INTERVAL] = ms }
    }

    suspend fun setJitterEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_JITTER_ENABLED] = enabled }
    }

    suspend fun setJitterIntensity(value: Float) {
        dataStore.edit { it[KEY_JITTER_INTENSITY] = value }
    }

    suspend fun setDefaultTransportMode(mode: String) {
        dataStore.edit { it[KEY_TRANSPORT_MODE] = mode }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    companion object {
        private val KEY_GPS_INTERVAL = longPreferencesKey("gps_interval")
        private val KEY_JITTER_ENABLED = booleanPreferencesKey("jitter_enabled")
        private val KEY_JITTER_INTENSITY = floatPreferencesKey("jitter_intensity")
        private val KEY_TRANSPORT_MODE = stringPreferencesKey("transport_mode")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
