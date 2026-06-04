package com.spoofer.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.spoofer.model.SpoofMode
import com.spoofer.ui.component.JoystickOverlay
import com.spoofer.ui.component.LocationSearchBar
import com.spoofer.ui.component.MockLocationSetupDialog
import com.spoofer.ui.component.StatusChip
import com.spoofer.viewmodel.FavoritesViewModel
import com.spoofer.viewmodel.MapViewModel
import com.spoofer.viewmodel.SpoofViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    spoofViewModel: SpoofViewModel,
    favoriteViewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    val targetLatLng by mapViewModel.targetLatLng.collectAsState()
    val originLatLng by mapViewModel.originLatLng.collectAsState()
    val selectedMode by mapViewModel.selectedMode.collectAsState()
    val cameraPosition by mapViewModel.cameraPosition.collectAsState()
    val isSpoofing by mapViewModel.isSpoofing.collectAsState()
    val currentSpoofedLocation by mapViewModel.currentSpoofedLocation.collectAsState()
    val spoofMode by mapViewModel.spoofMode.collectAsState()
    val elapsedSeconds by mapViewModel.elapsedSeconds.collectAsState()
    val speedKmh by mapViewModel.speedKmh.collectAsState()
    val speedMode by mapViewModel.speedMode.collectAsState()
    val currentSpeedKmh by mapViewModel.currentSpeedKmh.collectAsState()
    val transportMode by mapViewModel.transportMode.collectAsState()
    val joySpeedKmh by mapViewModel.joySpeedKmh.collectAsState()
    val totalDistanceTraveled by mapViewModel.totalDistanceTraveled.collectAsState()
    val currentHeading by mapViewModel.currentHeading.collectAsState()

    val routeInfo by spoofViewModel.routeInfo.collectAsState()
    val routePreview by spoofViewModel.routePreview.collectAsState()
    val remainingDistance by spoofViewModel.remainingDistance.collectAsState()
    val isLoadingRoute by spoofViewModel.isLoadingRoute.collectAsState()
    val routeError by spoofViewModel.routeError.collectAsState()
    val showSetupDialog by spoofViewModel.showSetupDialog.collectAsState()

    val favorites by favoriteViewModel.favorites.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scaffoldState = rememberBottomSheetScaffoldState()

    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogName by remember { mutableStateOf("") }
    var originText by remember { mutableStateOf("My Location") }
    var destText by remember { mutableStateOf("") }
    val favoritesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraState = rememberCameraPositionState()
    val originMarkerState = rememberMarkerState()
    val targetMarkerState = rememberMarkerState()

    val infiniteTransition = rememberInfiniteTransition(label = "spoof_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulse_radius",
    )

    val isJoystickActive = isSpoofing && spoofMode == SpoofMode.JOYSTICK
    val isJoystickPreview = !isSpoofing && spoofMode == SpoofMode.JOYSTICK

    val onStartStop: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isSpoofing) {
            spoofViewModel.stopSpoofing()
        } else {
            when (selectedMode) {
                SpoofMode.STATIC -> targetLatLng?.let {
                    spoofViewModel.startStaticSpoof(it)
                }
                SpoofMode.DIRECTIONS -> {
                    val origin = originLatLng ?: cameraPosition
                    val dest = targetLatLng
                    if (origin != null && dest != null) {
                        spoofViewModel.startDirectionsSpoof(origin, dest, speedKmh / 3.6f)
                    }
                }
                SpoofMode.JOYSTICK ->
                    originLatLng?.let { origin ->
                        spoofViewModel.startJoystick(origin, joySpeedKmh / 3.6f)
                    }
            }
        }
    }

    LaunchedEffect(isSpoofing) {
        if (isSpoofing && currentSpoofedLocation != null) {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(currentSpoofedLocation!!, 17f))
        }
    }

    LaunchedEffect(targetLatLng) { 
        targetLatLng?.let { 
            targetMarkerState.position = it
            if (isSpoofing && selectedMode == SpoofMode.STATIC) {
                spoofViewModel.startStaticSpoof(it)
            }
        } 
    }
    LaunchedEffect(originLatLng) { originLatLng?.let { originMarkerState.position = it } }
    LaunchedEffect(Unit) {
        mapViewModel.loadInitialLocation()
        spoofViewModel.checkMockLocationProvider()
    }
    LaunchedEffect(cameraPosition) { cameraPosition?.let { cameraState.move(CameraUpdateFactory.newLatLngZoom(it, 16f)) } }

    LaunchedEffect(selectedMode, originLatLng, targetLatLng) {
        if (selectedMode == SpoofMode.DIRECTIONS && originLatLng != null && targetLatLng != null) {
            spoofViewModel.fetchRoutePreview(originLatLng!!, targetLatLng!!)
        } else {
            spoofViewModel.clearRoutePreview()
        }
    }

    LaunchedEffect(isJoystickActive, currentSpoofedLocation) {
        if (isJoystickActive && currentSpoofedLocation != null) {
            cameraState.animate(CameraUpdateFactory.newLatLng(currentSpoofedLocation!!))
        }
    }
    LaunchedEffect(isJoystickActive, joySpeedKmh) {
        if (isJoystickActive) spoofViewModel.updateJoystickSpeed(joySpeedKmh / 3.6f)
    }

    LaunchedEffect(selectedMode) {
        if (selectedMode == SpoofMode.DIRECTIONS) {
            spoofViewModel.clearRoutePreview()
        }
    }

    val fineLocationGranted =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    val isDarkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 72.dp,
        sheetShape = MaterialTheme.shapes.extraLarge,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        containerColor = MaterialTheme.colorScheme.background,
        sheetContent = {
            BottomSheetContent(
                selectedMode = selectedMode,
                onModeSelected = { mapViewModel.setMode(it) },
                targetLatLng = targetLatLng,
                isSpoofing = isSpoofing,
                onSaveFavorite = { showSaveDialog = true },
                originText = originText, destText = destText,
                onOriginTextChange = { originText = it },
                onDestTextChange = { destText = it },
                onOriginSelected = { latLng ->
                    mapViewModel.setOrigin(latLng)
                    originMarkerState.position = latLng
                },
                onSwap = { mapViewModel.swapOriginAndDestination() },
                onDestSelected = { latLng ->
                    mapViewModel.setTarget(latLng)
                    targetMarkerState.position = latLng
                },
                onSearchPlace = { query -> mapViewModel.searchPlaces(query) },
                speedKmh = speedKmh, onSpeedChange = { mapViewModel.setSpeedKmh(it) },
                speedMode = speedMode, onSpeedModeChange = { mapViewModel.setSpeedMode(it) },
                currentSpeedKmh = currentSpeedKmh,
                transportMode = transportMode, onTransportModeChange = { mapViewModel.setTransportMode(it) },
                routeInfo = routeInfo, remainingDistance = remainingDistance,
                isLoadingRoute = isLoadingRoute,
                routeError = routeError,
                joySpeedKmh = joySpeedKmh, onJoySpeedChange = { mapViewModel.setJoySpeedKmh(it) },
                totalDistanceTraveled = totalDistanceTraveled, currentHeading = currentHeading,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties =
                    MapProperties(
                        isMyLocationEnabled = fineLocationGranted && !isSpoofing,
                        mapStyleOptions =
                            if (isDarkMap) {
                                MapStyleOptions.loadRawResourceStyle(
                                    context,
                                    com.spoofer.R.raw.map_style_dark,
                                )
                            } else {
                                null
                            },
                    ),
                uiSettings =
                    MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false,
                    ),
                onMapClick = { latLng ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedMode == SpoofMode.DIRECTIONS && targetLatLng != null) {
                        mapViewModel.setOrigin(latLng)
                        originMarkerState.position = latLng
                    } else {
                        mapViewModel.setTarget(latLng)
                        targetMarkerState.position = latLng
                    }
                },
            ) {
                originLatLng?.let {
                    Marker(
                        state = originMarkerState,
                        title = "Origin",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    )
                }
                Marker(
                    state = targetMarkerState,
                    title = "Destination",
                    draggable = true,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                )

                if (routePreview.isNotEmpty()) {
                    Polyline(
                        points = routePreview,
                        color = MaterialTheme.colorScheme.primary,
                        width = 6f,
                    )
                }
                currentSpoofedLocation?.let { loc ->
                    Circle(
                        center = loc,
                        radius = pulseRadius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2f,
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            ) {
                LocationSearchBar(
                    onLocationSelected = { latLng ->
                        mapViewModel.setTarget(latLng)
                        targetMarkerState.position = latLng
                        cameraState.move(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    },
                    onFavoritesClick = { showFavoritesSheet = true },
                    onHistoryClick = onNavigateToHistory,
                    onSettingsClick = onNavigateToSettings,
                    onSearch = { query -> mapViewModel.searchPlaces(query) },
                )
                Spacer(Modifier.height(8.dp))
                spoofMode?.let { mode ->
                    StatusChip(
                        mode = mode,
                        elapsedSeconds = elapsedSeconds,
                        isActive = isSpoofing,
                    )
                }
            }

            JoystickOverlay(
                isActive = isJoystickActive,
                isPreview = isJoystickPreview,
                onInput = { spoofViewModel.updateJoystick(it.angle, it.magnitude, joySpeedKmh / 3.6f) },
            )

            val fabInteraction = remember { MutableInteractionSource() }
            val fabPressed by fabInteraction.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
                label = "fab_scale",
            )

            SmallFloatingActionButton(
                onClick = {
                    val current = cameraState.position.target
                    cameraState.move(CameraUpdateFactory.newLatLngZoom(current, 17f))
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 96.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                    ),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(24.dp))
            }

            ExtendedFloatingActionButton(
                onClick = onStartStop,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .scale(fabScale),
                interactionSource = fabInteraction,
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor =
                    if (isSpoofing) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                contentColor =
                    if (isSpoofing) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp,
                    ),
            ) {
                Icon(
                    if (isSpoofing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isSpoofing) "Stop" else "Start",
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (isSpoofing) "Stop spoofing" else "Start spoofing",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showFavoritesSheet) {
        FavoritesSheet(
            favorites = favorites,
            onSelect = { location ->
                val ll = LatLng(location.latitude, location.longitude)
                mapViewModel.setTarget(ll)
                targetMarkerState.position = ll
                cameraState.move(CameraUpdateFactory.newLatLngZoom(ll, 16f))
                showFavoritesSheet = false
            },
            onDelete = { favoriteViewModel.delete(it) },
            onDismiss = { showFavoritesSheet = false },
            sheetState = favoritesSheetState,
        )
    }

    if (showSaveDialog && targetLatLng != null) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveDialogName = ""
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Save Location") },
            text = {
                OutlinedTextField(
                    value = saveDialogName,
                    onValueChange = { saveDialogName = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Home, Work, Park") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (saveDialogName.isNotBlank()) {
                        favoriteViewModel.save(saveDialogName.trim(), targetLatLng!!.latitude, targetLatLng!!.longitude)
                        showSaveDialog = false
                        saveDialogName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    saveDialogName = ""
                }) { Text("Cancel") }
            },
        )
    }

    if (showSetupDialog) MockLocationSetupDialog(onDismiss = { spoofViewModel.dismissSetupDialog() })
}
