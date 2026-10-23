package com.qvacell.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.R
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.ui.components.rememberCubacelStatus
import com.qvacell.app.service.DashboardRefreshService
import com.qvacell.app.service.AppTelemetry
import com.qvacell.app.ui.components.RechargeBottomSheet
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.TransferBottomSheet
import com.qvacell.app.ui.components.hasCellularService
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun HomeQuickActionsScreen(
    onNavigateToReminders: () -> Unit = {},
    onNavigateToFriendsPlan: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onDynamicDashboardChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val settings = remember { SettingsDataStore(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val quickActionsStyle by settings.quickActionsStyle.collectAsStateWithLifecycle(initialValue = "filled")
    val cardTapMode by settings.cardTapMode.collectAsStateWithLifecycle(initialValue = "tap_confirm")
    val manualHiddenSections by settings.manualHiddenSections.collectAsStateWithLifecycle(initialValue = emptySet())

    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }
    var showHomeMenu by remember { mutableStateOf(false) }
    var showDynamicDashboard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showDynamicDashboard) {
        onDynamicDashboardChanged(showDynamicDashboard)
        AppTelemetry.screen(context, if (showDynamicDashboard) "dashboard_dynamic" else "dashboard_manual")
    }
    var showUnavailableDialog by remember { mutableStateOf(false) }

    val selectedSimSlot by settings.selectedSimSlot.collectAsStateWithLifecycle(initialValue = -1)
    val cubacelStatus by rememberCubacelStatus(selectedSimSlot)
    val isRefreshing by DashboardRefreshService.isRefreshing.collectAsState()

    var hasSignal by remember { mutableStateOf(hasCellularService(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            hasSignal = hasCellularService(context)
            delay(5000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshRotation"
    )

    Scaffold(topBar = {
        TopAppBar(
            modifier = Modifier.padding(top = 12.dp),
            title = { Text("Qvacell") },
            actions = {
                SimSelectorIcon()
                if (showDynamicDashboard) {
                    IconButton(
                        onClick = {
                            DashboardRefreshService.refresh(context, repository, dashboardRepository)
                        },
                        enabled = !isRefreshing && hasSignal && !cubacelStatus.blocksDashboardRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showHomeMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.action_menu),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showHomeMenu,
                        onDismissRequest = { showHomeMenu = false }
                    ) {
                        if ("transferir" in manualHiddenSections) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.quick_action_transfer)) },
                                onClick = {
                                    showHomeMenu = false
                                    showTransferSheet = true
                                }
                            )
                        }
                        if ("recargar" in manualHiddenSections) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.quick_action_recharge)) },
                                onClick = {
                                    showHomeMenu = false
                                    showRechargeSheet = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.options_friends_plan)) },
                            onClick = {
                                showHomeMenu = false
                                onNavigateToFriendsPlan()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reminders_title)) },
                            onClick = {
                                showHomeMenu = false
                                onNavigateToReminders()
                            }
                        )
                        if (BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_events)) },
                                onClick = {
                                    showHomeMenu = false
                                    onNavigateToEvents()
                                }
                            )
                        }
                        if (showDynamicDashboard && BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tab_options)) },
                                onClick = {
                                    showHomeMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.help_title)) },
                                onClick = {
                                    showHomeMenu = false
                                    onNavigateToHelp()
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        showHomeMenu = false
                        if (showDynamicDashboard) {
                            showDynamicDashboard = false
                        } else if (!BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                            showUnavailableDialog = true
                        } else {
                            showDynamicDashboard = true
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (showDynamicDashboard) Icons.Filled.GridView else Icons.Filled.InsertChart,
                        contentDescription = stringResource(
                            if (showDynamicDashboard) R.string.action_switch_to_manual
                            else R.string.action_switch_to_dynamic
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showDynamicDashboard) {
                DynamicDashboardContent(
                    repository = repository,
                    dashboardRepository = dashboardRepository,
                    ussdCaptureEnabled = ussdCaptureEnabled,
                    cardTapMode = cardTapMode
                )
            } else {
                ManualDashboardContent(
                    repository = repository,
                    dashboardRepository = dashboardRepository,
                    ussdCaptureEnabled = ussdCaptureEnabled,
                    tileStyle = quickActionsStyle,
                    hiddenSections = manualHiddenSections
                )
            }
        }
    }

    if (showTransferSheet) {
        TransferBottomSheet(onDismiss = { showTransferSheet = false })
    }
    if (showRechargeSheet) {
        RechargeBottomSheet(onDismiss = { showRechargeSheet = false })
    }
    if (showUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showUnavailableDialog = false },
            title = { Text(stringResource(R.string.dialog_dynamic_dashboard_unavailable_title)) },
            text = {
                Text(stringResource(R.string.dialog_dynamic_dashboard_unavailable_desc))
            },
            confirmButton = {
                TextButton(onClick = { showUnavailableDialog = false }) {
                    Text(stringResource(R.string.action_got_it))
                }
            }
        )
    }
}
