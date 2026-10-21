package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class SmsBalanceParser : UssdResponseParser {
    override val ussdCodeId: String = "sms-balance"

    private val hasSms = Regex(
        """Usted\s+dispone\s+de\s+(\d+)\s*SMS\s*[.:]?\s*validos\s+por\s+(\d+)\s*dias""", RegexOption.IGNORE_CASE
    )

    private val noPlan = Regex(
        """Usted debe adquirir un plan de SMS""", RegexOption.IGNORE_CASE
    )

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        val response = UssdTextNormalizer.normalize(rawResponseText)
        hasSms.find(response)?.let { m ->
            val count = m.groupValues[1].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val days = m.groupValues[2].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)

            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.SMS_PLAN_ACTIVE,
                        numericValue = 1.0
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.SMS_COUNT_REMAINING,
                        numericValue = count.toDouble(),
                        unit = "SMS"
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.SMS_DAYS_REMAINING,
                        numericValue = days.toDouble()
                    )
                )
            )
        }

        noPlan.find(response)?.let {
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.SMS_PLAN_ACTIVE,
                        numericValue = 0.0
                    )
                )
            )
        }

        return ParseResult.Unrecognized(rawResponseText)
    }
}
