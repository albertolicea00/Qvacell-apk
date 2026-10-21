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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import com.qvacell.app.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes

@Composable
fun MainBalanceCard(
    @StringRes titleRes: Int = R.string.dashboard_balance,
    balance: String,
    currency: String,
    lineActiveUntil: String,
    accountDueDate: String,
    hasFetched: Boolean = true,
    lastUpdatedAt: Long? = null,
    onQuery: (() -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(if (onQuery != null) Modifier.dashboardQueryGesture(onQuery) else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            stringResource(titleRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    if (hasFetched) {
                        if (lineActiveUntil != "--") {
                            Text(
                                stringResource(R.string.dashboard_active_until, lineActiveUntil),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (accountDueDate != "--") {
                            Text(
                                stringResource(R.string.dashboard_expires, accountDueDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    TimeAgoText(lastUpdatedAt)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!hasFetched) {
                        Text(
                            stringResource(R.string.dashboard_not_checked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else {
                        val (wholePart, centsPart) = balance.split(".", limit = 2)
                            .let { it[0] to it.getOrElse(1) { "" } }
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 36.sp)) {
                                    append(wholePart)
                                }
                                if (centsPart.isNotEmpty()) {
                                    withStyle(
                                        SpanStyle(fontSize = 20.sp, baselineShift = BaselineShift.Superscript)
                                    ) {
                                        append(".$centsPart")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            currency.ifEmpty { "CUP" },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.offset(y = (-15).dp)
                        )
                    }
                }
            }
        }
    }
}
