package com.qvacell.app.service

/**
 * STUB — placeholder heuristic only, unverified against real ETECSA traffic. Replace once the
 * user supplies real ETECSA sender-id/short-code and message-body samples. Isolated in its own
 * file so it's a single, obviously-swappable point — do not inline this logic elsewhere.
 */
object EtecsaSmsFilter {
    fun isEtecsaMessage(sender: String, body: String): Boolean {
        val looksLikeShortCode = sender.matches(Regex("^\\d{3,6}$"))
        val mentionsEtecsaTerms = body.contains("saldo", ignoreCase = true) || body.contains("ETECSA", ignoreCase = true)
        return looksLikeShortCode && mentionsEtecsaTerms
    }
}
