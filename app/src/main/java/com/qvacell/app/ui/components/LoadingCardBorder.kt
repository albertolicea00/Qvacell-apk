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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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
    val transition = rememberInfiniteTransition(label = "loading")
    val sweepPosition by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeWidth = 3.dp
    Box(
        modifier = modifier
            .padding(strokeWidth / 2)
            .drawWithContent {
                drawContent()
                val sw = strokeWidth.toPx()
                val radius = 20.dp.toPx()
                val gradientWidth = size.width * 0.4f
                val start = sweepPosition * size.width - gradientWidth / 2
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    startX = start,
                    endX = start + gradientWidth
                )
                drawRoundRect(
                    brush = brush,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = sw)
                )
            }
    ) {
        content()
    }
}
