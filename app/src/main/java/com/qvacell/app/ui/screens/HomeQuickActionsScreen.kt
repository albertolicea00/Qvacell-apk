package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.ui.components.RechargeBottomSheet
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

    // Bottom sheets only used by the dynamic dashboard path
    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(modifier = Modifier.padding(top = 12.dp), title = { Text("Qvacell") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (dashboardMode) {
                "dynamic" -> DynamicDashboardContent(
                    repository = repository,
                    dashboardRepository = dashboardRepository,
                    ussdCaptureEnabled = ussdCaptureEnabled,
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
