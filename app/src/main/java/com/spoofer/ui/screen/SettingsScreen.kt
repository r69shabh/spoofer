package com.spoofer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoofer.model.TransportMode
import com.spoofer.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val gpsInterval by viewModel.gpsUpdateInterval.collectAsState(initial = 1000L)
    val jitterEnabled by viewModel.jitterEnabled.collectAsState(initial = true)
    val jitterIntensity by viewModel.jitterIntensity.collectAsState(initial = 2f)
    val transportMode by viewModel.defaultTransportMode.collectAsState(initial = "CYCLE")
    val darkTheme by viewModel.darkTheme.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Appearance
            CategoryHeader("Appearance")
            ListItem(
                headlineContent = { Text("Dark Theme") },
                supportingContent = { Text("Use dark color scheme") },
                leadingContent = {
                    Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingContent = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { scope.launch { viewModel.setDarkTheme(it) } },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // GPS Settings
            CategoryHeader("GPS Settings")
            ListItem(
                headlineContent = { Text("Update Interval") },
                supportingContent = { Text("How often mock location is pushed") },
                leadingContent = {
                    Icon(Icons.Default.GpsFixed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingContent = {
                    Text(
                        "${gpsInterval / 1000f}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            Slider(
                value = gpsInterval.toFloat(),
                onValueChange = { scope.launch { viewModel.setGpsUpdateInterval(it.toLong()) } },
                valueRange = 500f..5000f,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
            
            ListItem(
                headlineContent = { Text("GPS Jitter") },
                supportingContent = { Text("Adds random variation to location") },
                trailingContent = {
                    Switch(
                        checked = jitterEnabled,
                        onCheckedChange = { scope.launch { viewModel.setJitterEnabled(it) } },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )

            if (jitterEnabled) {
                ListItem(
                    headlineContent = { Text("Jitter Intensity") },
                    supportingContent = { Text("%.0fm radius".format(jitterIntensity)) },
                    trailingContent = {
                        Text(
                            "%.0fm".format(jitterIntensity),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
                Slider(
                    value = jitterIntensity,
                    onValueChange = { scope.launch { viewModel.setJitterIntensity(it) } },
                    valueRange = 1f..5f,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Movement Defaults
            CategoryHeader("Movement Defaults")
            ListItem(
                headlineContent = { Text("Default Transport Mode") },
                leadingContent = {
                    Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = transportMode == mode.name,
                        onClick = { scope.launch { viewModel.setDefaultTransportMode(mode.name) } },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // About
            CategoryHeader("About")
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    "Spoofer v1.0",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Requires mock location permission in Developer Options.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Location data is spoofed locally. No data is sent to external servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}
