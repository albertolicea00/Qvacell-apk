package com.qvacell.app.parsing

/** Parses one USSD code's raw response text into structured dashboard values. One response
 *  commonly carries several values at once (e.g. *222# returns balance + line-active-until +
 *  due date together), hence a list rather than a single field. */
interface UssdResponseParser {
    val ussdCodeId: String
    fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>>
}

/**
 * STUB — always [ParseResult.Unresolved]. Placeholder awaiting real ETECSA USSD response text
 * samples from the user for [ussdCodeId]. When those arrive, replace this class's registry entry
 * in [UssdParsers] with a real implementation — do not change any caller.
 */
class StubUssdResponseParser(override val ussdCodeId: String) : UssdResponseParser {
    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> =
        ParseResult.Unresolved()
}

object UssdParsers {
    /** The six dashboard-relevant codes.json ids — keep in sync with app/src/main/assets/codes.json. */
    val KNOWN_CODE_IDS = listOf(
        "main-balance",
        "bonus-usd-plans",
        "data-plan",
        "voice-balance",
        "sms-balance",
        "national-recharge-limit"
    )

    private val registry: Map<String, UssdResponseParser> =
        KNOWN_CODE_IDS.associateWith { StubUssdResponseParser(it) }

    fun forCode(codeId: String): UssdResponseParser = registry[codeId] ?: StubUssdResponseParser(codeId)
}
