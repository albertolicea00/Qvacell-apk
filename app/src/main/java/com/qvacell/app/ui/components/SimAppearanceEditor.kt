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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qvacell.app.R
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.service.SimUtils
import java.util.Locale

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
    var showCustomColorPicker by remember(sim.subscriptionId, appearance) { mutableStateOf(colorHex !in simColors) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SimCardPreview(
                slotIndex = sim.simSlotIndex,
                name = name.trim().ifBlank { SimUtils.simLabel(sim) },
                icon = icon,
                color = Color(android.graphics.Color.parseColor(colorHex)),
                isCorporate = isCorporate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            RoundedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = stringResource(R.string.sim_custom_name)
            )
            Text(stringResource(R.string.sim_line_type), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !isCorporate,
                    onClick = { isCorporate = false },
                    label = { Text(stringResource(R.string.sim_standard_line)) },
                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isCorporate,
                    onClick = { isCorporate = true },
                    label = { Text(stringResource(R.string.sim_corporate_line)) },
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 28.dp, bottomEnd = 28.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(stringResource(R.string.sim_custom_icon), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "number" to R.string.sim_icon_number,
                    "home" to R.string.sim_icon_home,
                    "work" to R.string.sim_icon_work,
                    "favorite" to R.string.sim_icon_favorite,
                    "star" to R.string.sim_icon_star,
                    "travel" to R.string.sim_icon_travel
                ).forEach { (choice, label) ->
                    val selected = icon == choice
                    val iconDescription = stringResource(label)
                    Surface(
                        onClick = { icon = choice },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val image = simIcon(choice)
                            if (image == null) {
                                Text(
                                    "#",
                                    modifier = Modifier.semantics { contentDescription = iconDescription },
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                Icon(
                                    image,
                                    contentDescription = stringResource(label),
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.sim_custom_color), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                simColors.forEach { choice ->
                    val choiceColor = Color(android.graphics.Color.parseColor(choice))
                    Surface(
                        onClick = {
                            colorHex = choice
                            showCustomColorPicker = false
                        },
                        shape = CircleShape,
                        color = choiceColor,
                        border = if (colorHex == choice) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                        modifier = Modifier.size(40.dp)
                    ) {}
                }
                val customColor = Color(android.graphics.Color.parseColor(colorHex))
                Surface(
                    onClick = { showCustomColorPicker = true },
                    shape = CircleShape,
                    color = if (showCustomColorPicker) customColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (showCustomColorPicker) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_custom),
                            tint = if (showCustomColorPicker && customColor.luminance() <= 0.4f) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            if (showCustomColorPicker) {
                Text(
                    "#%06X".format(Locale.ROOT, 0xFFFFFF and Color(android.graphics.Color.parseColor(colorHex)).toArgb()),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                ColorWheelPicker(
                    color = Color(android.graphics.Color.parseColor(colorHex)),
                    onColorChange = { selected ->
                        colorHex = "#%06X".format(Locale.ROOT, 0xFFFFFF and selected.toArgb())
                    },
                    modifier = Modifier.fillMaxWidth()
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

@Composable
fun SimCardGlyph(
    slotIndex: Int,
    icon: String,
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val foreground = if (color.luminance() > 0.4f) Color.Black else Color.White
    Surface(
        modifier = modifier.size(width = width, height = height),
        shape = CutCornerShape(
            topStart = width * 0.08f,
            topEnd = width * 0.26f,
            bottomEnd = width * 0.08f,
            bottomStart = width * 0.08f
        ),
        color = color,
        shadowElevation = if (width > 50.dp) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            val image = simIcon(icon)
            if (image == null) {
                Text(
                    "${slotIndex + 1}",
                    color = foreground,
                    style = if (width > 50.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelLarge
                )
            } else {
                Icon(
                    image,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(if (width > 50.dp) 28.dp else width * 0.48f)
                )
            }
        }
    }
}

@Composable
private fun SimCardPreview(
    slotIndex: Int,
    name: String,
    icon: String,
    color: Color,
    isCorporate: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
                SimCardGlyph(slotIndex, icon, color, width = 42.dp, height = 52.dp)
        Text(
            stringResource(if (isCorporate) R.string.sim_corporate_line else R.string.sim_personal_line),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.sim_named_slot, name, slotIndex + 1),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
