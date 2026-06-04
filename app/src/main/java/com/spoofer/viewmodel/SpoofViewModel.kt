package com.spoofer.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.DirectionsRepository
import com.spoofer.data.RouteInfo
import com.spoofer.service.MockLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpoofViewModel
    @Inject
    constructor(
        private val application: Application,
        private val dataStore: DataStore<Preferences>,
        private val directionsRepo: DirectionsRepository,
    ) : ViewModel() {
        private val _showSetupDialog = MutableStateFlow(false)
        val showSetupDialog: StateFlow<Boolean> = _showSetupDialog.asStateFlow()

        private val _routeInfo = MutableStateFlow<RouteInfo?>(null)
        val routeInfo: StateFlow<RouteInfo?> = _routeInfo.asStateFlow()

        private val _routePreview = MutableStateFlow<List<LatLng>>(emptyList())
        val routePreview: StateFlow<List<LatLng>> = _routePreview.asStateFlow()

        private val _isLoadingRoute = MutableStateFlow(false)
    val isLoadingRoute: StateFlow<Boolean> = _isLoadingRoute.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()

    val remainingDistance: StateFlow<Double> = MockLocationService.remainingDistance

        fun startStaticSpoof(target: LatLng) {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_SET_STATIC
                    putExtra(MockLocationService.EXTRA_LATITUDE, target.latitude)
                    putExtra(MockLocationService.EXTRA_LONGITUDE, target.longitude)
                }
            application.startForegroundService(intent)
        }

        fun startDirectionsSpoof(
            origin: LatLng,
            destination: LatLng,
            speedMps: Float,
        ) {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_START_MOVEMENT
                    putExtra(MockLocationService.EXTRA_LATITUDE, origin.latitude)
                    putExtra(MockLocationService.EXTRA_LONGITUDE, origin.longitude)
                    putExtra(MockLocationService.EXTRA_DEST_LATITUDE, destination.latitude)
                    putExtra(MockLocationService.EXTRA_DEST_LONGITUDE, destination.longitude)
                    putExtra(MockLocationService.EXTRA_SPEED, speedMps)
                }
            application.startForegroundService(intent)
        }

        fun stopSpoofing() {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_STOP
                }
            application.startService(intent)
        }

        fun fetchRoutePreview(
            origin: LatLng,
            destination: LatLng,
        ) {
            viewModelScope.launch {
                _isLoadingRoute.value = true
                try {
                    val route = directionsRepo.getRoute(origin, destination)
                    _routeInfo.value = route
                    _routePreview.value = route.polyline
                    _routeError.value = null
                } catch (_: Exception) {
                    _routeInfo.value = null
                    _routePreview.value = emptyList()
                    _routeError.value = "Could not find a route. Check your locations."
                } finally {
                    _isLoadingRoute.value = false
                }
            }
        }

        fun clearRoutePreview() {
            _routeInfo.value = null
            _routePreview.value = emptyList()
        }

        fun checkMockLocationProvider() {
            viewModelScope.launch {
                val dismissed = dataStore.data.first()[KEY_SETUP_DISMISSED] ?: false
                if (dismissed) return@launch
                val allowMock =
                    try {
                        Settings.Secure.getInt(application.contentResolver, "mock_location", 0)
                    } catch (_: Exception) {
                        0
                    }
                if (allowMock == 0) {
                    _showSetupDialog.value = true
                }
            }
        }

        fun dismissSetupDialog() {
            _showSetupDialog.value = false
            viewModelScope.launch { dataStore.edit { it[KEY_SETUP_DISMISSED] = true } }
        }

        fun startJoystick(
            origin: LatLng,
            speedMps: Float,
        ) {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_START_JOYSTICK
                    putExtra(MockLocationService.EXTRA_LATITUDE, origin.latitude)
                    putExtra(MockLocationService.EXTRA_LONGITUDE, origin.longitude)
                    putExtra(MockLocationService.EXTRA_SPEED, speedMps)
                }
            application.startForegroundService(intent)
        }

        fun updateJoystick(
            angle: Float,
            magnitude: Float,
            speedMps: Float? = null,
        ) {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_UPDATE_JOYSTICK
                    putExtra(MockLocationService.EXTRA_ANGLE, angle)
                    putExtra(MockLocationService.EXTRA_MAGNITUDE, magnitude)
                    if (speedMps != null) putExtra(MockLocationService.EXTRA_SPEED, speedMps)
                }
            application.startService(intent)
        }

        fun updateJoystickSpeed(speedMps: Float) {
            val intent =
                Intent(application, MockLocationService::class.java).apply {
                    action = MockLocationService.ACTION_UPDATE_JOYSTICK
                    putExtra(MockLocationService.EXTRA_SPEED, speedMps)
                }
            application.startService(intent)
        }

        companion object {
            private val KEY_SETUP_DISMISSED = booleanPreferencesKey("setup_dismissed")
        }
    }
