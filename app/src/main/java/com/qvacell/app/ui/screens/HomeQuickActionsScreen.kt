package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.ui.components.ConnectionBanner
import com.qvacell.app.ui.components.ConsultCardsRow
import com.qvacell.app.ui.components.DataUsageCard
import com.qvacell.app.ui.components.MainBalanceCard
import com.qvacell.app.ui.components.NationalBonusCard
import com.qvacell.app.ui.components.RechargeBottomSheet
import com.qvacell.app.ui.components.RechargeLimitCard
import com.qvacell.app.ui.components.TransferBottomSheet
import com.qvacell.app.ui.components.VoiceSmsRow

@Composable
fun HomeQuickActionsScreen() {
    val context = LocalContext.current
    val repository = remember { CatalogRepository(context) }
    val settings = remember { SettingsDataStore(context) }
    val dashboardRepository = remember { DashboardDataRepository(context) }
    val showNetworkStatus by settings.showNetworkStatus.collectAsStateWithLifecycle(initialValue = false)
    val ussdCaptureEnabled by settings.ussdCaptureEnabled.collectAsStateWithLifecycle(initialValue = false)
    var showTransferSheet by remember { mutableStateOf(false) }
    var showRechargeSheet by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Qvacell") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (showNetworkStatus) {
                ConnectionBanner()
            }
            // A fixed handful of cards, not an open-ended list — Column+verticalScroll sizes to the
            // actual content height. LazyColumn always stretches to fill the viewport, which left a
            // permanent blank gap above the bottom nav bar whenever the cards didn't fill the screen.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Placeholder figures — wire to real computed values once available.
                RechargeLimitCard(
                reached = true,
                limitAmount = "360 cup",
                availableFrom = "24-10-2026"
            )
            MainBalanceCard(
                balance = "1520.21",
                currency = "",
                lineActiveUntil = "20 Ago 2027",
                accountDueDate = "16 Feb 2028"
            )
            VoiceSmsRow(
                voiceDaysRemaining = "35d",
                voiceDuration = "4d 23h 55m",
                smsDaysRemaining = "35d",
                smsCount = "8,419"
            )
            DataUsageCard(
                daysRemaining = "35 días restantes",
                packageGb = "6.00",
                tariffStatus = "No Activa"
            )
            NationalBonusCard(
                amount = "300 MB",
                expiry = "Vence 30 días"
            )
            ConsultCardsRow(
                onPlanAmigoQuery = {
                    repository.findCodeById("friends-plan")?.let { code ->
                        DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
                    }
                },
                onPrepagoQuery = {
                    repository.findCodeById("postpaid-balance")?.let { code ->
                        DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showTransferSheet = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Transferir Saldo", modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = { showRechargeSheet = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Recargar Saldo", modifier = Modifier.padding(start = 8.dp))
                }
            }
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
