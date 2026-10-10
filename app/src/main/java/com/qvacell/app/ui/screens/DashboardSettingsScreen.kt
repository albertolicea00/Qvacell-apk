package com.qvacell.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DialService
import com.qvacell.app.ui.components.BackNavigationIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSettingsScreen(onBack: () -> Unit, onNavigateToHelp: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsDataStore(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }

    val cardTapMode by settings.cardTapMode.collectAsStateWithLifecycle(initialValue = "tap_confirm")
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smsCaptureEnabled by settings.smsCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hasRunSmsBackfill by settings.hasRunSmsBackfill.collectAsStateWithLifecycle(initialValue = false)

    var showCardTapModeSheet by remember { mutableStateOf(false) }

    val cardTapModeLabel = when (cardTapMode) {
        "long_confirm" -> "Presionar + confirmación"
        "long" -> "Solo Presionar"
        "tap" -> "Un tap"
        "double_tap" -> "Doble tap"
        "triple_tap" -> "Triple tap"
        else -> "Un tap + confirmación"
    }

    val ussdCapturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settings.setUssdCaptureEnabled(granted) }
    }

    val smsCallLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        scope.launch { settings.setSmsCaptureEnabled(granted) }
        DashboardCapture.scheduleEstimationIfEnabled(context, granted)
        if (granted && !hasRunSmsBackfill) {
            DashboardCapture.triggerSmsBackfill(context, dashboardRepository)
            scope.launch { settings.setHasRunSmsBackfill(true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text("Dashboard Dinámico") },
                navigationIcon = { BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            OptionsSection(header = "Interacción") {
                OptionsRow(
                    headline = "Acción de consulta USSD",
                    supporting = cardTapModeLabel,
                    onClick = { showCardTapModeSheet = true }
                )
            }

            OptionsSection(header = "Captura de datos (experimental)") {
                OptionsRow(
                    headline = "Consulta automática de saldo",
                    supporting = "Lee la respuesta USSD sin abrir el marcador, usando el mismo permiso de Compras.",
                    trailingContent = {
                        Switch(
                            checked = ussdCaptureEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !DialService.hasCallPermission(context)) {
                                    ussdCapturePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                } else {
                                    scope.launch { settings.setUssdCaptureEnabled(checked) }
                                }
                            }
                        )
                    }
                )
                OptionsDivider()
                OptionsRow(
                    headline = "Detección automática por SMS",
                    supporting = "Lee los SMS de saldo de ETECSA (nuevos e historial) y el registro de llamadas para estimar tu consumo.",
                    trailingContent = {
                        Switch(
                            checked = smsCaptureEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    smsCallLogPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.READ_CALL_LOG
                                        )
                                    )
                                } else {
                                    scope.launch { settings.setSmsCaptureEnabled(false) }
                                }
                            }
                        )
                    }
                )
            }

            OptionsSection(header = "") {
                OptionsRow(
                    headline = "Ayuda del Dashboard",
                    supporting = "Conoce cómo funciona el dashboard dinámico",
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = onNavigateToHelp
                )
            }
        }
    }

    if (showCardTapModeSheet) {
        ModalBottomSheet(onDismissRequest = { showCardTapModeSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Acción de consulta USSD",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Define cómo interactuar con las cards para realizar la marcación USSD.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                listOf(
                    "tap_confirm" to "Un tap + confirmación",
                    "long_confirm" to "Presionar + confirmación",
                    "long" to "Solo Presionar",
                    "tap" to "Un tap",
                    "double_tap" to "Doble tap",
                    "triple_tap" to "Triple tap"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { settings.setCardTapMode(mode) }
                                showCardTapModeSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = cardTapMode == mode, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
