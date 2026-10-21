package com.qvacell.app.ui.components

import androidx.compose.ui.res.stringResource
import com.qvacell.app.R
import android.telephony.SubscriptionInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.service.SimUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimSelectorIcon() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val selectorPreferences by settings.simSelectorPreferences.collectAsStateWithLifecycle(initialValue = null)
    val simSwitchAction by settings.simSwitchAction.collectAsStateWithLifecycle(initialValue = "sheet")
    val scope = rememberCoroutineScope()

    var activeSims by remember { mutableStateOf(SimUtils.getActiveSubscriptions(context)) }
    var showSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editingSim by remember { mutableStateOf<SubscriptionInfo?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        activeSims = SimUtils.getActiveSubscriptions(context)
    }

    if (activeSims.size < 2 || selectorPreferences == null) return

    val selectedSlot = selectorPreferences!!.selectedSlot
    val appearances = selectorPreferences!!.appearances
    val effectiveSlot = SimUtils.resolveSelectedSlot(context, selectedSlot)
    val currentSim = activeSims.firstOrNull { it.simSlotIndex == effectiveSlot }
    val onSelectorClick: () -> Unit = {
        when (simSwitchAction) {
            "cycle" -> {
                val currentIndex = activeSims.indexOfFirst { it.simSlotIndex == effectiveSlot }
                val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % activeSims.size
                scope.launch { settings.setSelectedSimSlot(activeSims[nextIndex].simSlotIndex) }
            }
            "menu" -> showMenu = true
            else -> showSheet = true
        }
    }

    val currentAppearance = currentSim?.let { appearances[it.subscriptionId.toString()] }

    Box {
        Surface(
            onClick = onSelectorClick,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSim != null) {
                    SimBadge(
                        currentSim.simSlotIndex,
                        currentAppearance,
                        width = 17.dp,
                        height = 21.dp,
                        iconScale = 0.58f
                    )
                } else {
                    Icon(Icons.Filled.SimCard, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = currentSim?.let { SimUtils.simLabel(it, currentAppearance) }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        if (simSwitchAction == "menu") {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                activeSims.forEach { sim ->
                    val appearance = appearances[sim.subscriptionId.toString()]
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(SimUtils.simLabel(sim, appearance))
                                Text(
                                    stringResource(
                                        if (appearance?.isCorporate == true) R.string.sim_corporate_line
                                        else R.string.sim_personal_line
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = { SimBadge(sim.simSlotIndex, appearance) },
                        trailingIcon = if (sim.simSlotIndex == effectiveSlot) ({
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }) else null,
                        onClick = {
                            scope.launch { settings.setSelectedSimSlot(sim.simSlotIndex) }
                            showMenu = false
                        }
                    )
                }
            }
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
                    stringResource(R.string.sim_select),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                activeSims.forEach { sim ->
                    val appearance = appearances[sim.subscriptionId.toString()]
                    val selected = sim.simSlotIndex == effectiveSlot
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
                        SimBadge(sim.simSlotIndex, appearance, modifier = Modifier.padding(start = 8.dp))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                SimUtils.simLabel(sim, appearance),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val lineType = stringResource(
                                if (appearance?.isCorporate == true) R.string.sim_corporate_line
                                else R.string.sim_personal_line
                            )
                            val carrier = sim.carrierName?.toString()?.takeIf { it.isNotBlank() }
                            val number = SimUtils.getPhoneNumber(context, sim)
                            val subtitle = listOfNotNull(lineType, carrier, number).joinToString(" · ")
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = {
                            showSheet = false
                            editingSim = sim
                        }) { Text(stringResource(R.string.sim_customize)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    editingSim?.let { sim ->
        SimAppearanceEditor(
            sim = sim,
            appearance = appearances[sim.subscriptionId.toString()] ?: SimAppearance(),
            showDefaultOption = activeSims.size >= 2,
            isDefault = sim.simSlotIndex == effectiveSlot,
            onMakeDefault = { scope.launch { settings.setSelectedSimSlot(sim.simSlotIndex) } },
            onSave = { appearance ->
                scope.launch {
                    settings.setSimAppearance(sim.subscriptionId, appearance)
                    editingSim = null
                }
            },
            onDismiss = { editingSim = null }
        )
    }
}
