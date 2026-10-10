package com.qvacell.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DashboardRefreshService
import com.qvacell.app.ui.components.RechargeBottomSheet
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.TransferBottomSheet

@Composable
fun HomeQuickActionsScreen(
    onNavigateToEvents: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val settings = remember { SettingsDataStore(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val dashboardMode by settings.dashboardMode.collectAsStateWithLifecycle(initialValue = "")
    val quickActionsStyle by settings.quickActionsStyle.collectAsStateWithLifecycle(initialValue = "filled")
    val cardTapMode by settings.cardTapMode.collectAsStateWithLifecycle(initialValue = "tap_confirm")

    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }
    var showDashboardMenu by remember { mutableStateOf(false) }
    var viewOverride by remember { mutableStateOf<String?>(null) }
    var showUnavailableDialog by remember { mutableStateOf(false) }

    val currentView = viewOverride ?: dashboardMode
    val isRefreshing by DashboardRefreshService.isRefreshing.collectAsState()
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
                if (currentView == "dynamic") {
                    IconButton(
                        onClick = {
                            DashboardRefreshService.refresh(context, repository, dashboardRepository)
                        },
                        enabled = !isRefreshing,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Actualizar",
                            modifier = Modifier
                                .size(22.dp)
                                .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                        )
                    }
                }
                if (currentView == "dynamic") {
                    IconButton(
                        onClick = { showDashboardMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Menú",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showDashboardMenu,
                        onDismissRequest = { showDashboardMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Eventos") },
                            onClick = {
                                showDashboardMenu = false
                                onNavigateToEvents()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Opciones") },
                            onClick = {
                                showDashboardMenu = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ayuda") },
                            onClick = {
                                showDashboardMenu = false
                                onNavigateToHelp()
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            viewOverride = if (dashboardMode == "dynamic") "" else null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.GridView,
                            contentDescription = "Cambiar a manual",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (!BuildConfig.DASHBOARD_CAPTURE_ENABLED) {
                                showUnavailableDialog = true
                            } else {
                                viewOverride = if (dashboardMode != "dynamic") "dynamic" else null
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.InsertChart,
                            contentDescription = "Cambiar a dinámico",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (currentView) {
                "dynamic" -> DynamicDashboardContent(
                    repository = repository,
                    dashboardRepository = dashboardRepository,
                    ussdCaptureEnabled = ussdCaptureEnabled,
                    cardTapMode = cardTapMode,
                    onTransfer = { showTransferSheet = true },
                    onRecharge = { showRechargeSheet = true }
                )
                else -> ManualDashboardContent(
                    repository = repository,
                    dashboardRepository = dashboardRepository,
                    ussdCaptureEnabled = ussdCaptureEnabled,
                    tileStyle = quickActionsStyle
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
            title = { Text("Dashboard Dinámico no disponible") },
            text = {
                Text("El Dashboard Dinámico solo está disponible en la versión descargada desde GitHub.")
            },
            confirmButton = {
                TextButton(onClick = { showUnavailableDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
