package com.spoofer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.repository.HistoryRepository
import com.spoofer.location.MockLocationProvider
import com.spoofer.model.SpoofMode
import com.spoofer.usecase.SpeedSimulationUseCase
import com.spoofer.usecase.StaticSpoofUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MockLocationService : Service() {
    @Inject lateinit var mockLocationProvider: MockLocationProvider

    @Inject lateinit var staticSpoofUseCase: StaticSpoofUseCase

    @Inject lateinit var speedSimulationUseCase: SpeedSimulationUseCase

    @Inject lateinit var historyRepo: HistoryRepository

    @Inject lateinit var spoofLocationSource: com.spoofer.location.SpoofLocationSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private var spoofMode: SpoofMode = SpoofMode.STATIC
    private var staticLat = 0.0
    private var staticLng = 0.0
    private var joyAngle = 0f
    private var joyMagnitude = 0f
    private var joySpeed = 0f
    private var speedMps = 0f
    private var destLat = 0.0
    private var destLng = 0.0
    private var historySessionId: Long = -1
    private var lastNotifText = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mockLocationProvider.addTestProvider()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_SET_STATIC -> {
                spoofMode = SpoofMode.STATIC
                staticLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                staticLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                startSpoofing()
            }
            ACTION_START_JOYSTICK -> {
                spoofMode = SpoofMode.JOYSTICK
                staticLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                staticLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                joySpeed = intent.getFloatExtra(EXTRA_SPEED, 5f)
                joyAngle = 0f
                joyMagnitude = 0f
                startSpoofing()
            }
            ACTION_UPDATE_JOYSTICK -> {
                joyAngle = intent.getFloatExtra(EXTRA_ANGLE, joyAngle)
                joyMagnitude = intent.getFloatExtra(EXTRA_MAGNITUDE, joyMagnitude)
                if (intent.hasExtra(EXTRA_SPEED)) {
                    joySpeed = intent.getFloatExtra(EXTRA_SPEED, joySpeed)
                }
            }
            ACTION_START_MOVEMENT -> {
                spoofMode = SpoofMode.DIRECTIONS
                staticLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                staticLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                destLat = intent.getDoubleExtra(EXTRA_DEST_LATITUDE, 0.0)
                destLng = intent.getDoubleExtra(EXTRA_DEST_LONGITUDE, 0.0)
                speedMps = intent.getFloatExtra(EXTRA_SPEED, 4.17f)
                // Bug 5 fix: initialize the route BEFORE starting the ticker so
                // the first tick has valid polyline data and doesn't freeze/teleport.
                scope.launch(Dispatchers.IO) {
                    speedSimulationUseCase.initialize(
                        LatLng(staticLat, staticLng),
                        LatLng(destLat, destLng),
                    )
                    _remainingDistance.value = speedSimulationUseCase.remainingDistance
                    startSpoofing()
                }
                return START_STICKY
            }
            ACTION_STOP -> stopSpoofing()
        }

        return START_STICKY
    }

    private fun startSpoofing() {
        // Bug 6 fix: always cancel the previous ticker job before resetting state
        // to prevent race conditions when SpeedSimulationUseCase (a singleton) is
        // re-initialized for a new route while the old ticker is still running.
        tickerJob?.cancel()
        tickerJob = null

        _isActive.value = true
        _currentMode.value = spoofMode
        _elapsedSeconds.value = 0L
        _totalDistanceTraveled.value = 0.0
        _remainingDistance.value = 0.0
        _currentHeading.value = 0f

        spoofLocationSource.enterSpoofMode()

        val notification = buildNotification(staticLat, staticLng)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        tickerJob?.cancel()
        tickerJob =
            scope.launch {
                logHistoryStart()
                val startRealTime = android.os.SystemClock.elapsedRealtime()
                while (true) {
                    _elapsedSeconds.value = (android.os.SystemClock.elapsedRealtime() - startRealTime) / 1000L

                    when (spoofMode) {
                        SpoofMode.STATIC -> {
                            val jittered =
                                staticSpoofUseCase.getJitteredLocation(
                                    com.google.android.gms.maps.model.LatLng(staticLat, staticLng),
                                    jitterEnabled = false,
                                )
                            mockLocationProvider.setMockLocation(jittered.latitude, jittered.longitude)
                            spoofLocationSource.pushSpoofedLocation(jittered.latitude, jittered.longitude)
                            _currentLocation.value = jittered
                        }
                        SpoofMode.JOYSTICK -> {
                            val radians = Math.toRadians(joyAngle.toDouble())
                            val joyMetersPerTick = (joySpeed * (TICK_INTERVAL_MS / 1000f)).toDouble()
                            val deltaLat = joyMagnitude * joyMetersPerTick * Math.cos(radians) * METERS_PER_DEGREE_LAT
                            val deltaLng =
                                joyMagnitude * joyMetersPerTick * Math.sin(radians) *
                                    Math.cos(
                                        Math.toRadians(staticLat),
                                    ) * METERS_PER_DEGREE_LAT
                            staticLat += deltaLat
                            staticLng += deltaLng
                            val distanceThisTick = joyMagnitude * joyMetersPerTick
                            _totalDistanceTraveled.value += distanceThisTick
                            _currentHeading.value = joyAngle
                            mockLocationProvider.setMockLocation(
                                staticLat, staticLng,
                                bearing = joyAngle,
                                speed = joySpeed,
                            )
                            spoofLocationSource.pushSpoofedLocation(staticLat, staticLng, joyAngle, joySpeed)
                            _currentLocation.value = LatLng(staticLat, staticLng)
                        }
                        SpoofMode.DIRECTIONS -> {
                            val speedVariation = speedMps * (1f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.1f)
                            val metersPerTick = speedVariation * (TICK_INTERVAL_MS / 1000f)
                            val result = speedSimulationUseCase.tick(metersPerTick)
                            if (result != null) {
                                val jitterLat =
                                    result.position.latitude +
                                        (Random.nextDouble() - 0.5) * 0.000018
                                val jitterLng =
                                    result.position.longitude +
                                        (Random.nextDouble() - 0.5) * 0.000018
                                mockLocationProvider.setMockLocation(
                                    jitterLat, jitterLng,
                                    bearing = result.bearing,
                                    speed = speedVariation,
                                )
                                spoofLocationSource.pushSpoofedLocation(jitterLat, jitterLng, result.bearing, speedVariation)
                                _currentLocation.value = LatLng(jitterLat, jitterLng)
                                // Bug 8 fix: keep staticLat/Lng tracking the un-jittered route
                                // position so the notification and state don't drift off-route.
                                staticLat = result.position.latitude
                                staticLng = result.position.longitude
                                _totalDistanceTraveled.value = result.totalDistance
                                _remainingDistance.value = speedSimulationUseCase.remainingDistance
                                if (result.arrived) {
                                    stopSpoofing()
                                }
                            }
                        }
                    }

                    updateNotification(staticLat, staticLng)
                    delay(TICK_INTERVAL_MS)
                }
            }
    }

    private fun stopSpoofing() {
        logHistoryEnd()
        tickerJob?.cancel()
        tickerJob = null
        _isActive.value = false
        _currentMode.value = null
        _currentLocation.value = null
        _elapsedSeconds.value = 0L
        _totalDistanceTraveled.value = 0.0
        _remainingDistance.value = 0.0
        _currentHeading.value = 0f
        spoofLocationSource.exitSpoofMode()
        mockLocationProvider.removeTestProvider()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        logHistoryEnd()
        scope.cancel()
        mockLocationProvider.removeTestProvider()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Location Spoofing",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows when location spoofing is active"
                setSound(null, null)
                enableVibration(false)
            }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        lat: Double,
        lng: Double,
    ): Notification {
        val stopIntent =
            Intent(this, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val coordinateText = formatCoordinate(lat, lng)
        val modeText =
            when (spoofMode) {
                SpoofMode.STATIC -> "Static"
                SpoofMode.DIRECTIONS -> "Directions"
                SpoofMode.JOYSTICK -> "Joystick"
            }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Spoofing Active")
                .setContentText("$modeText • $coordinateText")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Location Spoofing Active")
                .setContentText("$modeText • $coordinateText")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                .build()
        }
    }

    private fun updateNotification(
        lat: Double,
        lng: Double,
    ) {
        val coord = formatCoordinate(lat, lng)
        if (coord == lastNotifText) return
        lastNotifText = coord
        val notification = buildNotification(lat, lng)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatCoordinate(
        lat: Double,
        lng: Double,
    ): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format("%.4f°%s, %.4f°%s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
    }

    private fun logHistoryStart() {
        scope.launch {
            historySessionId =
                historyRepo.startSession(
                    mode = spoofMode.name,
                    lat = staticLat,
                    lng = staticLng,
                )
        }
    }

    private fun logHistoryEnd() {
        if (historySessionId <= 0) return
        val id = historySessionId
        historySessionId = -1
        val distance =
            if (spoofMode == SpoofMode.JOYSTICK || spoofMode == SpoofMode.DIRECTIONS) {
                _totalDistanceTraveled.value.toFloat()
            } else {
                null
            }
        scope.launch {
            historyRepo.endSession(id, distance)
        }
    }

    companion object {
        private const val CHANNEL_ID = "spoofing_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_INTERVAL_MS = 200L
        private const val METERS_PER_DEGREE_LAT = 1.0 / 111_320.0

        const val ACTION_SET_STATIC = "com.spoofer.action.SET_STATIC"
        const val ACTION_START_JOYSTICK = "com.spoofer.action.START_JOYSTICK"
        const val ACTION_UPDATE_JOYSTICK = "com.spoofer.action.UPDATE_JOYSTICK"
        const val ACTION_START_MOVEMENT = "com.spoofer.action.START_MOVEMENT"
        const val ACTION_STOP = "com.spoofer.action.STOP"

        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_DEST_LATITUDE = "dest_latitude"
        const val EXTRA_DEST_LONGITUDE = "dest_longitude"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_ANGLE = "angle"
        const val EXTRA_MAGNITUDE = "magnitude"

        val isActive: StateFlow<Boolean>
            get() = _isActive.asStateFlow()
        private val _isActive = MutableStateFlow(false)

        val currentLocation: StateFlow<LatLng?>
            get() = _currentLocation.asStateFlow()
        private val _currentLocation = MutableStateFlow<LatLng?>(null)

        val currentMode: StateFlow<SpoofMode?>
            get() = _currentMode.asStateFlow()
        private val _currentMode = MutableStateFlow<SpoofMode?>(null)

        val elapsedSeconds: StateFlow<Long>
            get() = _elapsedSeconds.asStateFlow()
        private val _elapsedSeconds = MutableStateFlow(0L)

        val totalDistanceTraveled: StateFlow<Double>
            get() = _totalDistanceTraveled.asStateFlow()
        private val _totalDistanceTraveled = MutableStateFlow(0.0)

        val remainingDistance: StateFlow<Double>
            get() = _remainingDistance.asStateFlow()
        private val _remainingDistance = MutableStateFlow(0.0)

        val currentHeading: StateFlow<Float>
            get() = _currentHeading.asStateFlow()
        private val _currentHeading = MutableStateFlow(0f)
    }
}
