package com.qvacell.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HSV wheel (hue = angle, saturation = distance from center) plus a brightness slider below —
 * replaces typing a raw #RRGGBB hex for Ajustes › Color de acento.
 */
@Composable
fun ColorWheelPicker(color: Color, onColorChange: (Color) -> Unit, modifier: Modifier = Modifier) {
    val initialHsv = remember(color) {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), out)
        out
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun updateFromOffset(offset: Offset, size: IntSize) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val radius = min(center.x, center.y)
        if (radius <= 0f) return
        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) angle += 360f
        hue = angle
        saturation = dist / radius
        onColorChange(Color.hsv(hue, saturation, value))
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> updateFromOffset(offset, size) }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> updateFromOffset(offset, size) }
                    ) { change, _ -> updateFromOffset(change.position, size) }
                }
        ) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = (0..12).map { Color.hsv(it * 30f, 1f, 1f) },
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            val angleRad = Math.toRadians(hue.toDouble())
            val dist = saturation * radius
            val dot = Offset(
                x = center.x + (cos(angleRad) * dist).toFloat(),
                y = center.y + (sin(angleRad) * dist).toFloat()
            )
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = dot, style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = 10.dp.toPx(), center = dot, style = Stroke(width = 1.dp.toPx()))
        }

        Slider(
            value = value,
            onValueChange = {
                value = it
                onColorChange(Color.hsv(hue, saturation, value))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.hsv(hue, saturation, 1f),
                activeTrackColor = Color.hsv(hue, saturation, 1f)
            )
        )
    }
}
