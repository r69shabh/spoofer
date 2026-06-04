package com.spoofer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.spoofer.location.RealLocationProvider
import com.spoofer.model.SpeedMode
import com.spoofer.model.SpoofMode
import com.spoofer.model.TransportMode
import com.spoofer.service.MockLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val realLocationProvider: RealLocationProvider,
        val spoofLocationSource: com.spoofer.location.SpoofLocationSource,
        private val geocodingRepo: com.spoofer.data.GeocodingRepository,
    ) : ViewModel() {
        suspend fun searchPlaces(query: String): List<com.spoofer.data.PlaceSuggestion> {
            val bias = _cameraPosition.value ?: _originLatLng.value
            return geocodingRepo.search(
                query = query,
                biasLat = bias?.latitude,
                biasLon = bias?.longitude,
            )
        }

        private val _targetLatLng = MutableStateFlow<LatLng?>(null)
        val targetLatLng: StateFlow<LatLng?> = _targetLatLng.asStateFlow()

        private val _originLatLng = MutableStateFlow<LatLng?>(null)
        val originLatLng: StateFlow<LatLng?> = _originLatLng.asStateFlow()

        private val _selectedMode = MutableStateFlow(SpoofMode.STATIC)
        val selectedMode: StateFlow<SpoofMode> = _selectedMode.asStateFlow()

        private val _cameraPosition = MutableStateFlow<LatLng?>(null)
        val cameraPosition: StateFlow<LatLng?> = _cameraPosition.asStateFlow()

        private val _speedKmh = MutableStateFlow(15f)
        val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

        private val _transportMode = MutableStateFlow(TransportMode.CAR)
        val transportMode: StateFlow<TransportMode> = _transportMode.asStateFlow()

        private val _speedMode = MutableStateFlow(SpeedMode.MANUAL)
        val speedMode: StateFlow<SpeedMode> = _speedMode.asStateFlow()

        private val _joySpeedKmh = MutableStateFlow(5f)
        val joySpeedKmh: StateFlow<Float> = _joySpeedKmh.asStateFlow()

        val currentSpeedKmh: StateFlow<Float> = spoofLocationSource.currentSpeedKmh

        val isSpoofing = MockLocationService.isActive
        val currentSpoofedLocation = MockLocationService.currentLocation
        val spoofMode = MockLocationService.currentMode
        val elapsedSeconds = MockLocationService.elapsedSeconds
        val totalDistanceTraveled = MockLocationService.totalDistanceTraveled
        val currentHeading = MockLocationService.currentHeading

        fun setTarget(latLng: LatLng) {
            _targetLatLng.value = latLng
        }

        fun setOrigin(latLng: LatLng) {
            _originLatLng.value = latLng
        }

        fun swapOriginAndDestination() {
            val orig = _originLatLng.value
            val dest = _targetLatLng.value
            _originLatLng.value = dest
            _targetLatLng.value = orig
        }

        fun setMode(mode: SpoofMode) {
            _selectedMode.value = mode
        }

        fun setSpeedKmh(kmh: Float) {
            _speedKmh.value = kmh
        }

        fun setTransportMode(mode: TransportMode) {
            _transportMode.value = mode
            _speedKmh.value = mode.defaultSpeedKmh
        }

        fun setSpeedMode(mode: SpeedMode) {
            _speedMode.value = mode
        }

        fun setJoySpeedKmh(kmh: Float) {
            _joySpeedKmh.value = kmh
        }

        fun loadInitialLocation() {
            viewModelScope.launch {
                val location = realLocationProvider.getLastLocation()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    _cameraPosition.value = latLng
                    _originLatLng.value = latLng
                    if (_targetLatLng.value == null) {
                        _targetLatLng.value = latLng
                    }
                } else {
                    val default = LatLng(DEFAULT_LAT, DEFAULT_LNG)
                    _cameraPosition.value = default
                    _originLatLng.value = default
                    _targetLatLng.value = default
                }
            }
        }

        companion object {
            private const val DEFAULT_LAT = 28.6139
            private const val DEFAULT_LNG = 77.2090
        }
    }
