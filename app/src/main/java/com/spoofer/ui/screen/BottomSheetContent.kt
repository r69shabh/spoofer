package com.spoofer.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.RouteInfo
import com.spoofer.model.SpeedMode
import com.spoofer.model.SpoofMode
import com.spoofer.model.TransportMode
import com.spoofer.ui.component.LocationInputField
import com.spoofer.ui.component.SpeedSlider
import java.util.Locale

@Composable
fun BottomSheetContent(
    selectedMode: SpoofMode,
    onModeSelected: (SpoofMode) -> Unit,
    targetLatLng: LatLng?,
    isSpoofing: Boolean,
    onSaveFavorite: () -> Unit = {},
    originText: String = "",
    destText: String = "",
    onOriginTextChange: (String) -> Unit = {},
    onDestTextChange: (String) -> Unit = {},
    onOriginSelected: (LatLng) -> Unit = {},
    onSwap: () -> Unit = {},
    speedKmh: Float = 15f,
    onSpeedChange: (Float) -> Unit = {},
    speedMode: SpeedMode = SpeedMode.MANUAL,
    onSpeedModeChange: (SpeedMode) -> Unit = {},
    currentSpeedKmh: Float = 0f,
    transportMode: TransportMode = TransportMode.CAR,
    onTransportModeChange: (TransportMode) -> Unit = {},
    routeInfo: RouteInfo? = null,
    remainingDistance: Double? = null,
    isLoadingRoute: Boolean = false,
    routeError: String? = null,
    joySpeedKmh: Float = 5f,
    onJoySpeedChange: (Float) -> Unit = {},
    totalDistanceTraveled: Double = 0.0,
    currentHeading: Float = 0f,
    onSearchPlace: suspend (String) -> List<com.spoofer.data.PlaceSuggestion> = { emptyList() },
    onDestSelected: (LatLng) -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drag Handle
        Box(
            modifier =
                Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.height(16.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedMode == SpoofMode.STATIC,
                onClick = { onModeSelected(SpoofMode.STATIC) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = { Icon(Icons.Default.LocationOn, null, Modifier.size(SegmentedButtonDefaults.IconSize)) },
            ) { Text("Static", style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = selectedMode == SpoofMode.DIRECTIONS,
                onClick = { onModeSelected(SpoofMode.DIRECTIONS) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = { Icon(Icons.Default.DirectionsWalk, null, Modifier.size(SegmentedButtonDefaults.IconSize)) },
            ) { Text("Directions", style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = selectedMode == SpoofMode.JOYSTICK,
                onClick = { onModeSelected(SpoofMode.JOYSTICK) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = { Icon(Icons.Default.Gamepad, null, Modifier.size(SegmentedButtonDefaults.IconSize)) },
            ) { Text("Joystick", style = MaterialTheme.typography.labelMedium) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(300)) { it * direction } + fadeIn(tween(200, delayMillis = 60)))
                    .togetherWith(slideOutHorizontally(tween(300)) { -it * direction } + fadeOut(tween(150)))
                    .using(SizeTransform(clip = false))
            },
            label = "mode_content",
        ) { mode ->
            when (mode) {
                SpoofMode.STATIC -> StaticModePanel(targetLatLng, onSaveFavorite)
                SpoofMode.DIRECTIONS ->
                    DirectionsModePanel(
                        originText = originText,
                        destText = destText,
                        onOriginTextChange = onOriginTextChange,
                        onDestTextChange = onDestTextChange,
                        onOriginSelected = onOriginSelected,
                        onDestSelected = onDestSelected,
                        onSearchPlace = onSearchPlace,
                        onSwap = onSwap,
                        speedKmh = speedKmh,
                        onSpeedChange = onSpeedChange,
                        speedMode = speedMode,
                        onSpeedModeChange = onSpeedModeChange,
                        currentSpeedKmh = currentSpeedKmh,
                        transportMode = transportMode,
                        onTransportModeChange = onTransportModeChange,
                        routeInfo = routeInfo,
                        remainingDistance = remainingDistance,
                        isLoadingRoute = isLoadingRoute,
                        routeError = routeError,
                        isSpoofing = isSpoofing,
                    )
                SpoofMode.JOYSTICK ->
                    JoystickPanel(
                        joySpeedKmh,
                        onJoySpeedChange,
                        totalDistanceTraveled,
                        currentHeading,
                        isSpoofing,
                    )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun StaticModePanel(
    targetLatLng: LatLng?,
    onSaveFavorite: () -> Unit,
) {
    if (targetLatLng == null) {
        androidx.compose.material3.Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors =
                androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Tap the map to select a target",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        androidx.compose.material3.Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors =
                androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Target Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatDms(targetLatLng.latitude, targetLatLng.longitude),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.6f, %.6f".format(targetLatLng.latitude, targetLatLng.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = onSaveFavorite,
                    label = { Text("Save to Favorites") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.StarBorder,
                            null,
                            Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectionsModePanel(
    originText: String,
    destText: String,
    onOriginTextChange: (String) -> Unit,
    onDestTextChange: (String) -> Unit,
    onOriginSelected: (LatLng) -> Unit,
    onDestSelected: (LatLng) -> Unit,
    onSearchPlace: suspend (String) -> List<com.spoofer.data.PlaceSuggestion>,
    onSwap: () -> Unit,
    speedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    speedMode: SpeedMode,
    onSpeedModeChange: (SpeedMode) -> Unit,
    currentSpeedKmh: Float,
    transportMode: TransportMode,
    onTransportModeChange: (TransportMode) -> Unit,
    routeInfo: RouteInfo?,
    remainingDistance: Double?,
    isLoadingRoute: Boolean = false,
    routeError: String? = null,
    isSpoofing: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.material3.Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors =
                androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Column {
                LocationInputField(
                    value = originText,
                    onValueChange = onOriginTextChange,
                    placeholder = "From",
                    onLocationSelected = onOriginSelected,
                    onSearch = onSearchPlace,
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        androidx.compose.material3.TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                LocationInputField(
                    value = destText,
                    onValueChange = onDestTextChange,
                    placeholder = "To",
                    onLocationSelected = onDestSelected,
                    onSearch = onSearchPlace,
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onSwap) {
                            Icon(Icons.Default.SwapVert, "Swap", Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        androidx.compose.material3.TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Transport Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransportMode.entries.forEach { mode ->
                FilterChip(
                    selected = transportMode == mode,
                    onClick = { onTransportModeChange(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = speedMode == SpeedMode.MANUAL,
                onClick = { onSpeedModeChange(SpeedMode.MANUAL) },
                label = { Text("Manual", style = MaterialTheme.typography.labelMedium) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
            FilterChip(
                selected = speedMode == SpeedMode.CURRENT,
                onClick = { onSpeedModeChange(SpeedMode.CURRENT) },
                label = { Text("Current Speed", style = MaterialTheme.typography.labelMedium) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        Spacer(Modifier.height(12.dp))

        when (speedMode) {
            SpeedMode.MANUAL -> SpeedSlider(speedKmh, onSpeedChange)
            SpeedMode.CURRENT -> {
                androidx.compose.material3.Card(
                    Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Your Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            String.format(Locale.US, "%.0f km/h", currentSpeedKmh),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (isLoadingRoute) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        AnimatedVisibility(
            visible = routeError != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(150)),
        ) {
            if (routeError != null) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Card(
                    Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            routeError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        routeInfo?.let { route ->
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Card(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors =
                    androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            ) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Route: ${formatDistance(route.distanceMeters.toDouble())}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        "ETA: ${formatDuration(route.durationSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (isSpoofing && remainingDistance != null && routeInfo != null) {
            val progress =
                if (routeInfo.distanceMeters > 0) {
                    ((routeInfo.distanceMeters - remainingDistance) / routeInfo.distanceMeters).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${formatDistance(remainingDistance)} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val etaSeconds =
                    if (speedMode == SpeedMode.CURRENT && currentSpeedKmh > 0) {
                        (remainingDistance / (currentSpeedKmh / 3.6)).toInt()
                    } else if (speedKmh > 0) {
                        (remainingDistance / (speedKmh / 3.6)).toInt()
                    } else {
                        0
                    }
                Text(
                    "ETA: ${formatDuration(etaSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun JoystickPanel(
    speedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    totalDistanceTraveled: Double,
    currentHeading: Float,
    isSpoofing: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Drag the joystick on the map to move.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SpeedSlider(speedKmh, onSpeedChange)
        if (isSpoofing) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Heading: ${currentHeading.toInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Traveled: ${formatDistance(totalDistanceTraveled)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatDms(
    lat: Double,
    lng: Double,
): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lngDir = if (lng >= 0) "E" else "W"
    return String.format("%.4f\u00B0 %s, %.4f\u00B0 %s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
}

private fun formatDistance(meters: Double): String = if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}min" else "$m min"
}
