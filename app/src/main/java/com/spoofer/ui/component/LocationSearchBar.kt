package com.spoofer.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.PlaceSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchBar(
    onLocationSelected: (LatLng) -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearch: suspend (String) -> List<PlaceSuggestion>,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var isActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    SearchBar(
        query = query,
        onQueryChange = { newQuery ->
            query = newQuery
            searchJob?.cancel()
            if (newQuery.length >= 2) {
                searchJob = scope.launch {
                    delay(300)
                    try {
                        suggestions = onSearch(newQuery)
                    } catch (_: Exception) {
                        suggestions = emptyList()
                    }
                }
            } else {
                suggestions = emptyList()
            }
        },
        onSearch = { q ->
            if (q.isNotBlank() && suggestions.isNotEmpty()) {
                val first = suggestions.first()
                onLocationSelected(LatLng(first.latitude, first.longitude))
                query = first.label
            }
            isActive = false
        },
        active = isActive,
        onActiveChange = { isActive = it },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search location...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (isActive && query.isNotEmpty()) {
                IconButton(onClick = {
                    query = ""
                    suggestions = emptyList()
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else if (!isActive) {
                Row {
                    IconButton(onClick = onFavoritesClick) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        shape = androidx.compose.foundation.shape.CircleShape,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        suggestions.forEach { place ->
            val subtitle = place.locality.ifBlank {
                val parts = place.label.split(",").drop(1)
                parts.joinToString(",").trim()
            }

            ListItem(
                headlineContent = { Text(place.name.ifBlank { place.label }, maxLines = 1) },
                supportingContent = {
                    if (subtitle.isNotEmpty()) Text(subtitle, maxLines = 1)
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable {
                        isActive = false
                        query = place.label
                        suggestions = emptyList()
                        onLocationSelected(LatLng(place.latitude, place.longitude))
                    },
            )
        }
    }
}
