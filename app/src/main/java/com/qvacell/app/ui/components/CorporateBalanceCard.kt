package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.R

@Composable
fun CorporateBalanceCard(
    voice: String,
    voiceConsumed: String,
    sms: String,
    smsConsumed: String,
    data: String,
    roaming: String,
    lastUpdatedAt: Long?,
    onQuery: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier.fillMaxWidth().clip(shape).dashboardQueryGesture(onQuery),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.dashboard_corporate_balance),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            TimeAgoText(lastUpdatedAt, modifier = Modifier.offset(y = (-6).dp))
            SectionDivider()

            Column {
                CorporateMetricSection(
                    icon = Icons.Filled.Sms,
                    title = stringResource(R.string.dashboard_corporate_sms),
                    value = sms
                )
                CorporateConsumptionRow(
                    label = stringResource(R.string.dashboard_corporate_sms_consumed),
                    value = smsConsumed,
                    verticalOffset = (-7).dp
                )
            }

            SectionDivider()

            Column {
                CorporateMetricSection(
                    icon = Icons.Filled.Mic,
                    title = stringResource(R.string.dashboard_corporate_voice),
                    value = voice,
                    prominent = false
                )
                CorporateConsumptionRow(
                    label = stringResource(R.string.dashboard_corporate_voice_consumed),
                    value = voiceConsumed,
                    verticalOffset = 0.dp
                )
            }

            SectionDivider()

            CorporateDataSection(
                icon = Icons.Filled.NetworkCell,
                title = stringResource(R.string.dashboard_corporate_data),
                value = data
            )

            SectionDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        stringResource(R.string.dashboard_corporate_roaming),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 7.dp)
                    )
                }
                val roamingValue = roaming.ifBlank { "--" }
                Text(
                    roamingValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (roamingValue == "--") FontWeight.Normal else FontWeight.Bold,
                    color = if (roamingValue == "--") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }

        }
    }
}

@Composable
private fun CorporateDataSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    val match = Regex("^\\s*([\\d.,]+)\\s*(.*)$").matchEntire(value)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 7.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (match != null) {
                Text(match.groupValues[1], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (match.groupValues[2].isNotBlank()) {
                    Text(match.groupValues[2], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                }
            } else {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = if (value.trim() == "--") FontWeight.Normal else FontWeight.Bold,
                    color = if (value.trim() == "--") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CorporateMetricSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    prominent: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
        Text(value,
            style = if (prominent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = if (value.trim() == "--") FontWeight.Normal else FontWeight.Bold,
            color = if (value.trim() == "--") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.primary
        )
    }
}

/** Secondary pay-as-you-go style line for the usage amount consumed in CUP. */
@Composable
private fun CorporateConsumptionRow(label: String, value: String, verticalOffset: androidx.compose.ui.unit.Dp = (-3).dp) {
    Row(
        modifier = Modifier.offset(y = verticalOffset),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (value.trim() == "--") FontWeight.Normal else FontWeight.SemiBold,
            color = if (value.trim() == "--") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 22.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    )
}
