package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

val LocalStaleDaysThreshold = compositionLocalOf { 14 }

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/** "20 Ago 2027" — matches the date style used across dashboard cards. */
internal fun formatSpanishDate(date: LocalDate): String =
    "${date.dayOfMonth} ${SPANISH_MONTH_ABBREVIATIONS[date.monthValue - 1]} ${date.year}"

internal fun formatTimeAgo(epochMillis: Long?): String {
    if (epochMillis == null) return "No actualizado"
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Actualizado justo ahora"
        minutes < 60 -> "Actualizado hace ${minutes}min"
        hours < 24 -> "Actualizado hace ${hours}h"
        days == 1L -> "Actualizado hace 1 día"
        else -> "Actualizado hace $days días"
    }
}

@Composable
internal fun TimeAgoText(
    epochMillis: Long?,
    modifier: Modifier = Modifier
) {
    val staleDays = LocalStaleDaysThreshold.current
    val text = formatTimeAgo(epochMillis)
    val color = when {
        epochMillis == null -> Color(0xFFE53935)
        (System.currentTimeMillis() - epochMillis) / 86_400_000 >= staleDays ->
            Color(0xFFFB8C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = color,
        modifier = modifier.padding(top = 2.dp)
    )
}
