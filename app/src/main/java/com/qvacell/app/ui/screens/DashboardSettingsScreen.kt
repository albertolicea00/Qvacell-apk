package com.qvacell.app.ui.screens

import androidx.compose.ui.res.stringResource
import com.qvacell.app.R
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.core.content.ContextCompat
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
    val loadingStyle by settings.dashboardLoadingStyle.collectAsStateWithLifecycle(initialValue = "sweep")
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smsCaptureEnabled by settings.smsCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val smsHistoryEnabled by settings.smsHistoryEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hasRunSmsBackfill by settings.hasRunSmsBackfill.collectAsStateWithLifecycle(initialValue = false)

    var showCardTapModeSheet by remember { mutableStateOf(false) }
    var showLoadingStyleSheet by remember { mutableStateOf(false) }
    var historyImporting by remember { mutableStateOf(false) }

    val cardTapModeLabel = when (cardTapMode) {
        "long_confirm" -> stringResource(R.string.dashboard_setting_long_confirm)
        "long" -> stringResource(R.string.dashboard_setting_long)
        "tap" -> stringResource(R.string.dashboard_setting_tap)
        "double_tap" -> stringResource(R.string.dashboard_setting_double_tap)
        "triple_tap" -> stringResource(R.string.dashboard_setting_triple_tap)
        else -> stringResource(R.string.dashboard_setting_tap_confirm)
    }

    val ussdCapturePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { settings.setUssdCaptureEnabled(granted) }
    }

    val newSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        val callLogGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        scope.launch { settings.setSmsCaptureEnabled(smsGranted) }
        DashboardCapture.scheduleEstimationIfEnabled(context, smsGranted && callLogGranted)
    }

    fun enableSmsHistory() {
        if (historyImporting) return
        historyImporting = true
        scope.launch {
            try {
                if (hasRunSmsBackfill) {
                    settings.setSmsHistoryEnabled(true)
                } else if (DashboardCapture.triggerSmsBackfill(context, dashboardRepository)) {
                    settings.setHasRunSmsBackfill(true)
                    settings.setSmsHistoryEnabled(true)
                } else {
                    Toast.makeText(context, context.getString(R.string.dashboard_setting_sms_history_error), Toast.LENGTH_SHORT).show()
                }
            } finally {
                historyImporting = false
            }
        }
    }

    val smsHistoryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableSmsHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.title_dashboard_settings)) },
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
            OptionsSection(header = stringResource(R.string.dashboard_setting_interaction)) {
                OptionsRow(
                    headline = stringResource(R.string.dashboard_setting_ussd_action),
                    supporting = cardTapModeLabel,
                    onClick = { showCardTapModeSheet = true }
                )
                OptionsDivider()
                OptionsRow(
                    headline = stringResource(R.string.dashboard_setting_loading),
                    supporting = when (loadingStyle) {
                        "bottom" -> stringResource(R.string.dashboard_setting_center_line)
                        "perimeter" -> stringResource(R.string.dashboard_setting_circular)
                        "skeleton" -> stringResource(R.string.dashboard_setting_skeleton)
                        else -> stringResource(R.string.dashboard_setting_animated_border)
                    },
                    onClick = { showLoadingStyleSheet = true }
                )
            }

            OptionsSection(header = stringResource(R.string.dashboard_setting_capture)) {
                OptionsRow(
                    headline = stringResource(R.string.dashboard_setting_auto_balance),
                    supporting = stringResource(R.string.dashboard_setting_auto_balance_desc),
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
                    headline = stringResource(R.string.dashboard_setting_new_sms),
                    supporting = stringResource(R.string.dashboard_setting_new_sms_desc),
                    trailingContent = {
                        Switch(
                            checked = smsCaptureEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    newSmsPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_CALL_LOG
                                        )
                                    )
                                } else {
                                    scope.launch { settings.setSmsCaptureEnabled(false) }
                                    DashboardCapture.scheduleEstimationIfEnabled(context, false)
                                }
                            }
                        )
                    }
                )
                OptionsDivider()
                OptionsRow(
                    headline = stringResource(R.string.dashboard_setting_sms_history),
                    supporting = if (historyImporting) stringResource(R.string.dashboard_setting_importing) else
                        stringResource(R.string.dashboard_setting_sms_history_desc),
                    trailingContent = {
                        Switch(
                            checked = smsHistoryEnabled,
                            enabled = !historyImporting,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (hasRunSmsBackfill || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                                        PackageManager.PERMISSION_GRANTED
                                    ) enableSmsHistory()
                                    else smsHistoryPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                } else {
                                    scope.launch { settings.setSmsHistoryEnabled(false) }
                                }
                            }
                        )
                    }
                )
            }

            OptionsSection(header = "") {
                OptionsRow(
                    headline = stringResource(R.string.dashboard_menu_help),
                    supporting = stringResource(R.string.dashboard_setting_help_desc),
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
                    stringResource(R.string.dashboard_setting_ussd_action),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    stringResource(R.string.dashboard_setting_ussd_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                listOf(
                    "tap_confirm" to stringResource(R.string.dashboard_setting_tap_confirm),
                    "long_confirm" to stringResource(R.string.dashboard_setting_long_confirm),
                    "long" to stringResource(R.string.dashboard_setting_long),
                    "tap" to stringResource(R.string.dashboard_setting_tap),
                    "double_tap" to stringResource(R.string.dashboard_setting_double_tap),
                    "triple_tap" to stringResource(R.string.dashboard_setting_triple_tap)
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

    if (showLoadingStyleSheet) {
        ModalBottomSheet(onDismissRequest = { showLoadingStyleSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.dashboard_setting_loading), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.dashboard_setting_loading_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                listOf(
                    "sweep" to stringResource(R.string.dashboard_setting_animated_border),
                    "bottom" to stringResource(R.string.dashboard_setting_center_line),
                    "perimeter" to stringResource(R.string.dashboard_setting_circular),
                    "skeleton" to stringResource(R.string.dashboard_setting_skeleton)
                ).forEach { (style, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch { settings.setDashboardLoadingStyle(style) }
                            showLoadingStyleSheet = false
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = loadingStyle == style, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

}
