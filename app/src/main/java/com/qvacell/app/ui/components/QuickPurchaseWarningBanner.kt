package com.qvacell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Compras-only, pinned banner — Ajustes' "Acción sin Confirmación" is on, so a tap dials
 *  straight through with no in-app confirmation step. Ported from iOS's QuickPurchaseWarningBannerView. */
@Composable
fun QuickPurchaseWarningBanner() {
    val warningColor = Color(0xFFFB8C00)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(warningColor.copy(alpha = 0.15f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = warningColor, modifier = Modifier.size(16.dp))
        Text(
            "Acción sin Confirmación activada — las compras se marcan de una vez, sin pedir confirmación",
            style = MaterialTheme.typography.labelMedium,
            color = warningColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
