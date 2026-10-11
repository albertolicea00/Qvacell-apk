package com.qvacell.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** "24-10-2026" (dd-MM-yyyy) — the format Options/backend hand us for this date; null on parse failure. */
private fun parseDmyDate(value: String): LocalDate? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

@Composable
fun RechargeLimitCard(
    reached: Boolean,
    limitAmount: String,
    availableFrom: String,
    lastUpdatedAt: Long? = null,
    onQuery: (() -> Unit)? = null
) {
    if (!reached) return
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(if (onQuery != null) Modifier.clickable { onQuery() } else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val parsedAvailableDate = parseDmyDate(availableFrom)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Límite Mensual ($limitAmount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (parsedAvailableDate != null) {
                    Text(
                        "Puede Recargar el ${formatSpanishDate(parsedAvailableDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TimeAgoText(lastUpdatedAt)
            }
            val daysUntilAvailable = parsedAvailableDate?.let {
                ChronoUnit.DAYS.between(LocalDate.now(), it)
            }
            if (daysUntilAvailable != null) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        ) {
                            append("$daysUntilAvailable")
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(if (daysUntilAvailable == 1L) " día" else " días")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
