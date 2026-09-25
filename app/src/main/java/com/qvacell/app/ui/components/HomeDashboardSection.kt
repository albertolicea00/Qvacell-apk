package com.qvacell.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Placeholder home dashboard — layout/structure ported from the ui-android-home-view.html
 * mockup, using this app's own Material3 color roles instead of the mockup's palette. All the
 * figures here (balance, dates, data/voice/SMS usage) are hardcoded placeholders; wire them to
 * real computed values once that data is available.
 */
@Composable
fun MainBalanceCard(
    balance: String,
    currency: String,
    lineActiveUntil: String,
    accountDueDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "SALDO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        "Activa hasta $lineActiveUntil",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        "Vence el $accountDueDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    // Placeholder days-ago figure — wire to the real last-sync timestamp once available.
                    val lastUpdateDaysAgo = 1
                    Text(
                        if (lastUpdateDaysAgo == 0) {
                            "Actualizado hoy"
                        } else if (lastUpdateDaysAgo == 1) {
                            "Actualizado hace 1 día"
                        } else {
                            "Actualizado hace $lastUpdateDaysAgo días"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    val (wholePart, centsPart) = balance.split(".", limit = 2)
                        .let { it[0] to it.getOrElse(1) { "" } }
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 36.sp)) {
                                append(wholePart)
                            }
                            if (centsPart.isNotEmpty()) {
                                withStyle(SpanStyle(fontSize = 20.sp)) {
                                    append(".$centsPart")
                                }
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        currency,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

/** "24-10-2026" (dd-MM-yyyy) — the format Ajustes/backend hand us for this date; ⊥ on parse failure. */
private fun parseDmyDate(value: String): LocalDate? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

/** Standalone card — shown above [MainBalanceCard] only while the monthly recharge limit is hit. */
@Composable
fun RechargeLimitCard(reached: Boolean, limitAmount: String, availableFrom: String) {
    if (!reached) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val parsedAvailableDate = parseDmyDate(availableFrom)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Límite Mensual ($limitAmount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (parsedAvailableDate != null) {
                    Text(
                        "Puede Recargar el ${formatSpanishDate(parsedAvailableDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Placeholder days-ago figure — wire to the real last-sync timestamp once available.
                val lastUpdateDaysAgo = 1
                Text(
                    if (lastUpdateDaysAgo == 0) {
                        "Actualizado hoy"
                    } else if (lastUpdateDaysAgo == 1) {
                        "Actualizado hace 1 día"
                    } else {
                        "Actualizado hace $lastUpdateDaysAgo días"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            val daysUntilAvailable = parsedAvailableDate?.let {
                ChronoUnit.DAYS.between(LocalDate.now(), it)
            }
            if (daysUntilAvailable != null) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        ) {
                            append("$daysUntilAvailable")
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("d")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DataUsageCard(
    daysRemaining: String,
    packageGb: String,
    tariffStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.NetworkCell,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "DATOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    val expiryDays = daysRemaining.filter { it.isDigit() }.toIntOrNull()
                    if (expiryDays != null) {
                        Text(
                            "Vence el ${formatSpanishDate(LocalDate.now().plusDays(expiryDays.toLong()))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // Placeholder days-ago figure — wire to the real last-sync timestamp once available.
                    val lastUpdateDaysAgo = 1
                    Text(
                        if (lastUpdateDaysAgo == 0) {
                            "Actualizado hoy"
                        } else if (lastUpdateDaysAgo == 1) {
                            "Actualizado hace 1 día"
                        } else {
                            "Actualizado hace $lastUpdateDaysAgo días"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        packageGb,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "GB",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    "Tarifa por consumo: ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    tariffStatus,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Standalone card — pulled out of [DataUsageCard] to sit between it and [VoiceSmsRow]. */
@Composable
fun NationalBonusCard(amount: String, expiry: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "BONOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    val expiryDays = expiry.filter { it.isDigit() }.toIntOrNull()
                    if (expiryDays != null) {
                        Text(
                            "Vence el ${formatSpanishDate(LocalDate.now().plusDays(expiryDays.toLong()))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // Placeholder days-ago figure — wire to the real last-sync timestamp once available.
                    val lastUpdateDaysAgo = 1
                    Text(
                        if (lastUpdateDaysAgo == 0) {
                            "Actualizado hoy"
                        } else if (lastUpdateDaysAgo == 1) {
                            "Actualizado hace 1 día"
                        } else {
                            "Actualizado hace $lastUpdateDaysAgo días"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    val (number, unit) = amount.split(" ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                    Text(
                        number,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            unit,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    "Datos Ilimitados (Nocturnos): ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    "No Disponible",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun VoiceSmsRow(
    voiceDaysRemaining: String,
    voiceDuration: String,
    smsDaysRemaining: String,
    smsCount: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UsageStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Mic,
            accentColor = MaterialTheme.colorScheme.primary,
            badge = voiceDaysRemaining,
            label = "VOZ",
            value = voiceDuration
        )
        UsageStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Sms,
            accentColor = MaterialTheme.colorScheme.primary,
            badge = smsDaysRemaining,
            label = "SMS",
            value = smsCount
        )
    }
}

@Composable
private fun UsageStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badge: String,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                val unitColor = MaterialTheme.colorScheme.onSurface
                Text(
                    buildAnnotatedString {
                        value.forEach { char ->
                            val style = if (char.isLetter()) {
                                SpanStyle(color = unitColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                SpanStyle(color = accentColor)
                            }
                            withStyle(style) { append(char) }
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val expiryDays = badge.filter { it.isDigit() }.toIntOrNull()
                if (expiryDays != null) {
                    Text(
                        "Vence el ${formatSpanishDate(LocalDate.now().plusDays(expiryDays.toLong()))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                // Placeholder days-ago figure — wire to the real last-sync timestamp once available.
                val lastUpdateDaysAgo = 1
                Text(
                    if (lastUpdateDaysAgo == 0) {
                        "Actualizado hoy"
                    } else if (lastUpdateDaysAgo == 1) {
                        "Actualizado hace 1 día"
                    } else {
                        "Actualizado hace $lastUpdateDaysAgo días"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/** "20 Ago 2027" — matches the date style already used in [MainBalanceCard]. */
private fun formatSpanishDate(date: LocalDate): String =
    "${date.dayOfMonth} ${SPANISH_MONTH_ABBREVIATIONS[date.monthValue - 1]} ${date.year}"

/** Two side-by-side placeholder cards — pulled out of the request to sit below [NationalBonusCard]. */
@Composable
fun ConsultCardsRow(onPlanAmigoClick: () -> Unit = {}, onSaldoPrepagadoClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ConsultCard(
            title = "PLAN AMIGO",
            icon = Icons.Filled.People,
            modifier = Modifier.weight(1f),
            onClick = onPlanAmigoClick
        )
        ConsultCard(
            title = "SALDO PREPAGO",
            icon = Icons.Filled.CreditCard,
            modifier = Modifier.weight(1f),
            onClick = onSaldoPrepagadoClick
        )
    }
}

@Composable
private fun ConsultCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    "Presiona para consultar",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    Icons.Filled.ArrowOutward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            }
            Text(
                "UI Próximamente",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
