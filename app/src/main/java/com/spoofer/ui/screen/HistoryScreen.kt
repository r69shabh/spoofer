package com.spoofer.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoofer.data.db.SpoofHistoryEntity
import com.spoofer.model.SpoofMode
import com.spoofer.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSelectEntry: (SpoofHistoryEntity) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val entries by viewModel.historyEntries.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = entries.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = currentFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                        )
                    }
                    SpoofMode.entries.forEach { mode ->
                        item {
                            FilterChip(
                                selected = currentFilter == mode.name,
                                onClick = { viewModel.setFilter(mode.name) },
                                label = {
                                    Text(
                                        when (mode) {
                                            SpoofMode.STATIC -> "Static"
                                            SpoofMode.DIRECTIONS -> "Directions"
                                            SpoofMode.JOYSTICK -> "Joystick"
                                        },
                                    )
                                },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No spoofing history",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start spoofing to see your sessions here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    HistoryItem(
                        entry = entry,
                        onClick = { onSelectEntry(entry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Clear All History") },
            text = { Text("This will permanently delete all spoofing history entries.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun HistoryItem(
    entry: SpoofHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeIcon =
        when (entry.mode) {
            "STATIC" -> Icons.Default.LocationOn
            "DIRECTIONS" -> Icons.Default.DirectionsWalk
            "JOYSTICK" -> Icons.Default.Gamepad
            else -> Icons.Default.LocationOn
        }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val duration =
        entry.endTime?.let { end ->
            val seconds = (end - entry.startTime) / 1000
            if (seconds >= 3600) {
                "${seconds / 3600}h ${(seconds % 3600) / 60}min"
            } else {
                "${seconds / 60} min"
            }
        } ?: "In progress"

    val modeLabel = entry.mode.lowercase().replaceFirstChar { it.uppercase() }
    val coordText =
        entry.destinationName
            ?: "%.4f, %.4f".format(entry.targetLatitude, entry.targetLongitude)

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = coordText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(duration)
                Text("•")
                Text(dateFormat.format(Date(entry.startTime)))
            }
        },
        overlineContent = {
            Text(modeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        },
        leadingContent = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailingContent = {
            entry.distanceTraveled?.let { dist ->
                Text(
                    text = if (dist >= 1000) "%.1f km".format(dist / 1000) else "%.0f m".format(dist),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
    )
}
