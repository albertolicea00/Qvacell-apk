package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.R
import java.time.LocalDate

val LocalStaleDaysThreshold = compositionLocalOf { 14 }

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

private val ENGLISH_MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** Formats date e.g. "20 Ago 2027" or "20 Aug 2027" according to device locale. */
@Composable
internal fun formatSpanishDate(date: LocalDate): String {
    val isEnglish = LocalConfiguration.current.locales[0].language.startsWith("en", ignoreCase = true)
    val months = if (isEnglish) ENGLISH_MONTH_ABBREVIATIONS else SPANISH_MONTH_ABBREVIATIONS
    return "${date.dayOfMonth} ${months[date.monthValue - 1]} ${date.year}"
}

@Composable
internal fun formatTimeAgo(epochMillis: Long?): String {
    if (epochMillis == null) return stringResource(R.string.dashboard_not_updated)
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> stringResource(R.string.dashboard_updated_just_now)
        minutes < 60 -> stringResource(R.string.dashboard_updated_minutes_ago, minutes)
        hours < 24 -> stringResource(R.string.dashboard_updated_hours_ago, hours)
        days == 1L -> stringResource(R.string.dashboard_updated_yesterday)
        else -> stringResource(R.string.dashboard_updated_days_ago, days)
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
