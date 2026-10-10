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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DashboardRefreshService
import com.qvacell.app.ui.components.RechargeBottomSheet
import com.qvacell.app.ui.components.SimSelectorIcon
import com.qvacell.app.ui.components.TransferBottomSheet

@Composable
fun HomeQuickActionsScreen() {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val settings = remember { SettingsDataStore(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    val dashboardMode by settings.dashboardMode.collectAsStateWithLifecycle(initialValue = "")
    val quickActionsStyle by settings.quickActionsStyle.collectAsStateWithLifecycle(initialValue = "filled")
    val cardTapMode by settings.cardTapMode.collectAsStateWithLifecycle(initialValue = "tap_confirm")

    // Bottom sheets only used by the dynamic dashboard path
    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }

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
                if (dashboardMode == "dynamic") {
                    IconButton(
                        onClick = {
                            DashboardRefreshService.refresh(context, repository, dashboardRepository)
                        },
                        enabled = !isRefreshing
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
                SimSelectorIcon()
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (dashboardMode) {
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
}
