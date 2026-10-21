package com.qvacell.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.R
import com.qvacell.app.data.FieldTypes
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.SimAppearance
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.SimUtils
import com.qvacell.app.ui.components.BackNavigationIcon
import com.qvacell.app.ui.components.SimBadge as ConfiguredSimBadge
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private enum class EventKind(val icon: ImageVector, val labelRes: Int) {
    LINE_EXPIRY(Icons.Filled.CreditCard, R.string.events_line_expiry),
    RECHARGE_DEADLINE(Icons.Filled.CreditCard, R.string.events_recharge_deadline),
    VOICE_EXPIRY(Icons.Filled.PhoneInTalk, R.string.events_voice_expiry),
    SMS_EXPIRY(Icons.Filled.Message, R.string.events_sms_expiry),
    DATA_CU_EXPIRY(Icons.Filled.DataUsage, R.string.events_data_expiry),
    DATA_TODUS_EXPIRY(Icons.Filled.DataUsage, R.string.events_todus_expiry),
    DATA_DAILY_EXPIRY(Icons.Filled.DataUsage, R.string.events_daily_expiry),
    BONUS_CU_EXPIRY(Icons.Filled.DataUsage, R.string.events_bonus_expiry),
    BONUS_NIGHT_EXPIRY(Icons.Filled.DataUsage, R.string.events_night_bonus_expiry)
}

private data class DashboardEvent(
    val date: LocalDate,
    val kind: EventKind,
    val simLabel: String,
    val simColor: Color,
    val simSlotIndex: Int,
    val simAppearance: SimAppearance?
)

@Composable
fun DashboardEventsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val repository = remember { DashboardDataRepository(context) }
    val appearances by settings.simAppearances.collectAsStateWithLifecycle(initialValue = emptyMap())
    val sims = remember { SimUtils.getActiveSubscriptions(context) }
    val valueFlows = remember(sims) { sims.map { repository.observeCurrentValues(it.subscriptionId) } }
    val valuesBySim by remember(valueFlows) {
        if (valueFlows.isEmpty()) flowOf(emptyList<Map<String, DashboardValueSnapshot>>())
        else combine(valueFlows) { values -> values.toList() }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val locale = remember {
        context.resources.configuration.locales[0] ?: Locale.getDefault()
    }
    val events = remember(sims, valuesBySim, appearances, locale) {
        sims.flatMapIndexed { index, sim ->
            val appearance = appearances[sim.subscriptionId.toString()]
            val values = valuesBySim.getOrNull(index).orEmpty()
            buildEvents(
                values = values,
                simLabel = SimUtils.simLabel(sim, appearance),
                simColor = SimUtils.simColor(sim.simSlotIndex, appearance),
                simSlotIndex = sim.simSlotIndex,
                simAppearance = appearance,
            )
        }.sortedWith(compareBy<DashboardEvent> { it.date }.thenBy { it.kind.ordinal })
    }
    val today = LocalDate.now()
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var collapsedPixels by remember { mutableFloatStateOf(0f) }
    val firstWeekday = WeekFields.of(locale).firstDayOfWeek
    val leadingDays = (displayedMonth.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
    val weekCount = (leadingDays + displayedMonth.lengthOfMonth() + 6) / 7
    val maxCollapsePixels = with(LocalDensity.current) { (40.dp * (weekCount - 1)).toPx() }
    val collapseFraction = (collapsedPixels / maxCollapsePixels).coerceIn(0f, 1f)
    val calendarScrollConnection = remember(maxCollapsePixels) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val previous = collapsedPixels
                collapsedPixels = (previous - available.y).coerceIn(0f, maxCollapsePixels)
                return Offset(0f, previous - collapsedPixels)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val previous = collapsedPixels
                collapsedPixels = (previous - available.y).coerceIn(0f, maxCollapsePixels)
                return Offset(0f, previous - collapsedPixels)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 12.dp),
                title = { Text(stringResource(R.string.title_dashboard_events)) },
                navigationIcon = { BackNavigationIcon(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .nestedScroll(calendarScrollConnection)
        ) {
            CalendarCard(
                month = displayedMonth,
                events = events,
                locale = locale,
                collapseFraction = collapseFraction,
                onPrevious = {
                    displayedMonth = displayedMonth.minusMonths(1)
                    collapsedPixels = 0f
                },
                onNext = {
                    displayedMonth = displayedMonth.plusMonths(1)
                    collapsedPixels = 0f
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.events_all_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (events.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.events_no_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(events, key = { "${it.simSlotIndex}:${it.kind}:${it.date}" }) { event ->
                        EventRow(event, locale, today, showSimBadge = sims.size > 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    events: List<DashboardEvent>,
    locale: Locale,
    collapseFraction: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val first = month.atDay(1)
    val leading = ((first.dayOfWeek.value - firstDay.value) + 7) % 7
    val totalCells = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val weekCount = totalCells / 7
    // Keep today's week visible for the current month; other months show their first week.
    val focusDay = if (YearMonth.from(LocalDate.now()) == month) LocalDate.now().dayOfMonth else 1
    val focusWeek = (leading + focusDay - 1) / 7
    val rowHeightPixels = with(LocalDensity.current) { 40.dp.toPx() }
    val eventDates = events.map { it.date }.toSet()
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 24.dp * (1f - collapseFraction),
            bottomEnd = 24.dp * (1f - collapseFraction)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.events_previous_month)) }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.events_next_month)) }
            }
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    val day = firstDay.plus(offset.toLong())
                    Text(
                        day.getDisplayName(java.time.format.TextStyle.SHORT, locale).take(2),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp * (weekCount - (weekCount - 1) * collapseFraction))
                    .clipToBounds()
            ) {
                Column(
                    Modifier
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .graphicsLayer { translationY = -focusWeek * rowHeightPixels * collapseFraction }
                ) {
                    repeat(weekCount) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { column ->
                                val dayNumber = row * 7 + column - leading + 1
                                Box(Modifier.weight(1f).size(40.dp), contentAlignment = Alignment.Center) {
                                    if (dayNumber in 1..month.lengthOfMonth()) {
                                        val date = month.atDay(dayNumber)
                                        val hasEvent = date in eventDates
                                        val isPast = date.isBefore(LocalDate.now())
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (hasEvent) MaterialTheme.colorScheme.primary.copy(alpha = if (isPast) 0.4f else 1f)
                                                    else Color.Transparent
                                                )
                                        ) {
                                            Text(
                                                dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                    hasEvent -> MaterialTheme.colorScheme.onPrimary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: DashboardEvent, locale: Locale, today: LocalDate, showSimBadge: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(event.kind.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(event.kind.labelRes), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val date = event.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale))
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, event.date).toInt()
                Text(
                    "$date · ${relativeEventTime(days)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showSimBadge) SimBadge(event)
        }
    }
}

@Composable
private fun SimBadge(event: DashboardEvent) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(event.simColor.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConfiguredSimBadge(
            slotIndex = event.simSlotIndex,
            appearance = event.simAppearance,
            width = 16.dp,
            height = 20.dp
        )
        Text(
            text = event.simLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 5.dp),
            maxLines = 1
        )
    }
}

private fun buildEvents(
    values: Map<String, DashboardValueSnapshot>,
    simLabel: String,
    simColor: Color,
    simSlotIndex: Int,
    simAppearance: SimAppearance?
): List<DashboardEvent> {
    val zone = ZoneId.systemDefault()
    val events = mutableListOf<DashboardEvent>()
    fun add(kind: EventKind, date: LocalDate?) {
        if (date != null) events += DashboardEvent(date, kind, simLabel, simColor, simSlotIndex, simAppearance)
    }
    fun exactDate(field: String): LocalDate? = values[field]?.dateValue?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
    }
    fun daysDate(field: String): LocalDate? = values[field]?.let { snapshot ->
        val days = snapshot.numericValue?.toInt() ?: return@let null
        if (days < 0) return@let null
        Instant.ofEpochMilli(snapshot.capturedAt).atZone(zone).toLocalDate().plusDays(days.toLong())
    }
    add(EventKind.LINE_EXPIRY, exactDate(FieldTypes.LINE_ACTIVE_UNTIL))
    add(EventKind.RECHARGE_DEADLINE, exactDate(FieldTypes.ACCOUNT_DUE_DATE))
    if (values[FieldTypes.VOICE_PLAN_ACTIVE]?.numericValue == 1.0) add(EventKind.VOICE_EXPIRY, daysDate(FieldTypes.VOICE_DAYS_REMAINING))
    if (values[FieldTypes.SMS_PLAN_ACTIVE]?.numericValue == 1.0) add(EventKind.SMS_EXPIRY, daysDate(FieldTypes.SMS_DAYS_REMAINING))
    if (values[FieldTypes.DATA_DATOS_ACTIVE]?.numericValue == 1.0) add(EventKind.DATA_CU_EXPIRY, daysDate(FieldTypes.DATA_DATOS_DAYS))
    add(EventKind.DATA_TODUS_EXPIRY, daysDate(FieldTypes.DATA_TODUS_DAYS))
    if (values[FieldTypes.DATA_DIARIA_ACTIVE]?.numericValue == 1.0) {
        values[FieldTypes.DATA_DIARIA_ACTIVE]?.let { snapshot ->
            add(EventKind.DATA_DAILY_EXPIRY, Instant.ofEpochMilli(snapshot.capturedAt).atZone(zone).toLocalDate().plusDays(1))
        }
    }
    add(EventKind.BONUS_CU_EXPIRY, exactDate(FieldTypes.BONUS_DATOS_CU_EXPIRY))
    add(EventKind.BONUS_NIGHT_EXPIRY, exactDate(FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY))
    return events.distinctBy { it.kind to it.date }
}

@Composable
private fun relativeEventTime(days: Int): String = when {
    days == 0 -> stringResource(R.string.events_today)
    days == 1 -> stringResource(R.string.events_in_one_day)
    days in 2..29 -> stringResource(R.string.events_in_days, days)
    days >= 30 -> {
        val months = (days / 30).coerceAtLeast(1)
        if (months == 1) stringResource(R.string.events_in_one_month) else stringResource(R.string.events_in_months, months)
    }
    days == -1 -> stringResource(R.string.events_one_day_ago)
    days >= -29 -> stringResource(R.string.events_days_ago, -days)
    else -> {
        val months = (-days / 30).coerceAtLeast(1)
        if (months == 1) stringResource(R.string.events_one_month_ago) else stringResource(R.string.events_months_ago, months)
    }
}
