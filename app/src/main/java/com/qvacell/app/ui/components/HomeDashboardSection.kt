package com.qvacell.app.ui.components

import java.time.LocalDate

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/** "20 Ago 2027" — matches the date style used across dashboard cards. */
internal fun formatSpanishDate(date: LocalDate): String =
    "${date.dayOfMonth} ${SPANISH_MONTH_ABBREVIATIONS[date.monthValue - 1]} ${date.year}"
