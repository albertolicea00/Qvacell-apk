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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var pendingAction by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    fun requestQuery(codeId: String) {
        val code = catalogRepository.findCodeById(codeId) ?: return
        pendingAction = Triple(code.title.value, code.details.value) {
            DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
        }
    }

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
            availableFrom = "24-10-2026",
            onQuery = { requestQuery("national-recharge-limit") }
        )
        MainBalanceCard(
            balance = "1520.21",
            currency = "",
            lineActiveUntil = "20 Ago 2027",
            accountDueDate = "16 Feb 2028",
            onQuery = { requestQuery("main-balance") }
        )
        VoiceSmsRow(
            voiceDaysRemaining = "35d",
            voiceDuration = "4d 23h 55m",
            smsDaysRemaining = "35d",
            smsCount = "8,419",
            onVoiceQuery = { requestQuery("voice-balance") },
            onSmsQuery = { requestQuery("sms-balance") }
        )
        DataUsageCard(
            daysRemaining = "35 días restantes",
            packageGb = "6.00",
            tariffStatus = "No Activa",
            onQuery = { requestQuery("data-plan") }
        )
        NationalBonusCard(
            amount = "300 MB",
            expiry = "Vence 30 días",
            onQuery = { requestQuery("bonus-usd-plans") }
        )
        ConsultCardsRow(
            // TODO: postpaid-balance (*111#) — parser not implemented, needs real ETECSA response samples
            onPostpagoQuery = { requestQuery("postpaid-balance") },
            // TODO: tfa (*118#) — parser not implemented, needs real ETECSA response samples
            onTfaQuery = { requestQuery("tfa") },
            // TODO: friends-plan (*222*264#) — parser not implemented, needs real ETECSA response samples
            onPlanAmigoQuery = { requestQuery("friends-plan") }
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

    pendingAction?.let { (title, details, action) ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(title) },
            text = { Text(details) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        action()
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
