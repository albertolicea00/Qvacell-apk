package com.qvacell.app.ui.components

import android.telephony.SubscriptionInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.service.SimUtils

private val simColors = listOf("#FFB300", "#E91E63", "#1976D2", "#008577", "#7B1FA2", "#D84315")

private fun simIcon(icon: String): ImageVector? = when (icon) {
    "home" -> Icons.Filled.Home
    "work" -> Icons.Filled.Work
    "favorite" -> Icons.Filled.Favorite
    "star" -> Icons.Filled.Star
    "travel" -> Icons.Filled.Public
    else -> null
}

@Composable
fun SimBadge(slotIndex: Int, appearance: SimAppearance?, modifier: Modifier = Modifier) {
    val color = SimUtils.simColor(slotIndex, appearance)
    val foreground = if (color.luminance() > 0.4f) Color.Black else Color.White
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(9.dp), color = color, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                val icon = simIcon(appearance?.icon ?: "number")
                if (icon == null) {
                    Text("${slotIndex + 1}", color = foreground, style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SimAppearanceEditor(
    sim: SubscriptionInfo,
    appearance: SimAppearance,
    onSave: (SimAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(sim.subscriptionId, appearance) { mutableStateOf(appearance.name) }
    var colorHex by remember(sim.subscriptionId, appearance) {
        mutableStateOf(appearance.colorHex ?: if (sim.simSlotIndex == 1) "#E91E63" else "#FFB300")
    }
    var icon by remember(sim.subscriptionId, appearance) { mutableStateOf(appearance.icon) }
    var isCorporate by remember(sim.subscriptionId, appearance) { mutableStateOf(appearance.isCorporate) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.sim_customize_title, sim.simSlotIndex + 1), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text(stringResource(R.string.sim_custom_name)) },
                placeholder = { Text(SimUtils.simLabel(sim)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.sim_custom_color), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                simColors.forEach { choice ->
                    val choiceColor = Color(android.graphics.Color.parseColor(choice))
                    Surface(
                        onClick = { colorHex = choice },
                        shape = CircleShape,
                        color = choiceColor,
                        border = if (colorHex == choice) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier.size(40.dp)
                    ) {}
                }
            }
            Text(stringResource(R.string.sim_custom_icon), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "number" to R.string.sim_icon_number,
                    "home" to R.string.sim_icon_home,
                    "work" to R.string.sim_icon_work,
                    "favorite" to R.string.sim_icon_favorite,
                    "star" to R.string.sim_icon_star,
                    "travel" to R.string.sim_icon_travel
                ).forEach { (choice, label) ->
                    FilterChip(
                        selected = icon == choice,
                        onClick = { icon = choice },
                        label = { Text(stringResource(label)) },
                        leadingIcon = simIcon(choice)?.let { image ->
                            { Icon(image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        }
                    )
                }
            }
            Text(stringResource(R.string.sim_line_type), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !isCorporate,
                    onClick = { isCorporate = false },
                    label = { Text(stringResource(R.string.sim_standard_line)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isCorporate,
                    onClick = { isCorporate = true },
                    label = { Text(stringResource(R.string.sim_corporate_line)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.size(2.dp))
            Button(
                onClick = { onSave(SimAppearance(name.trim(), colorHex, icon, isCorporate)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sim_custom_save))
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}
