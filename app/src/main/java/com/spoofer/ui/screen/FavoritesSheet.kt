package com.spoofer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spoofer.data.db.SavedLocationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesSheet(
    favorites: List<SavedLocationEntity>,
    onSelect: (SavedLocationEntity) -> Unit,
    onDelete: (SavedLocationEntity) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered =
        if (searchQuery.isBlank()) {
            favorites
        } else {
            favorites.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Saved Locations",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter favorites...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text =
                                if (favorites.isEmpty()) {
                                    "No saved locations yet"
                                } else {
                                    "No matches"
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
            ) {
                items(
                    items = filtered,
                    key = { it.id },
                ) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text("%.6f, %.6f".format(item.latitude, item.longitude))
                        },
                        leadingContent = {
                            FilledTonalIconButton(onClick = {}) {
                                Icon(Icons.Default.LocationOn, null)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(item) },
                        colors =
                            ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
