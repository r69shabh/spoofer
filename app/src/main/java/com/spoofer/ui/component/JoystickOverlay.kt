package com.spoofer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.sqrt

data class JoystickInput(
    val angle: Float,
    val magnitude: Float,
)

@Composable
fun JoystickOverlay(
    isActive: Boolean,
    onInput: (JoystickInput) -> Unit,
    modifier: Modifier = Modifier,
    baseRadiusDp: Float = 56f,
    thumbRadiusDp: Float = 18f,
) {
    if (!isActive) return

    val density = LocalDensity.current
    val baseRadiusPx = with(density) { baseRadiusDp.dp.toPx() }
    val thumbRadiusPx = with(density) { thumbRadiusDp.dp.toPx() }
    val maxOffset = baseRadiusPx - thumbRadiusPx

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    val totalSizeDp = (baseRadiusDp * 2).dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier =
                Modifier
                    .offset(y = (-80).dp)
                    .size(totalSizeDp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    thumbOffset = Offset.Zero
                                    onInput(JoystickInput(angle = 0f, magnitude = 0f))
                                },
                                onDragCancel = {
                                    thumbOffset = Offset.Zero
                                    onInput(JoystickInput(angle = 0f, magnitude = 0f))
                                },
                            ) { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val rawOffset = change.position - center
                                val distance = sqrt(rawOffset.x * rawOffset.x + rawOffset.y * rawOffset.y)
                                val clampedOffset =
                                    if (distance > maxOffset) {
                                        Offset(
                                            rawOffset.x / distance * maxOffset,
                                            rawOffset.y / distance * maxOffset,
                                        )
                                    } else {
                                        rawOffset
                                    }
                                thumbOffset = clampedOffset

                                val magnitude =
                                    (
                                        sqrt(
                                            clampedOffset.x * clampedOffset.x +
                                                clampedOffset.y * clampedOffset.y,
                                        ) / maxOffset
                                    ).coerceIn(0f, 1f)

                                val angleDegrees =
                                    if (magnitude < 0.05f) {
                                        0f
                                    } else {
                                        val rad = atan2(clampedOffset.x.toDouble(), -clampedOffset.y.toDouble())
                                        Math.toDegrees(rad).toFloat().let { if (it < 0) it + 360f else it }
                                    }

                                onInput(JoystickInput(angle = angleDegrees, magnitude = magnitude))
                                change.consume()
                            }
                        },
            ) {
                val crosshairColor = MaterialTheme.colorScheme.outlineVariant
                val thumbColor = MaterialTheme.colorScheme.primary
                val thumbGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                val thumbHighlightColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val thumbCenter = center + thumbOffset

                    // Crosshair lines
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    drawLine(
                        color = crosshairColor,
                        start = Offset(center.x, 12.dp.toPx()),
                        end = Offset(center.x, size.height - 12.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    drawLine(
                        color = crosshairColor,
                        start = Offset(12.dp.toPx(), center.y),
                        end = Offset(size.width - 12.dp.toPx(), center.y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )

                    // Thumb glow
                    drawCircle(
                        color = thumbGlow,
                        radius = thumbRadiusPx + 6.dp.toPx(),
                        center = thumbCenter,
                    )

                    // Thumb
                    drawCircle(
                        color = thumbColor,
                        radius = thumbRadiusPx,
                        center = thumbCenter,
                    )

                    // Thumb highlight
                    drawCircle(
                        color = thumbHighlightColor,
                        radius = thumbRadiusPx * 0.4f,
                        center = thumbCenter - Offset(0f, thumbRadiusPx * 0.25f),
                    )
                }
            }
        }
    }
}
