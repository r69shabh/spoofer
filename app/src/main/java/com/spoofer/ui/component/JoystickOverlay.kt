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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
    isPreview: Boolean = false,
    onInput: (JoystickInput) -> Unit,
    modifier: Modifier = Modifier,
    baseRadiusDp: Float = 56f,
    thumbRadiusDp: Float = 18f,
) {
    val show = isActive || isPreview
    if (!show) return

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val baseRadiusPx = with(density) { baseRadiusDp.dp.toPx() }
    val thumbRadiusPx = with(density) { thumbRadiusDp.dp.toPx() }
    val maxOffset = baseRadiusPx - thumbRadiusPx

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    val totalSizeDp = (baseRadiusDp * 2).dp

    val surfaceAlpha = if (isActive) 0.85f else 0.55f
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = surfaceAlpha)

    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isActive) 1f else 0.5f)
    val thumbGlow = MaterialTheme.colorScheme.primary.copy(alpha = if (isActive) 0.3f else 0.1f)
    val crosshairColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isActive) 1f else 0.5f)
    val thumbHighlightColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (isActive) 0.2f else 0.05f)

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
            color = surfaceColor,
            border = BorderStroke(if (isActive) 1.dp else 0.5.dp, crosshairColor),
            shadowElevation = if (isActive) 8.dp else 2.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (isActive) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = {
                                            thumbOffset = Offset.Zero
                                            onInput(JoystickInput(angle = 0f, magnitude = 0f))
                                        },
                                        onDragCancel = {
                                            thumbOffset = Offset.Zero
                                            onInput(JoystickInput(angle = 0f, magnitude = 0f))
                                        },
                                    ) { change, dragAmount ->
                                        // Bug 4 fix: accumulate with the dragAmount DELTA rather than
                                        // computing from the absolute touch position.  The absolute
                                        // approach rubber-bands whenever the coordinate origin
                                        // of the drag gesture doesn't match the joystick center.
                                        val rawOffset = thumbOffset + dragAmount
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
                                }
                            } else {
                                Modifier
                            }
                        ),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val thumbCenter = center + thumbOffset

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

                    drawCircle(
                        color = thumbGlow,
                        radius = thumbRadiusPx + 6.dp.toPx(),
                        center = thumbCenter,
                    )

                    drawCircle(
                        color = thumbColor,
                        radius = thumbRadiusPx,
                        center = thumbCenter,
                    )

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
