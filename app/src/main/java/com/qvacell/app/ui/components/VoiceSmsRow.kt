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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import com.qvacell.app.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun VoiceSmsRow(
    voiceDaysRemaining: String,
    voiceDuration: String,
    smsDaysRemaining: String,
    smsCount: String,
    voiceHasFetched: Boolean = true,
    smsHasFetched: Boolean = true,
    voiceLastUpdatedAt: Long? = null,
    smsLastUpdatedAt: Long? = null,
    voiceIsLoading: Boolean = false,
    smsIsLoading: Boolean = false,
    onVoiceQuery: (() -> Unit)? = null,
    onSmsQuery: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LoadingCardBorder(isLoading = voiceIsLoading, modifier = Modifier.weight(1f)) {
            UsageStatCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Mic,
                accentColor = MaterialTheme.colorScheme.primary,
                badge = voiceDaysRemaining,
                label = stringResource(R.string.dashboard_minutes),
                value = voiceDuration,
                hasFetched = voiceHasFetched,
                lastUpdatedAt = voiceLastUpdatedAt,
                onQuery = onVoiceQuery
            )
        }
        LoadingCardBorder(isLoading = smsIsLoading, modifier = Modifier.weight(1f)) {
            UsageStatCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Sms,
                accentColor = MaterialTheme.colorScheme.primary,
                badge = smsDaysRemaining,
                label = stringResource(R.string.dashboard_sms),
                value = smsCount,
                hasFetched = smsHasFetched,
                lastUpdatedAt = smsLastUpdatedAt,
                onQuery = onSmsQuery
            )
        }
    }
}

@Composable
private fun UsageStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accentColor: Color,
    badge: String,
    label: String,
    value: String,
    hasFetched: Boolean = true,
    lastUpdatedAt: Long? = null,
    onQuery: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier
            .clip(cardShape)
            .then(if (onQuery != null) Modifier.clickable { onQuery() } else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (!hasFetched) {
                    Text(
                        stringResource(R.string.dashboard_not_checked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (value == "--") {
                    Text(
                        stringResource(R.string.dashboard_no_plan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    val unitColor = MaterialTheme.colorScheme.onSurface
                    Text(
                        buildAnnotatedString {
                            value.forEach { char ->
                                val style = if (char.isLetter()) {
                                    SpanStyle(color = unitColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    SpanStyle(color = accentColor)
                                }
                                withStyle(style) { append(char) }
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    val expiryDays = badge.filter { it.isDigit() }.toIntOrNull()
                    if (expiryDays != null) {
                        Text(
                            stringResource(R.string.dashboard_expires, formatSpanishDate(LocalDate.now().plusDays(expiryDays.toLong()))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                TimeAgoText(lastUpdatedAt)
            }
        }
    }
}
