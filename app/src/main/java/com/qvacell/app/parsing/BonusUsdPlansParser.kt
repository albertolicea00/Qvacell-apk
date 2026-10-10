package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class BonusUsdPlansParser : UssdResponseParser {
    override val ussdCodeId: String = "bonus-usd-plans"

    private val noBonuses = Regex(
        """Usted no dispone de bonos activos"""
    )

    private val datosCu = Regex(
        """Datos\.cu:?\s*(\d+)\s*(MB|GB)\s*vence\s*(\d{2}-\d{2}-\d{2,4})"""
    )

    private val datosNocturno = Regex(
        """Datos:?\s*ilimitados:?\s*Nocturno\s*vence\s*(\d{2}-\d{2}-\d{2,4})"""
    )

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        if (noBonuses.containsMatchIn(rawResponseText)) {
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

        datosCu.find(rawResponseText)?.let { m ->
            val amount = m.groupValues[1].toDoubleOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            val unit = m.groupValues[2]
            val dateEpoch = NationalRechargeLimitParser.parseDateDmy(m.groupValues[3])
                ?: return ParseResult.Unrecognized(rawResponseText)

            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.BONUS_DATOS_CU_MB,
                    numericValue = if (unit == "GB") amount * 1024 else amount,
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

        datosNocturno.find(rawResponseText)?.let { m ->
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
