package com.qvacell.app.ui.components

import android.telephony.SubscriptionInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.SimUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimSelectorIcon() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val selectedSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    val scope = rememberCoroutineScope()

    var activeSims by remember { mutableStateOf<List<SubscriptionInfo>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activeSims = SimUtils.getActiveSubscriptions(context)
    }

    if (activeSims.size < 2) return

    val isDefault = selectedSlot < 0
    val currentIndex = if (isDefault) -1 else activeSims.indexOfFirst { it.simSlotIndex == selectedSlot }
    val currentSim = if (currentIndex >= 0) activeSims[currentIndex] else null

    val currentNumber = remember(currentSim, activeSims) {
        currentSim?.let { SimUtils.getPhoneNumber(context, it) }
    }

    Surface(
        onClick = { showSheet = true },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.SimCard,
                contentDescription = currentSim?.let { SimUtils.simLabel(context, it) } ?: "Predeterminada",
                modifier = Modifier.size(18.dp),
                tint = currentSim?.let { SimUtils.simColor(it.simSlotIndex) } ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isDefault) "Auto" else (currentNumber ?: "SIM ${(currentSim?.simSlotIndex ?: 0) + 1}"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Seleccionar SIM",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Opción Predeterminada
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { settings.setSelectedSimSlot(-1) }
                            showSheet = false
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isDefault, onClick = null)
                    Icon(
                        Icons.Filled.SimCard,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "Predeterminada",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Usar ajuste o selección del sistema",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                activeSims.forEachIndexed { index, sim ->
                    val bitmap = remember(sim.subscriptionId) {
                        try { sim.createIconBitmap(context) } catch (_: Exception) { null }
                    }
                    val selected = !isDefault && sim.simSlotIndex == selectedSlot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setSelectedSimSlot(sim.simSlotIndex) }
                                showSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Icon(
                            Icons.Filled.SimCard,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(28.dp),
                            tint = SimUtils.simColor(sim.simSlotIndex)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                SimUtils.simLabel(sim),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            val number = SimUtils.getPhoneNumber(context, sim)
                            val subtitle = listOfNotNull(carrier, number).joinToString(" · ")
                            if (subtitle.isNotBlank() && subtitle != SimUtils.simLabel(sim)) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
