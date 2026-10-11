package com.qvacell.app.ui.components

import java.time.LocalDate

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/** "20 Ago 2027" — matches the date style used across dashboard cards. */
internal fun formatSpanishDate(date: LocalDate): String =
    "${date.dayOfMonth} ${SPANISH_MONTH_ABBREVIATIONS[date.monthValue - 1]} ${date.year}"

internal fun formatTimeAgo(epochMillis: Long?): String {
    if (epochMillis == null) return "Sin datos"
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Actualizado justo ahora"
        minutes < 60 -> "Actualizado hace ${minutes}min"
        hours < 24 -> "Actualizado hace ${hours}h"
        days == 1L -> "Actualizado hace 1 día"
        else -> "Actualizado hace $days días"
    }
}
