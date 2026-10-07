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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.ui.components.ConsultCardsRow
import com.qvacell.app.ui.components.DataUsageCard
import com.qvacell.app.ui.components.MainBalanceCard
import com.qvacell.app.ui.components.NationalBonusCard
import com.qvacell.app.ui.components.RechargeLimitCard
import com.qvacell.app.ui.components.VoiceSmsRow

@Composable
fun DynamicDashboardContent(
    repository: CatalogRepository,
    dashboardRepository: DashboardDataRepository,
    ussdCaptureEnabled: Boolean,
    onTransfer: () -> Unit,
    onRecharge: () -> Unit
) {
    val context = LocalContext.current
    val catalogRepository = remember { repository }

    // Placeholder figures — wire to real computed values once DashboardCapture pipeline is ready.
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                catalogRepository.findCodeById("friends-plan")?.let { code ->
                    DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
                }
            },
            onPrepagoQuery = {
                catalogRepository.findCodeById("postpaid-balance")?.let { code ->
                    DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTransfer,
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
                onClick = onRecharge,
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
