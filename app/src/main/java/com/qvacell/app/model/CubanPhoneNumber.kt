package com.qvacell.app.model

/** Ported from the iOS app's CubanPhoneNumber validation. */
object CubanPhoneNumber {

    val validMobilePrefixes = listOf("5", "6")

    /**
     * Normalizes a raw phone number string into an 8-digit Cuban local number.
     * Accepts a bare 8-digit number, or a 10-digit number prefixed with the
     * country code "53". Returns null if the number is not a valid Cuban number.
     */
    fun normalize(rawNumber: String): String? {
        val digits = rawNumber.filter { it.isDigit() }

        val local = when {
            digits.length == 8 -> digits
            digits.length == 10 && digits.startsWith("53") -> digits.substring(2)
            else -> return null
        }

        if (local.length != 8) return null
        val firstDigit = local.first().toString()
        if (firstDigit !in validMobilePrefixes) return null

        return local
    }
}
