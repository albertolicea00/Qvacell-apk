package com.qvacell.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qvacell.app.model.UssdCode
import com.qvacell.app.ui.resolveAndroidIcon

@Composable
fun CodeRow(
    code: UssdCode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showDescription: Boolean = true,
    plainPrice: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            Icon(
                imageVector = resolveAndroidIcon(code.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(code.title.value, style = MaterialTheme.typography.bodyLarge)
            if (showDescription) {
                Text(
                    code.details.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (code.isSubscription == true) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "Suscripción",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        // Codes with options/variants open a picker sheet instead of dialing directly — the
        // parent code's price is shown per-option inside that sheet, not repeated out here.
        val hasOptionsOrVariants = !code.options.isNullOrEmpty() || !code.variants.isNullOrEmpty()
        val showPrice = code.price != null && !isZeroPrice(code.price) && !hasOptionsOrVariants
        if (plainPrice) {
            if (showPrice || !hasOptionsOrVariants) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showPrice) {
                        Text(
                            code.price!!,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!hasOptionsOrVariants) {
                        Icon(
                            Icons.Filled.ArrowOutward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = if (showPrice) 4.dp else 0.dp)
                                .size(14.dp)
                        )
                    }
                }
            }
        } else if (showPrice) {
            PriceChip(price = code.price!!)
        }
    }
}

/** "$0.00" → true — catalog codes priced at zero shouldn't display a price at all. */
internal fun isZeroPrice(price: String): Boolean =
    price.filter { it.isDigit() || it == '.' }.toDoubleOrNull() == 0.0

/** Small rounded price tag, e.g. "$4.00" — shared between `CodeRow` and `CodeOptionsSheet` rows. */
@Composable
fun PriceChip(price: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            price,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
