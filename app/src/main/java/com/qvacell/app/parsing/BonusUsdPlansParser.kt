package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class BonusUsdPlansParser : UssdResponseParser {
    override val ussdCodeId: String = "bonus-usd-plans"

    private val noBonuses = Regex(
        """Usted no dispone de bonos activos""", RegexOption.IGNORE_CASE
    )

    private val datosCu = Regex(
        """Datos\s*\.\s*cu\s*[.:]?\s*(\d+)\s*(MB|GB)\s*vence\s*[.:]?\s*(\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE
    )

    private val datosNocturno = Regex(
        """Datos\s*[.:]?\s*ilimitados\s*[.:]?\s*Nocturno\s*[.:]?\s*vence\s*[.:]?\s*(\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE
    )

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        val response = UssdTextNormalizer.normalize(rawResponseText)
        if (noBonuses.containsMatchIn(response)) {
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.BONUS_ACTIVE,
                        numericValue = 0.0
                    )
                )
            )
        }

        val values = mutableListOf<ParsedDashboardValue>()

        datosCu.find(response)?.let { m ->
            val amount = m.groupValues[1].toDoubleOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val unit = m.groupValues[2]
            val dateEpoch = NationalRechargeLimitParser.parseDateDmy(m.groupValues[3])
                ?: return ParseResult.Unrecognized(rawResponseText)

            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.BONUS_DATOS_CU_MB,
                    numericValue = if (unit.equals("GB", ignoreCase = true)) amount * 1024 else amount,
                    unit = "MB"
                )
            )
            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.BONUS_DATOS_CU_EXPIRY,
                    dateValue = dateEpoch
                )
            )
        }

        datosNocturno.find(response)?.let { m ->
            val dateEpoch = NationalRechargeLimitParser.parseDateDmy(m.groupValues[1])
                ?: return ParseResult.Unrecognized(rawResponseText)

            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY,
                    dateValue = dateEpoch
                )
            )
        }

        if (values.isEmpty()) return ParseResult.Unrecognized(rawResponseText)

        values.add(0, ParsedDashboardValue(fieldType = FieldTypes.BONUS_ACTIVE, numericValue = 1.0))
        return ParseResult.Success(values)
    }
}
