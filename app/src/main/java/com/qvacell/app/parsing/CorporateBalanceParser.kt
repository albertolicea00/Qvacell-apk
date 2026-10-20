package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

/** Parses the *111# corporate snapshot. A blank Roaming label clears an older value. */
class CorporateBalanceParser : UssdResponseParser {
    override val ussdCodeId = "corp-balance"

    private val voice = Regex("""\bVoz\s*[.:]?\s*([\d.,]+)\s*MIN\b""", RegexOption.IGNORE_CASE)
    private val sms = Regex("""\bSMS\s*[.:]?\s*(\d+)\b""", RegexOption.IGNORE_CASE)
    private val data = Regex("""\bDatos\s*[.:]?\s*([\d.,]+)\s*(MB|GB)\b""", RegexOption.IGNORE_CASE)
    private val consumed = Regex("""\bSaldo\s*[.:]?\s*consumido\s*[.:]?\s*([\d.,]+)\s*CUP\b""", RegexOption.IGNORE_CASE)
    private val roaming = Regex("""\bRoaming\s*[.:]?\s*([^\r\n]*)""", RegexOption.IGNORE_CASE)

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        val response = UssdTextNormalizer.normalize(rawResponseText)
        val voiceMatch = voice.find(response)
        val smsMatch = sms.find(response)
        val dataMatch = data.find(response)
        if (voiceMatch == null && smsMatch == null && dataMatch == null) {
            return ParseResult.Unrecognized(rawResponseText)
        }

        val values = mutableListOf<ParsedDashboardValue>()
        voiceMatch?.let { match ->
            val minutes = NationalRechargeLimitParser.parseAmount(match.groupValues[1])
            values += ParsedDashboardValue(FieldTypes.VOICE_MINUTES_REMAINING, minutes, textValue = "${match.groupValues[1]} MIN", unit = "MIN")
        }
        smsMatch?.let { match ->
            values += ParsedDashboardValue(FieldTypes.SMS_COUNT_REMAINING, match.groupValues[1].toDouble(), unit = "SMS")
        }
        dataMatch?.let { match ->
            val amount = NationalRechargeLimitParser.parseAmount(match.groupValues[1])
            val mb = if (match.groupValues[2].equals("GB", ignoreCase = true)) amount * 1024 else amount
            values += ParsedDashboardValue(FieldTypes.DATA_DATOS_MB, mb, unit = "MB")
        }

        fun consumedBetween(start: Int, end: Int): Double? {
            if (start < 0 || start >= end) return null
            return consumed.find(response.substring(start, end))
                ?.let { NationalRechargeLimitParser.parseAmount(it.groupValues[1]) }
        }
        consumedBetween(voiceMatch?.range?.last?.plus(1) ?: -1, smsMatch?.range?.first ?: rawResponseText.length)?.let {
            values += ParsedDashboardValue(FieldTypes.CORPORATE_VOICE_CONSUMED_CUP, it, unit = "CUP")
        }
        consumedBetween(smsMatch?.range?.last?.plus(1) ?: -1, dataMatch?.range?.first ?: rawResponseText.length)?.let {
            values += ParsedDashboardValue(FieldTypes.CORPORATE_SMS_CONSUMED_CUP, it, unit = "CUP")
        }
        roaming.find(response)?.groupValues?.get(1)?.trim()?.let {
            values += ParsedDashboardValue(FieldTypes.CORPORATE_ROAMING, textValue = it)
        }
        return ParseResult.Success(values)
    }
}
