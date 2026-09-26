package com.qvacell.app.parsing

/** Classifies and parses an unsolicited SMS body, since (unlike a USSD response) it isn't tied
 *  to a code the app just dialed. */
interface SmsBodyParser {
    val smsSignalId: String
    fun matches(rawSmsBody: String, sender: String): Boolean
    fun parse(rawSmsBody: String): ParseResult<List<ParsedDashboardValue>>
}

/**
 * STUB — [matches] always false (contributes zero SMS signal rather than guessing), [parse]
 * always [ParseResult.Unresolved]. Placeholder awaiting real ETECSA SMS sender/body samples for
 * [smsSignalId]. Replace the registry entry in [SmsParsers] when those arrive.
 */
class StubSmsBodyParser(override val smsSignalId: String) : SmsBodyParser {
    override fun matches(rawSmsBody: String, sender: String): Boolean = false
    override fun parse(rawSmsBody: String): ParseResult<List<ParsedDashboardValue>> = ParseResult.Unresolved()
}

object SmsParsers {
    val KNOWN_SIGNAL_IDS = listOf(
        "etecsa-balance-alert",
        "etecsa-deduction-notice",
        "etecsa-limit-date"
    )

    private val registry: List<SmsBodyParser> = KNOWN_SIGNAL_IDS.map { StubSmsBodyParser(it) }

    fun all(): List<SmsBodyParser> = registry
}
