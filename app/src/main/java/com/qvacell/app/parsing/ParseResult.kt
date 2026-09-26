package com.qvacell.app.parsing

/** Outcome of parsing raw USSD-response or SMS-body text into structured dashboard values. */
sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>

    /** Placeholder state: parser rules for this signal haven't been written yet. */
    data class Unresolved(val reason: String = "parser rules not yet implemented") : ParseResult<Nothing>

    /** Real parser, but this specific text didn't match anything it knows how to read. */
    data class Unrecognized(val rawText: String) : ParseResult<Nothing>
}

data class ParsedDashboardValue(
    val fieldType: String,
    val numericValue: Double? = null,
    val textValue: String? = null,
    val dateValue: Long? = null,
    val unit: String? = null
)
