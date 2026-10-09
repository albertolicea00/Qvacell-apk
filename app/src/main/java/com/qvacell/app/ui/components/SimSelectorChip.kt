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

    val currentIndex = activeSims.indexOfFirst { it.simSlotIndex == selectedSlot }
        .let { if (it == -1) 0 else it }
    val currentSim = activeSims[currentIndex]

    val simBitmap = remember(currentSim.subscriptionId) {
        try { currentSim.createIconBitmap(context) } catch (_: Exception) { null }
    }

    IconButton(onClick = { showSheet = true }) {
        if (simBitmap != null) {
            Image(
                bitmap = simBitmap.asImageBitmap(),
                contentDescription = SimUtils.simLabel(currentSim),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                Icons.Filled.SimCard,
                contentDescription = SimUtils.simLabel(currentSim),
                tint = MaterialTheme.colorScheme.onSurface
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
                activeSims.forEachIndexed { index, sim ->
                    val bitmap = remember(sim.subscriptionId) {
                        try { sim.createIconBitmap(context) } catch (_: Exception) { null }
                    }
                    val selected = sim.simSlotIndex == activeSims[currentIndex].simSlotIndex
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
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(28.dp)
                            )
                        } else {
                            Icon(
                                Icons.Filled.SimCard,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                SimUtils.simLabel(sim),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            @Suppress("DEPRECATION")
                            val number = sim.number?.takeIf { it.isNotBlank() }
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
