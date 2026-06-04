package com.spoofer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun SpeedSlider(
    speedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Speed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = String.format(Locale.US, "%.0f km/h", speedKmh),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = speedKmh,
            onValueChange = onSpeedChange,
            valueRange = 1f..120f,
            steps = 0,
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = String.format(Locale.US, "%.1f m/s", speedKmh / 3.6f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )
    }
}
