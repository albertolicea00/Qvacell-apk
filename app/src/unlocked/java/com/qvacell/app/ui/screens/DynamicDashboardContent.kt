package com.qvacell.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.FieldTypes
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DashboardRefreshService
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import com.qvacell.app.ui.components.formatSpanishDate
import com.qvacell.app.ui.components.ConsultCardsRow
import com.qvacell.app.ui.components.DataUsageCard
import com.qvacell.app.ui.components.MainBalanceCard
import com.qvacell.app.ui.components.NationalBonusCard
import com.qvacell.app.ui.components.RechargeLimitCard
import com.qvacell.app.ui.components.VoiceSmsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDashboardContent(
    repository: CatalogRepository,
    dashboardRepository: DashboardDataRepository,
    ussdCaptureEnabled: Boolean,
    cardTapMode: String,
    actionsStyle: String = "buttons",
    onTransfer: () -> Unit,
    onRecharge: () -> Unit
) {
    val context = LocalContext.current
    val catalogRepository = remember { repository }
    val isRefreshing by DashboardRefreshService.isRefreshing.collectAsState()
    val values by dashboardRepository.observeCurrentValues().collectAsState(initial = emptyMap())

    var pendingAction by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    fun executeQuery(codeId: String) {
        val code = catalogRepository.findCodeById(codeId) ?: return
        DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
    }

    fun requestQuery(codeId: String) {
        val code = catalogRepository.findCodeById(codeId) ?: return
        val needsConfirm = cardTapMode == "tap_confirm" || cardTapMode == "long_confirm"
        if (needsConfirm) {
            pendingAction = Triple(code.title.value, code.details.value) {
                DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
            }
        } else {
            DashboardCapture.captureOrDial(context, code, dashboardRepository, ussdCaptureEnabled)
        }
    }

    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { DashboardRefreshService.refresh(context, catalogRepository, dashboardRepository) },
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        indicator = {}
    ) {
        val activePull = pullState.distanceFraction > 0f && !isRefreshing
        val pullOffset = if (activePull) (pullState.distanceFraction * 60).coerceAtMost(60f) else 0f

        Box(modifier = Modifier.fillMaxSize()) {
            if (activePull) {
                val alpha = pullState.distanceFraction.coerceIn(0.3f, 0.8f)
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (pullOffset * 0.05f).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp).offset(y = 4.dp)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textColor.copy(alpha = alpha * 0.6f),
                        modifier = Modifier.size(16.dp).offset(y = (-4).dp)
                    )
                    Text(
                        if (pullState.distanceFraction >= 1f) "Suelta para refrescar"
                        else "Arrastra para refrescar",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textColor
                    )
                }
            }

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = pullOffset.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .padding(top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RechargeLimitCard(
                reached = values[FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED]?.numericValue == 1.0,
                limitAmount = values[FieldTypes.NATIONAL_RECHARGE_LIMIT_REMAINING]?.let {
                    formatAmount(it.numericValue) + " " + (it.unit ?: "CUP")
                } ?: values[FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT]?.let {
                    formatAmount(it.numericValue) + " " + (it.unit ?: "CUP")
                } ?: "--",
                availableFrom = values[FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                lastUpdatedAt = values[FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED]?.capturedAt
                    ?: values[FieldTypes.NATIONAL_RECHARGE_LIMIT_REMAINING]?.capturedAt,
                onQuery = { requestQuery("national-recharge-limit") }
            )
            MainBalanceCard(
                balance = values[FieldTypes.MAIN_BALANCE]?.let { formatAmount(it.numericValue) } ?: "--",
                currency = values[FieldTypes.MAIN_BALANCE]?.unit ?: "",
                lineActiveUntil = values[FieldTypes.LINE_ACTIVE_UNTIL]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                accountDueDate = values[FieldTypes.ACCOUNT_DUE_DATE]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                lastUpdatedAt = values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                onQuery = { requestQuery("main-balance") }
            )
            VoiceSmsRow(
                voiceDaysRemaining = values[FieldTypes.VOICE_DAYS_REMAINING]?.numericValue
                    ?.let { "${it.toInt()}d" } ?: "--",
                voiceDuration = values[FieldTypes.VOICE_MINUTES_REMAINING]?.textValue ?: "--",
                smsDaysRemaining = values[FieldTypes.SMS_DAYS_REMAINING]?.numericValue
                    ?.let { "${it.toInt()}d" } ?: "--",
                smsCount = values[FieldTypes.SMS_COUNT_REMAINING]?.numericValue
                    ?.let { DecimalFormat("#,###").format(it.toInt()) } ?: "--",
                voiceLastUpdatedAt = values[FieldTypes.VOICE_MINUTES_REMAINING]?.capturedAt,
                smsLastUpdatedAt = values[FieldTypes.SMS_COUNT_REMAINING]?.capturedAt,
                onVoiceQuery = { requestQuery("voice-balance") },
                onSmsQuery = { requestQuery("sms-balance") }
            )
            DataUsageCard(
                daysRemaining = values[FieldTypes.DATA_DATOS_DAYS]?.numericValue
                    ?.let { "${it.toInt()} días restantes" } ?: "--",
                packageGb = values[FieldTypes.DATA_DATOS_MB]?.numericValue
                    ?.let { DecimalFormat("0.00").format(it / 1024.0) } ?: "--",
                tariffStatus = if (values[FieldTypes.DATA_TARIFF_ACTIVE]?.numericValue == 1.0) "Activa" else "No Activa",
                diariaMb = values[FieldTypes.DATA_DIARIA_MB]?.numericValue
                    ?.let { formatAmount(it) } ?: "--",
                diariaExpiry = if (values[FieldTypes.DATA_DIARIA_ACTIVE]?.numericValue == 1.0)
                    formatSpanishDate(LocalDate.now().plusDays(1)) else "--",
                datosExpiry = values[FieldTypes.DATA_DATOS_DAYS]?.numericValue
                    ?.let { formatSpanishDate(LocalDate.now().plusDays(it.toLong())) } ?: "--",
                todusMb = values[FieldTypes.DATA_TODUS_MB]?.numericValue
                    ?.let { formatAmount(it) } ?: "--",
                todusExpiry = values[FieldTypes.DATA_TODUS_DAYS]?.numericValue
                    ?.let { formatSpanishDate(LocalDate.now().plusDays(it.toLong())) } ?: "--",
                lastUpdatedAt = values[FieldTypes.DATA_TARIFF_ACTIVE]?.capturedAt,
                onQuery = { requestQuery("data-plan") }
            )
            NationalBonusCard(
                amount = values[FieldTypes.BONUS_DATOS_CU_MB]?.let {
                    formatAmount(it.numericValue) + " " + (it.unit ?: "MB")
                } ?: "--",
                expiry = values[FieldTypes.BONUS_DATOS_CU_EXPIRY]?.dateValue
                    ?.let { "Vence ${formatDate(it)}" } ?: "--",
                nocturnoStatus = if (values[FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY]?.dateValue != null) "Activo" else "No Disponible",
                nocturnoExpiry = values[FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                lastUpdatedAt = values[FieldTypes.BONUS_ACTIVE]?.capturedAt
                    ?: values[FieldTypes.BONUS_DATOS_CU_MB]?.capturedAt,
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
            when (actionsStyle) {
                "buttons" -> Row(
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
                "list" -> {
                    Column {
                        Text(
                            "Acciones",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onTransfer)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Transferir Saldo",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                )
                                Icon(
                                    Icons.Filled.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onRecharge)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Recargar Saldo",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                                )
                                Icon(
                                    Icons.Filled.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
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

private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.forLanguageTag("es"))

private fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

private fun formatAmount(value: Double?): String {
    if (value == null) return "--"
    return if (value == value.toLong().toDouble()) {
        DecimalFormat("#,###").format(value.toLong())
    } else {
        DecimalFormat("#,##0.00").format(value)
    }
}
