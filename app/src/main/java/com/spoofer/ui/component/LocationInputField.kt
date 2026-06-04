package com.spoofer.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.PlaceSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (LatLng) -> Unit,
    placeholder: String,
    onSearch: suspend (String) -> List<PlaceSuggestion>,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )
) {
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                searchJob?.cancel()
                if (newValue.length >= 2) {
                    searchJob = scope.launch {
                        delay(300)
                        try {
                            suggestions = onSearch(newValue)
                            expanded = suggestions.isNotEmpty()
                        } catch (_: Exception) {
                            suggestions = emptyList()
                            expanded = false
                        }
                    }
                } else {
                    suggestions = emptyList()
                    expanded = false
                }
            },
            placeholder = { Text(placeholder) },
            singleLine = true,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            suggestions.forEach { place ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                place.name.ifBlank { place.label },
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (place.locality.isNotEmpty()) {
                                Text(
                                    place.locality,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        suggestions = emptyList()
                        onValueChange(place.name.ifBlank { place.label })
                        onLocationSelected(LatLng(place.latitude, place.longitude))
                    },
                )
            }
        }
    }
}
