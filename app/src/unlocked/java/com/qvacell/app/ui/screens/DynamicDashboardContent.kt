package com.qvacell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.R
import com.qvacell.app.data.FieldTypes
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.DashboardRefreshService
import com.qvacell.app.service.SimUtils
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
import com.qvacell.app.ui.components.CorporateBalanceCard
import com.qvacell.app.ui.components.DataUsageCard
import com.qvacell.app.ui.components.MainBalanceCard
import com.qvacell.app.ui.components.NationalBonusCard
import com.qvacell.app.ui.components.RechargeLimitCard
import com.qvacell.app.ui.components.VoiceSmsRow
import com.qvacell.app.ui.components.LoadingCardBorder
import com.qvacell.app.ui.components.LocalLoadingAnimationStyle
import com.qvacell.app.ui.components.LocalDashboardCardTapMode
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDashboardContent(
    repository: CatalogRepository,
    dashboardRepository: DashboardDataRepository,
    ussdCaptureEnabled: Boolean,
    cardTapMode: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val catalogRepository = remember { repository }
    val settings = remember { SettingsDataStore(context) }
    val simPreferences by settings.simSelectorPreferences.collectAsStateWithLifecycle(initialValue = null)
    if (simPreferences == null) {
        DashboardSwitchLoadingSkeleton(isCorporate = null)
        return
    }
    val line = remember(simPreferences) {
        SimUtils.resolveDashboardLine(context, simPreferences!!.selectedSlot, simPreferences!!.appearances)
    }
    val isRefreshing by DashboardRefreshService.isRefreshing.collectAsState()
    val currentCodeId by DashboardRefreshService.currentCodeId.collectAsState()
    // Reset the collector state when the selected subscription changes. collectAsState keeps its
    // last value while switching flows, so without this key the previous SIM can flash briefly.
    val values = key(line.subscriptionId, line.needsSelection) {
        val valuesFlow = remember(dashboardRepository, line.subscriptionId) {
            if (line.subscriptionId == null) {
                kotlinx.coroutines.flow.flowOf<Map<String, DashboardValueSnapshot>>(emptyMap())
            } else {
                dashboardRepository.observeCurrentValues(line.subscriptionId)
            }
        }
        val valuesState = valuesFlow.collectAsState<
            Map<String, DashboardValueSnapshot>,
            Map<String, DashboardValueSnapshot>?
        >(initial = null)
        valuesState.value?.let { current ->
            if (line.needsSelection || line.subscriptionId == null) emptyMap()
            else current.filterValues { it.sourceSignalId.startsWith("sim:${line.subscriptionId}:") }
        }
    }

    if (values == null) {
        DashboardSwitchLoadingSkeleton(isCorporate = line.isCorporate)
        return
    }

    var pendingAction by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    val staleDays by settings.staleDaysThreshold.collectAsStateWithLifecycle(initialValue = 14)
    val loadingStyle by settings.dashboardLoadingStyle.collectAsStateWithLifecycle(initialValue = "sweep")

    var hasSignal by remember { mutableStateOf(hasCellularService(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            hasSignal = hasCellularService(context)
            delay(5000)
        }
    }

    fun requestQuery(codeId: String) {
        if (!hasSignal || line.needsSelection) return
        val code = catalogRepository.findCodeById(codeId) ?: return
        val needsConfirm = cardTapMode == "tap_confirm" || cardTapMode == "long_confirm"
        if (needsConfirm) {
            pendingAction = Triple(code.title.value, code.details.value) {
                DashboardRefreshService.refreshCode(context, code, dashboardRepository)
            }
        } else {
            DashboardRefreshService.refreshCode(context, code, dashboardRepository)
        }
    }

    val pullState = rememberPullToRefreshState()

    CompositionLocalProvider(
        LocalStaleDaysThreshold provides staleDays,
        LocalLoadingAnimationStyle provides loadingStyle,
        LocalDashboardCardTapMode provides cardTapMode
    ) {
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
            if (line.needsSelection) {
                Text(stringResource(R.string.dashboard_choose_sim), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!line.isCorporate) {
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
            }
            LoadingCardBorder(isLoading = currentCodeId == line.balanceCodeId) {
                if (line.isCorporate) {
                    CorporateBalanceCard(
                        voice = values[FieldTypes.VOICE_MINUTES_REMAINING]?.textValue ?: "--",
                        voiceConsumed = formatCorporateCup(values[FieldTypes.CORPORATE_VOICE_CONSUMED_CUP]?.numericValue),
                        sms = values[FieldTypes.SMS_COUNT_REMAINING]?.numericValue?.let { formatAmount(it) } ?: "--",
                        smsConsumed = formatCorporateCup(values[FieldTypes.CORPORATE_SMS_CONSUMED_CUP]?.numericValue),
                        data = values[FieldTypes.DATA_DATOS_MB]?.numericValue?.let { "${formatAmount(it)} MB" } ?: "--",
                        roaming = values[FieldTypes.CORPORATE_ROAMING]?.textValue?.takeIf { it.isNotBlank() } ?: "--",
                        lastUpdatedAt = values[FieldTypes.CORPORATE_VOICE_CONSUMED_CUP]?.capturedAt
                            ?: listOfNotNull(
                            values[FieldTypes.VOICE_MINUTES_REMAINING]?.capturedAt,
                            values[FieldTypes.SMS_COUNT_REMAINING]?.capturedAt,
                            values[FieldTypes.DATA_DATOS_MB]?.capturedAt
                        ).maxOrNull(),
                        onQuery = { requestQuery(line.balanceCodeId) }
                    )
                } else {
                    MainBalanceCard(
                        balance = values[FieldTypes.MAIN_BALANCE]?.let { formatAmount(it.numericValue) } ?: "--",
                        currency = values[FieldTypes.MAIN_BALANCE]?.unit ?: "",
                        lineActiveUntil = values[FieldTypes.LINE_ACTIVE_UNTIL]?.dateValue
                            ?.let { formatDate(it) } ?: "--",
                        accountDueDate = values[FieldTypes.ACCOUNT_DUE_DATE]?.dateValue
                            ?.let { formatDate(it) } ?: "--",
                        hasFetched = values[FieldTypes.MAIN_BALANCE]?.capturedAt != null,
                        lastUpdatedAt = values[FieldTypes.MAIN_BALANCE]?.capturedAt,
                        onQuery = { requestQuery(line.balanceCodeId) }
                    )
                }
            }
            if (line.isCorporate) {
                Button(
                    onClick = { uriHandler.openUri(CORPORATE_PORTAL_URL) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.dashboard_open_corporate_portal))
                }
            }
            if (!line.isCorporate) {
                val mainBalanceFetched = values[FieldTypes.MAIN_BALANCE]?.capturedAt != null
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
                    voiceIsLoading = currentCodeId == "voice-balance",
                    smsIsLoading = currentCodeId == "sms-balance",
                    onVoiceQuery = { requestQuery("voice-balance") },
                    onSmsQuery = { requestQuery("sms-balance") }
                )
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
                    planAmigoIsLoading = currentCodeId == "friends-plan",
                    // TODO: friends-plan (*222*264#) — parser not implemented, needs real ETECSA response samples
                    onPlanAmigoQuery = { requestQuery("friends-plan") }
                )
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

private enum class DashboardSkeletonCard { Balance, UsageRow, Data, Bonus, Friends }

@Composable
private fun DashboardSwitchLoadingSkeleton(isCorporate: Boolean?) {
    val cards = when (isCorporate) {
        true -> listOf(DashboardSkeletonCard.Balance)
        false -> listOf(
            DashboardSkeletonCard.Balance,
            DashboardSkeletonCard.UsageRow,
            DashboardSkeletonCard.Data,
            DashboardSkeletonCard.Bonus,
            DashboardSkeletonCard.Friends
        )
        null -> listOf(DashboardSkeletonCard.Balance, DashboardSkeletonCard.UsageRow, DashboardSkeletonCard.Data)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.forEach { card ->
            when (card) {
                DashboardSkeletonCard.Balance -> BalanceSkeletonCard(isCorporate == true)
                DashboardSkeletonCard.UsageRow -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UsageSkeletonCard(Modifier.weight(1f))
                    UsageSkeletonCard(Modifier.weight(1f))
                }
                DashboardSkeletonCard.Data -> DataSkeletonCard()
                DashboardSkeletonCard.Bonus -> BonusSkeletonCard()
                DashboardSkeletonCard.Friends -> SkeletonCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonShape(Modifier.size(14.dp), CircleShape)
                            Spacer(Modifier.width(6.dp))
                            SkeletonShape(Modifier.width(104.dp).height(12.dp))
                        }
                        SkeletonShape(Modifier.width(62.dp).height(18.dp), RoundedCornerShape(50))
                    }
                }
            }
        }
        if (isCorporate == true) SkeletonShape(Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(50))
    }
}

@Composable
private fun BalanceSkeletonCard(isCorporate: Boolean) {
    SkeletonCard {
        if (isCorporate) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonShape(Modifier.size(18.dp), CircleShape)
                Spacer(Modifier.width(8.dp))
                SkeletonShape(Modifier.width(142.dp).height(14.dp))
            }
            SkeletonShape(Modifier.width(76.dp).height(9.dp))
            repeat(3) { index ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SkeletonShape(Modifier.width(74.dp).height(11.dp))
                    SkeletonShape(Modifier.width(58.dp).height(25.dp))
                }
                if (index < 2) {
                    Row(modifier = Modifier.padding(start = 22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonShape(Modifier.width(78.dp).height(8.dp))
                        SkeletonShape(Modifier.width(38.dp).height(9.dp))
                    }
                    SkeletonShape(Modifier.fillMaxWidth().height(1.dp), RoundedCornerShape(1.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonShape(Modifier.size(13.dp), CircleShape)
                Spacer(Modifier.width(7.dp))
                SkeletonShape(Modifier.width(54.dp).height(10.dp))
                Spacer(Modifier.width(8.dp))
                SkeletonShape(Modifier.width(32.dp).height(13.dp))
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonShape(Modifier.width(106.dp).height(13.dp))
                    SkeletonShape(Modifier.width(122.dp).height(9.dp))
                    SkeletonShape(Modifier.width(82.dp).height(9.dp))
                    SkeletonShape(Modifier.width(68.dp).height(8.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonShape(Modifier.width(88.dp).height(38.dp))
                    SkeletonShape(Modifier.width(34.dp).height(9.dp))
                }
            }
        }
    }
}

@Composable
private fun UsageSkeletonCard(modifier: Modifier) {
    SkeletonCard(modifier) {
        SkeletonShape(Modifier.width(64.dp).height(12.dp))
        SkeletonShape(Modifier.width(92.dp).height(28.dp))
        SkeletonShape(Modifier.width(70.dp).height(9.dp))
        SkeletonShape(Modifier.width(52.dp).height(8.dp))
    }
}

@Composable
private fun DataSkeletonCard() {
    SkeletonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonShape(Modifier.size(14.dp), CircleShape)
            Spacer(Modifier.width(6.dp))
            SkeletonShape(Modifier.width(58.dp).height(12.dp))
        }
        SkeletonShape(Modifier.fillMaxWidth(0.64f).height(9.dp))
        repeat(3) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SkeletonShape(Modifier.width(76.dp).height(9.dp))
                    SkeletonShape(Modifier.width(98.dp).height(8.dp))
                }
                SkeletonShape(Modifier.width(52.dp).height(22.dp))
            }
        }
    }
}

@Composable
private fun BonusSkeletonCard() {
    SkeletonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonShape(Modifier.size(14.dp), CircleShape)
            Spacer(Modifier.width(6.dp))
            SkeletonShape(Modifier.width(92.dp).height(12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonShape(Modifier.width(74.dp).height(9.dp))
                SkeletonShape(Modifier.width(96.dp).height(8.dp))
            }
            SkeletonShape(Modifier.width(66.dp).height(32.dp))
        }
        SkeletonShape(Modifier.width(126.dp).height(10.dp))
    }
}

@Composable
private fun SkeletonCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SkeletonShape(modifier: Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp)) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)))
}

private const val CORPORATE_PORTAL_URL = "https://www.portalcorporativo.etecsa.cu/"

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

private fun formatCorporateCup(value: Double?): String =
    value?.let { "${DecimalFormat("#,##0.00").format(it)} CUP" } ?: "--"
