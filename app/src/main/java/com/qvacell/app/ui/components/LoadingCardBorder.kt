package com.qvacell.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

val LocalLoadingAnimationStyle = compositionLocalOf { "sweep" }

@Composable
fun LoadingCardBorder(
    isLoading: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable () -> Unit
) {
    if (!isLoading) {
        Box(modifier = modifier) { content() }
        return
    }

    val style = LocalLoadingAnimationStyle.current
    val transition = rememberInfiniteTransition(label = "card loading")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (style == "skeleton") 1500 else 1200, easing = LinearEasing),
            repeatMode = if (style == "bottom") RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "card loading progress"
    )
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val placeholder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val strokeWidth = 3.dp

    Box(modifier = modifier.padding(strokeWidth / 2).drawWithContent {
        val radius = 20.dp.toPx()
        val stroke = strokeWidth.toPx()
        if (style == "skeleton") {
            drawRoundRect(surface, cornerRadius = CornerRadius(radius, radius))
            val barHeight = 14.dp.toPx()
            val left = 20.dp.toPx()
            val available = (size.width - left * 2).coerceAtLeast(0f)
            val bars = listOf(
                Triple(20.dp.toPx(), available * 0.45f, barHeight),
                Triple(50.dp.toPx(), available * 0.72f, 26.dp.toPx()),
                Triple(91.dp.toPx(), available * 0.55f, barHeight)
            )
            if (size.height < 90.dp.toPx()) {
                val height = 12.dp.toPx()
                drawRoundRect(placeholder,
                    topLeft = Offset(left, (size.height - height) / 2f),
                    size = Size(available * 0.55f, height),
                    cornerRadius = CornerRadius(height / 2f, height / 2f))
            }
            bars.forEach { (top, width, height) ->
                if (top + height < size.height - 10.dp.toPx()) {
                    drawRoundRect(placeholder, topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2, height / 2))
                }
            }
            val shimmerWidth = size.width * 0.55f
            val shimmerX = progress * (size.width + shimmerWidth) - shimmerWidth
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, primary.copy(alpha = 0.11f), Color.Transparent),
                    startX = shimmerX, endX = shimmerX + shimmerWidth
                ),
                cornerRadius = CornerRadius(radius, radius)
            )
        } else {
            drawContent()
            when (style) {
                "bottom" -> {
                    val center = size.width / 2f
                    val halfWidth = (center - radius).coerceAtLeast(0f) * progress
                    if (halfWidth > 0f) {
                        drawLine(primary, Offset(center - halfWidth, size.height),
                            Offset(center + halfWidth, size.height), stroke, cap = StrokeCap.Round)
                    }
                }
                "perimeter" -> {
                    val inset = stroke / 2f
                    val outline = Path().apply {
                        addRoundRect(RoundRect(Rect(inset, inset,
                            size.width - inset, size.height - inset),
                            CornerRadius(radius, radius)))
                    }
                    val measure = PathMeasure().apply { setPath(outline, true) }
                    val length = measure.length
                    if (length > 0f) {
                        val start = progress * length
                        val end = start + length * 0.23f
                        val segment = Path()
                        measure.getSegment(start, end.coerceAtMost(length), segment)
                        drawPath(segment, primary, style = Stroke(width = stroke, cap = StrokeCap.Round))
                        if (end > length) {
                            val wrappedSegment = Path()
                            measure.getSegment(0f, end - length, wrappedSegment)
                            drawPath(wrappedSegment, primary,
                                style = Stroke(width = stroke, cap = StrokeCap.Round))
                        }
                    }
                }
                else -> {
                    val gradientWidth = size.width * 0.4f
                    val start = (-0.3f + progress * 1.6f) * size.width - gradientWidth / 2f
                    val brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, primary.copy(alpha = 0.8f), primary,
                            primary.copy(alpha = 0.8f), Color.Transparent),
                        startX = start, endX = start + gradientWidth
                    )
                    drawRoundRect(brush = brush, cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = stroke))
                }
            }
        }
    }) { content() }
}
