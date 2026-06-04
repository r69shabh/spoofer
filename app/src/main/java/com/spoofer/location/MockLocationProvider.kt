package com.spoofer.location

import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockLocationProvider @Inject constructor(
    private val locationManager: LocationManager,
) {
    var isProviderAdded = false
        private set

    fun addTestProvider() {
        if (isProviderAdded) return

        @Suppress("DEPRECATION")
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused").forEach { provider ->
            try {
                locationManager.addTestProvider(
                    provider,
                    false, false, false, false,
                    true, true, true,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE,
                )
            } catch (e: Exception) {
                // Ignore if provider already exists or can't be added
            }
            try {
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {
                // Ignore if provider cannot be enabled
            }
        }

        isProviderAdded = true
    }

    fun removeTestProvider() {
        if (!isProviderAdded) return

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused").forEach { provider ->
            try {
                locationManager.clearTestProviderEnabled(provider)
            } catch (_: Exception) {}
            try {
                locationManager.clearTestProviderLocation(provider)
            } catch (_: Exception) {}
            try {
                locationManager.removeTestProvider(provider)
            } catch (_: Exception) {}
        }

        isProviderAdded = false
    }

    fun setMockLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float = randomAccuracy(),
        altitude: Double = 0.0,
        bearing: Float = 0f,
        speed: Float = 0f,
    ) {
        val now = System.currentTimeMillis()
        val elapsedNanos = android.os.SystemClock.elapsedRealtimeNanos()

        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused").forEach { provider ->
            val location = Location(provider).apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.altitude = altitude
                this.bearing = bearing
                this.speed = speed
                this.time = now
                this.elapsedRealtimeNanos = elapsedNanos
            }

            try {
                val makeCompleteMethod = Location::class.java.getMethod("makeComplete")
                makeCompleteMethod.invoke(location)
            } catch (e: Exception) {}

            location.extras = android.os.Bundle().apply {
                putInt("mockLocation", 1)
                putBoolean("mockLocation", true)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                location.isMock = true
            }

            try {
                locationManager.setTestProviderLocation(provider, location)
            } catch (_: Exception) {}
        }
    }

    companion object {
        private fun randomAccuracy(): Float = 1.0f // 1m accuracy to override real GPS
    }
}
