package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NationalRechargeLimitParser : UssdResponseParser {
    override val ussdCodeId: String = "national-recharge-limit"

    private val canRechargeAmountInPeriod = Regex(
        """Ud puede recargar un monto de ([\d,.]+)\s*CUP en un plazo de (\d+) dias""", RegexOption.IGNORE_CASE
    )

    private val canRechargeAmountUntilDate = Regex(
        """Ud puede recargar un monto de ([\d,.]+)\s*CUP hasta el (\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE
    )

    private val limitReached = Regex(
        """Ud ha alcanzado el monto de recarga permitido de ([\d,.]+)\s*CUP\.\s*Puede recargar posterior al dia (\d{2}-\d{2}-\d{2,4})""", RegexOption.IGNORE_CASE
    )

    override fun parse(rawResponseText: String): ParseResult<List<ParsedDashboardValue>> {
        canRechargeAmountInPeriod.find(rawResponseText)?.let { m ->
            val amount = parseAmount(m.groupValues[1])
            val periodDays = m.groupValues[2].toIntOrNull() ?: return ParseResult.Unrecognized(rawResponseText)
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT,
                        numericValue = amount,
                        unit = "CUP"
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED,
                        numericValue = 0.0
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_PERIOD_DAYS,
                        numericValue = periodDays.toDouble()
                    )
                )
            )
        }

        canRechargeAmountUntilDate.find(rawResponseText)?.let { m ->
            val remaining = parseAmount(m.groupValues[1])
            val dateEpoch = parseDateDmy(m.groupValues[2])
                ?: return ParseResult.Unrecognized(rawResponseText)
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_REMAINING,
                        numericValue = remaining,
                        unit = "CUP"
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED,
                        numericValue = 0.0
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM,
                        dateValue = dateEpoch
                    )
                )
            )
        }

        limitReached.find(rawResponseText)?.let { m ->
            val amount = parseAmount(m.groupValues[1])
            val dateEpoch = parseDateDmy(m.groupValues[2])
                ?: return ParseResult.Unrecognized(rawResponseText)
            return ParseResult.Success(
                listOf(
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT,
                        numericValue = amount,
                        unit = "CUP"
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED,
                        numericValue = 1.0
                    ),
                    ParsedDashboardValue(
                        fieldType = FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM,
                        dateValue = dateEpoch
                    )
                )
            )
        }

        return ParseResult.Unrecognized(rawResponseText)
    }

    companion object {
        private val DMY_2_DIGIT = DateTimeFormatter.ofPattern("dd-MM-yy")
        private val DMY_4_DIGIT = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        internal fun parseAmount(raw: String): Double =
            raw.replace(",", ".").toDouble()

        internal fun parseDateDmy(raw: String): Long? = try {
            val formatter = if (raw.length <= 8) DMY_2_DIGIT else DMY_4_DIGIT
            LocalDate.parse(raw, formatter)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
