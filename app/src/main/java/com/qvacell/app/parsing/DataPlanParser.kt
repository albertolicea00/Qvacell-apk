package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class DataPlanParser : UssdResponseParser {
    override val ussdCodeId: String = "data-plan"

    private val tarifa = Regex("""Tarifa:\s*(Activa|No activa)""", RegexOption.IGNORE_CASE)

    private val datos = Regex(
        """Datos:\s*([\d,.]+)\s*(MB|GB)\s*(validos\s*(\d+)\s*dias|no activos)""", RegexOption.IGNORE_CASE
    )

    private val diaria = Regex(
        """Diaria:\s*([\d,.]+)\s*MB\s*(validos\s*24\s*horas|no activos)""", RegexOption.IGNORE_CASE
    )

    private val todus = Regex(
        """toDus:\s*([\d,.]+)\s*MB\s*validos\s*(\d+)\s*dias""", RegexOption.IGNORE_CASE
    )

    private val noPlan = Regex("""Ud debe adquirir una oferta""", RegexOption.IGNORE_CASE)

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        val tarifaMatch = tarifa.find(rawResponseText)
            ?: return ParseResult.Unrecognized(rawResponseText)

        val values = mutableListOf<ParsedDashboardValue>()

        val tariffActive = tarifaMatch.groupValues[1].equals("Activa", ignoreCase = true)
        values.add(
            ParsedDashboardValue(
                fieldType = FieldTypes.DATA_TARIFF_ACTIVE,
                numericValue = if (tariffActive) 1.0 else 0.0
            )
        )

        if (noPlan.containsMatchIn(rawResponseText)) {
            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.DATA_PLAN_ACTIVE,
                    numericValue = 0.0
                )
            )
            return ParseResult.Success(values)
        }

        values.add(
            ParsedDashboardValue(
                fieldType = FieldTypes.DATA_PLAN_ACTIVE,
                numericValue = 1.0
            )
        )

        datos.find(rawResponseText)?.let { m ->
            val amount = NationalRechargeLimitParser.parseAmount(m.groupValues[1])
            val unit = m.groupValues[2]
            val mb = if (unit.equals("GB", ignoreCase = true)) amount * 1024 else amount
            val isActive = m.groupValues[3].startsWith("validos", ignoreCase = true)
            val days = if (isActive) m.groupValues[4].toIntOrNull() else null

            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DATOS_ACTIVE, numericValue = if (isActive) 1.0 else 0.0))
            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DATOS_MB, numericValue = mb, unit = "MB"))
            if (days != null) {
                values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DATOS_DAYS, numericValue = days.toDouble()))
            }
        }

        diaria.find(rawResponseText)?.let { m ->
            val amount = NationalRechargeLimitParser.parseAmount(m.groupValues[1])
            val isActive = m.groupValues[2].startsWith("validos", ignoreCase = true)

            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DIARIA_ACTIVE, numericValue = if (isActive) 1.0 else 0.0))
            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DIARIA_MB, numericValue = amount, unit = "MB"))
        }

        todus.find(rawResponseText)?.let { m ->
            val amount = NationalRechargeLimitParser.parseAmount(m.groupValues[1])
            val days = m.groupValues[2].toIntOrNull()

            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_TODUS_MB, numericValue = amount, unit = "MB"))
            if (days != null) {
                values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_TODUS_DAYS, numericValue = days.toDouble()))
            }
        }

        return ParseResult.Success(values)
    }
}
