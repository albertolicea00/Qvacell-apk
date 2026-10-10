package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class VoiceBalanceParser : UssdResponseParser {
    override val ussdCodeId: String = "voice-balance"

    private val hasMinutes = Regex(
        """Usted dispone de (\d{1,3}):(\d{2}):(\d{2}) MIN NAC validos por (\d+) dias""", RegexOption.IGNORE_CASE
    )

    private val noPlan = Regex(
        """Usted debe adquirir un plan de minutos""", RegexOption.IGNORE_CASE
    )

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        hasMinutes.find(rawResponseText)?.let { m ->
            val hours = m.groupValues[1].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val minutes = m.groupValues[2].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val seconds = m.groupValues[3].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val days = m.groupValues[4].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)

            val totalMinutes = hours * 60.0 + minutes + seconds / 60.0

            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.VOICE_PLAN_ACTIVE,
                        numericValue = 1.0
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.VOICE_MINUTES_REMAINING,
                        numericValue = totalMinutes,
                        textValue = "${m.groupValues[1]}:${m.groupValues[2]}:${m.groupValues[3]}",
                        unit = "MIN NAC"
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.VOICE_DAYS_REMAINING,
                        numericValue = days.toDouble()
                    )
                )
            )
        }

        noPlan.find(rawResponseText)?.let {
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.VOICE_PLAN_ACTIVE,
                        numericValue = 0.0
                    )
                )
            )
        }

        return ParseResult.Unrecognized(rawResponseText)
    }
}
