package com.qvacell.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import com.qvacell.app.ui.components.LocalStaleDaysThreshold
import com.qvacell.app.ui.components.formatSpanishDate
import com.qvacell.app.ui.components.hasCellularService
import com.qvacell.app.data.SettingsDataStore
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.ui.components.ConsultCardsRow
import com.qvacell.app.ui.components.PostpaidConsultCard
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
    cardTapMode: String
) {
    val context = LocalContext.current
    val catalogRepository = remember { repository }
    val isRefreshing by DashboardRefreshService.isRefreshing.collectAsState()
    val currentCodeId by DashboardRefreshService.currentCodeId.collectAsState()
    val values by dashboardRepository.observeCurrentValues().collectAsState(initial = emptyMap())

    var pendingAction by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    val settings = remember { SettingsDataStore(context) }
    val staleDays by settings.staleDaysThreshold.collectAsStateWithLifecycle(initialValue = 14)

    var hasSignal by remember { mutableStateOf(hasCellularService(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            hasSignal = hasCellularService(context)
            delay(5000)
        }
    }

    fun requestQuery(codeId: String) {
        if (!hasSignal) return
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

    CompositionLocalProvider(LocalStaleDaysThreshold provides staleDays) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { if (hasSignal) DashboardRefreshService.refresh(context, catalogRepository, dashboardRepository) },
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        indicator = {}
    ) {
        val activePull = pullState.distanceFraction > 0f && !isRefreshing && hasSignal
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
            LoadingCardBorder(isLoading = currentCodeId == "national-recharge-limit") {
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
            }
            PostpaidConsultCard(onQuery = { requestQuery("postpaid-balance") })
            LoadingCardBorder(isLoading = currentCodeId == "main-balance") {
            MainBalanceCard(
                balance = values[FieldTypes.MAIN_BALANCE]?.let { formatAmount(it.numericValue) } ?: "--",
                currency = values[FieldTypes.MAIN_BALANCE]?.unit ?: "",
                lineActiveUntil = values[FieldTypes.LINE_ACTIVE_UNTIL]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                accountDueDate = values[FieldTypes.ACCOUNT_DUE_DATE]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                hasFetched = values[FieldTypes.MAIN_BALANCE]?.capturedAt != null,
                lastUpdatedAt = values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                onQuery = { requestQuery("main-balance") }
            )
            }
            val mainBalanceFetched = values[FieldTypes.MAIN_BALANCE]?.capturedAt != null
            LoadingCardBorder(isLoading = currentCodeId == "voice-balance" || currentCodeId == "sms-balance") {
            VoiceSmsRow(
                voiceDaysRemaining = values[FieldTypes.VOICE_DAYS_REMAINING]?.numericValue
                    ?.let { "${it.toInt()}d" } ?: "--",
                voiceDuration = values[FieldTypes.VOICE_MINUTES_REMAINING]?.textValue ?: "--",
                smsDaysRemaining = values[FieldTypes.SMS_DAYS_REMAINING]?.numericValue
                    ?.let { "${it.toInt()}d" } ?: "--",
                smsCount = values[FieldTypes.SMS_COUNT_REMAINING]?.numericValue
                    ?.let { DecimalFormat("#,###").format(it.toInt()) } ?: "--",
                voiceHasFetched = values[FieldTypes.VOICE_MINUTES_REMAINING]?.capturedAt != null || mainBalanceFetched,
                smsHasFetched = values[FieldTypes.SMS_COUNT_REMAINING]?.capturedAt != null || mainBalanceFetched,
                voiceLastUpdatedAt = values[FieldTypes.VOICE_MINUTES_REMAINING]?.capturedAt
                    ?: values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                smsLastUpdatedAt = values[FieldTypes.SMS_COUNT_REMAINING]?.capturedAt
                    ?: values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                onVoiceQuery = { requestQuery("voice-balance") },
                onSmsQuery = { requestQuery("sms-balance") }
            )
            }
            LoadingCardBorder(isLoading = currentCodeId == "data-plan") {
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
                hasFetched = values[FieldTypes.DATA_TARIFF_ACTIVE]?.capturedAt != null || mainBalanceFetched,
                lastUpdatedAt = values[FieldTypes.DATA_TARIFF_ACTIVE]?.capturedAt
                    ?: values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                onQuery = { requestQuery("data-plan") }
            )
            }
            LoadingCardBorder(isLoading = currentCodeId == "bonus-usd-plans") {
            NationalBonusCard(
                amount = values[FieldTypes.BONUS_DATOS_CU_MB]?.let {
                    formatAmount(it.numericValue) + " " + (it.unit ?: "MB")
                } ?: "--",
                expiry = values[FieldTypes.BONUS_DATOS_CU_EXPIRY]?.dateValue
                    ?.let { "Vence ${formatDate(it)}" } ?: "--",
                nocturnoStatus = if (values[FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY]?.dateValue != null) "Activo" else "No Disponible",
                nocturnoExpiry = values[FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY]?.dateValue
                    ?.let { formatDate(it) } ?: "--",
                hasFetched = (values[FieldTypes.BONUS_ACTIVE]?.capturedAt
                    ?: values[FieldTypes.BONUS_DATOS_CU_MB]?.capturedAt) != null,
                lastUpdatedAt = values[FieldTypes.BONUS_ACTIVE]?.capturedAt
                    ?: values[FieldTypes.BONUS_DATOS_CU_MB]?.capturedAt,
                onQuery = { requestQuery("bonus-usd-plans") }
            )
            }
            ConsultCardsRow(
                // TODO: tfa (*118#) — parser not implemented, needs real ETECSA response samples
                onTfaQuery = { requestQuery("tfa") },
                // TODO: friends-plan (*222*264#) — parser not implemented, needs real ETECSA response samples
                onPlanAmigoQuery = { requestQuery("friends-plan") }
            )
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

@Composable
private fun LoadingCardBorder(
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    if (!isLoading) {
        content()
        return
    }
    val transition = rememberInfiniteTransition(label = "loading")
    val sweepPosition by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeWidth = 3.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(strokeWidth / 2)
            .drawWithContent {
                drawContent()
                val sw = strokeWidth.toPx()
                val radius = 20.dp.toPx()
                val gradientWidth = size.width * 0.4f
                val start = sweepPosition * size.width - gradientWidth / 2
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor,
                        primaryColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    startX = start,
                    endX = start + gradientWidth
                )
                drawRoundRect(
                    brush = brush,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = sw)
                )
            }
    ) {
        content()
    }
}
