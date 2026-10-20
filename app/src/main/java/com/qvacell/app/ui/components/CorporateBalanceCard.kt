package com.qvacell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.dashboard_corporate_balance), style = MaterialTheme.typography.titleMedium)
            TimeAgoText(lastUpdatedAt)
            CorporateMetric(stringResource(R.string.dashboard_corporate_voice), voice)
            CorporateMetric(stringResource(R.string.dashboard_corporate_voice_consumed), voiceConsumed)
            CorporateMetric(stringResource(R.string.dashboard_corporate_sms), sms)
            CorporateMetric(stringResource(R.string.dashboard_corporate_sms_consumed), smsConsumed)
            CorporateMetric(stringResource(R.string.dashboard_corporate_data), data)
            CorporateMetric(stringResource(R.string.dashboard_corporate_roaming), roaming)
        }
    }
}

@Composable
private fun CorporateMetric(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
