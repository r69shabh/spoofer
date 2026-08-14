package com.spoofer.location

import android.location.Location
import com.google.android.gms.maps.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpoofLocationSource
    @Inject
    constructor(
        private val realLocationProvider: RealLocationProvider,
    ) : LocationSource {
        private var scope: CoroutineScope? = null
        private var realRelayJob: Job? = null
        private var speedCollectionJob: Job? = null
        private var listener: LocationSource.OnLocationChangedListener? = null

        @Volatile
        private var isSpoofingActive = false

        private val _currentSpeedKmh = MutableStateFlow(0f)
        val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

        override fun activate(listener: LocationSource.OnLocationChangedListener) {
            this.listener = listener
            if (!isSpoofingActive) {
                startRealRelay()
            }
        }

        override fun deactivate() {
            stopRealRelay()
            listener = null
        }

        fun pushSpoofedLocation(
            latitude: Double,
            longitude: Double,
            bearing: Float = 0f,
            speed: Float = 0f,
        ) {
            val location =
                Location("spoof").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    this.bearing = bearing
                    this.speed = speed
                    this.time = System.currentTimeMillis()
                }
            listener?.onLocationChanged(location)
        }

        fun enterSpoofMode() {
            isSpoofingActive = true
            stopRealRelay()
        }

        fun exitSpoofMode() {
            isSpoofingActive = false
            startRealRelay()
        }

        fun stopRealRelay() {
            realRelayJob?.cancel()
            realRelayJob = null
        }

        fun startRealRelay() {
            stopRealRelay()
            // Bug 9 fix: lazily create a scope if none exists (e.g. exitSpoofMode
            // called before start() or after stop()).  Without this the map's blue
            // dot freezes permanently after spoofing ends.
            val currentScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main).also { scope = it }
            realRelayJob =
                currentScope.launch {
                    realLocationProvider.getLocationUpdates(1000).collect { location ->
                        if (!isSpoofingActive) {
                            listener?.onLocationChanged(location)
                        }
                    }
                }
        }

        fun start() {
            scope?.cancel()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            speedCollectionJob =
                scope!!.launch {
                    realLocationProvider.getLocationUpdatesWithSpeed(1000).collect { result ->
                        val speedMs = result.speedMs
                        _currentSpeedKmh.value = if (speedMs > 0.5f) speedMs * 3.6f else 0f
                    }
                }
            startRealRelay()
        }

        fun stop() {
            stopRealRelay()
            speedCollectionJob?.cancel()
            speedCollectionJob = null
            isSpoofingActive = false
            scope?.cancel()
            scope = null
        }
    }
