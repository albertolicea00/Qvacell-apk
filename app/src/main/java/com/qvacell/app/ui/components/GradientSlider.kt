package com.qvacell.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.height(28.dp)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbR = with(density) { 10.dp.toPx() }
        val trackH = with(density) { 12.dp.toPx() }
        val trackStart = thumbR
        val trackEnd = widthPx - thumbR
        val trackLen = (trackEnd - trackStart).coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .height(28.dp)
                .matchParentSize()
                .pointerInput(widthPx, trackStart, trackLen) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun update(x: Float) {
                            onValueChange(((x - trackStart) / trackLen).coerceIn(0f, 1f))
                        }
                        update(down.position.x)
                        down.consume()
                        var ch = awaitDragOrCancellation(down.id)
                        while (ch != null) {
                            update(ch.position.x)
                            ch.consume()
                            ch = awaitDragOrCancellation(ch.id)
                        }
                    }
                }
        ) {
            val cy = size.height / 2f
            val r = trackH / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(colors, startX = trackStart, endX = trackEnd),
                topLeft = Offset(trackStart, cy - r),
                size = Size(trackLen, trackH),
                cornerRadius = CornerRadius(r)
            )
            val tx = (trackStart + value * trackLen).coerceIn(trackStart, trackEnd)
            drawCircle(Color.White, thumbR, Offset(tx, cy))
            drawCircle(Color.Black.copy(alpha = 0.15f), thumbR, Offset(tx, cy), style = Stroke(2f))
        }
    }
}
