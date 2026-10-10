package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class NationalRechargeLimitParserTest {

    private val parser = NationalRechargeLimitParser()

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    // ── Variant 1: can recharge amount within period ──

    @Test
    fun `variant 1 - can recharge full limit within period`() {
        val raw = "Ud puede recargar un monto de 360,00CUP en un plazo de 30 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val amount = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT }
        assertEquals(360.0, amount.numericValue!!, 0.01)
        assertEquals("CUP", amount.unit)

        val reached = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED }
        assertEquals(0.0, reached.numericValue!!, 0.01)

        val period = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_PERIOD_DAYS }
        assertEquals(30.0, period.numericValue!!, 0.01)
    }

    // ── Variant 2: can recharge remaining amount until date ──

    @Test
    fun `variant 2 - can recharge remaining amount until date (2-digit year)`() {
        val raw = "Ud puede recargar un monto de 120,00CUP hasta el 25-10-25"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val remaining = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_REMAINING }
        assertEquals(120.0, remaining.numericValue!!, 0.01)
        assertEquals("CUP", remaining.unit)

        val reached = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED }
        assertEquals(0.0, reached.numericValue!!, 0.01)

        val availFrom = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM }
        assertEquals(epochOf(2025, 10, 25), availFrom.dateValue)
    }

    // ── Variant 3: limit reached, available from date ──

    @Test
    fun `variant 3 - limit reached with available-from date (2-digit year)`() {
        val raw = "Ud ha alcanzado el monto de recarga permitido de 360 CUP. Puede recargar posterior al dia 24-10-26"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val amount = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT }
        assertEquals(360.0, amount.numericValue!!, 0.01)
        assertEquals("CUP", amount.unit)

        val reached = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED }
        assertEquals(1.0, reached.numericValue!!, 0.01)

        val availFrom = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM }
        assertEquals(epochOf(2026, 10, 24), availFrom.dateValue)
    }

    // ── Edge cases ──

    @Test
    fun `variant 3 - limit reached with 4-digit year`() {
        val raw = "Ud ha alcanzado el monto de recarga permitido de 360 CUP. Puede recargar posterior al dia 24-10-2026"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val availFrom = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM }
        assertEquals(epochOf(2026, 10, 24), availFrom.dateValue)
    }

    @Test
    fun `variant 1 - amount with dot decimal separator`() {
        val raw = "Ud puede recargar un monto de 360.00CUP en un plazo de 30 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val amount = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT }
        assertEquals(360.0, amount.numericValue!!, 0.01)
    }

    @Test
    fun `variant 1 - integer amount without decimals`() {
        val raw = "Ud puede recargar un monto de 360CUP en un plazo de 30 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val amount = values.first { it.fieldType == FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT }
        assertEquals(360.0, amount.numericValue!!, 0.01)
    }

    @Test
    fun `variant 3 - amount with space before CUP`() {
        val raw = "Ud ha alcanzado el monto de recarga permitido de 360,00 CUP. Puede recargar posterior al dia 24-10-26"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
    }

    @Test
    fun `unrecognized text returns Unrecognized`() {
        val raw = "Servicio no disponible"
        val result = parser.parse(raw)
        assertTrue("expected Unrecognized, got $result", result is ParseResult.Unrecognized)
    }

    @Test
    fun `empty string returns Unrecognized`() {
        val result = parser.parse("")
        assertTrue(result is ParseResult.Unrecognized)
    }

    @Test
    fun `ussdCodeId is national-recharge-limit`() {
        assertEquals("national-recharge-limit", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers and not a stub`() {
        val registered = UssdParsers.forCode("national-recharge-limit")
        assertTrue(
            "expected NationalRechargeLimitParser, got ${registered::class.simpleName}",
            registered is NationalRechargeLimitParser
        )
    }
}
