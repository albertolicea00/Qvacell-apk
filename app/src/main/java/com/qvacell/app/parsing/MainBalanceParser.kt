package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes

class MainBalanceParser : UssdResponseParser {
    override val ussdCodeId: String = "main-balance"

    private val saldo = Regex("""Saldo\s*[.:]?\s*([\d,.]+)\s*CUP""", RegexOption.IGNORE_CASE)
    private val datos = Regex("""Datos\s*[.:]?\s*([\d,.]+)\s*(MB|GB)""", RegexOption.IGNORE_CASE)
    private val voz = Regex("""Voz\s*[.:]?\s*(\d{1,3})\s*:\s*(\d{2})\s*:\s*(\d{2})""", RegexOption.IGNORE_CASE)
    private val sms = Regex("""SMS\s*[.:]?\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val lineaActiva = Regex("""Linea\s+activa\s+hasta\s*[.:]?\s*(\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE)
    private val vence = Regex("""vence\s*[.:]?\s*(\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE)

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        val response = UssdTextNormalizer.normalize(rawResponseText)
        val saldoMatch = saldo.find(response)
            ?: return ParseResult.Unrecognized(rawResponseText)

        val values = mutableListOf<ParsedDashboardValue>()

        values.add(
            ParsedDashboardValue(
                fieldType = FieldTypes.MAIN_BALANCE,
                numericValue = NationalRechargeLimitParser.parseAmount(saldoMatch.groupValues[1]),
                unit = "CUP"
            )
        )

        datos.find(response)?.let { m ->
            val amount = NationalRechargeLimitParser.parseAmount(m.groupValues[1])
            val unit = m.groupValues[2]
            val mb = if (unit.equals("GB", ignoreCase = true)) amount * 1024 else amount
            values.add(ParsedDashboardValue(fieldType = FieldTypes.DATA_DATOS_MB, numericValue = mb, unit = "MB"))
        }

        voz.find(response)?.let { m ->
            val hours = m.groupValues[1].toInt()
            val minutes = m.groupValues[2].toInt()
            val seconds = m.groupValues[3].toInt()
            val totalMinutes = hours * 60.0 + minutes + seconds / 60.0
            values.add(
                ParsedDashboardValue(
                    fieldType = FieldTypes.VOICE_MINUTES_REMAINING,
                    numericValue = totalMinutes,
                    textValue = "${m.groupValues[1]}:${m.groupValues[2]}:${m.groupValues[3]}",
                    unit = "MIN NAC"
                )
            )
        }

        sms.find(response)?.let { m ->
            val count = m.groupValues[1].toInt()
            values.add(ParsedDashboardValue(fieldType = FieldTypes.SMS_COUNT_REMAINING, numericValue = count.toDouble(), unit = "SMS"))
        }

        lineaActiva.find(response)?.let { m ->
            NationalRechargeLimitParser.parseDateDmy(m.groupValues[1])?.let { epoch ->
                values.add(ParsedDashboardValue(fieldType = FieldTypes.LINE_ACTIVE_UNTIL, dateValue = epoch))
            }
        }

        vence.find(response)?.let { m ->
            NationalRechargeLimitParser.parseDateDmy(m.groupValues[1])?.let { epoch ->
                values.add(ParsedDashboardValue(fieldType = FieldTypes.ACCOUNT_DUE_DATE, dateValue = epoch))
            }
        }

        return ParseResult.Success(values)
    }
}
