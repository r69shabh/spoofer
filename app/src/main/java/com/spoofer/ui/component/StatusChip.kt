package com.spoofer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spoofer.model.SpoofMode

@Composable
fun StatusChip(
    mode: SpoofMode,
    elapsedSeconds: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isActive,
        enter =
            fadeIn(animationSpec = tween(300)) +
                slideInVertically(animationSpec = tween(300)),
        exit =
            fadeOut(animationSpec = tween(300)) +
                slideOutVertically(animationSpec = tween(300)),
        modifier = modifier,
    ) {
        AssistChip(
            onClick = {},
            label = {
                Text(
                    "${modeLabel(mode)} • ${formatElapsed(elapsedSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = {
                PulsingDot()
            },
            colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            shape = androidx.compose.foundation.shape.CircleShape,
            border = null,
            modifier = Modifier.height(32.dp),
        )
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_scale",
    )
    val dotColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = dotColor, radius = size.minDimension / 2 * scale)
    }
}

private fun modeLabel(mode: SpoofMode): String =
    when (mode) {
        SpoofMode.STATIC -> "Static"
        SpoofMode.DIRECTIONS -> "Directions"
        SpoofMode.JOYSTICK -> "Joystick"
    }

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    // Bug 16 fix: use %02d for hours too so the format is always HH:MM:SS.
    return String.format("%02d:%02d:%02d", h, m, s)
}
