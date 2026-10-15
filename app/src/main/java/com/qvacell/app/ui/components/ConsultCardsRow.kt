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
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.R

@Composable
fun CorporateBalanceConsultCard(onQuery: () -> Unit) {
    ConsultCard(stringResource(R.string.dashboard_corporate_balance), Icons.Filled.CreditCard, Modifier.fillMaxWidth(), onQuery)
}

@Composable
fun ConsultCardsRow(
    tfaIsLoading: Boolean = false,
    planAmigoIsLoading: Boolean = false,
    onTfaQuery: () -> Unit = {},
    onPlanAmigoQuery: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoadingCardBorder(isLoading = tfaIsLoading) {
            ConsultCard("TFA", Icons.Filled.PhoneAndroid, Modifier.fillMaxWidth(), onTfaQuery)
        }
        LoadingCardBorder(isLoading = planAmigoIsLoading) { ConsultCard(
            title = stringResource(R.string.dashboard_friends_plan_card),
            icon = Icons.Filled.People,
            modifier = Modifier.fillMaxWidth(),
            onClick = onPlanAmigoQuery
        ) }
    }
}

@Composable
private fun ConsultCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            ComingSoonBadge()
        }
    }
}

@Composable
private fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            stringResource(R.string.action_coming_soon),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Icon(
            Icons.Filled.ArrowOutward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(11.dp)
        )
    }
}
