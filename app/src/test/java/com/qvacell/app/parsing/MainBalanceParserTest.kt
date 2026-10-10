package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MainBalanceParserTest {

    private val parser = MainBalanceParser()

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun values(raw: String): List<ParsedDashboardValue> {
        val result = parser.parse(raw)
        assertTrue("expected Success, got $result", result is ParseResult.Success)
        return (result as ParseResult.Success).value
    }

    private fun List<ParsedDashboardValue>.field(ft: String) = firstOrNull { it.fieldType == ft }

    // ── Sample 1: full response with all sections ──

    @Test
    fun `full response - balance datos voz sms linea`() {
        val raw = "Saldo: 227.05 CUP. Datos: 4.03 GB. Voz: 00:45:00. SMS: 57 Linea activa hasta 17-08-27 vence 17-02-28."
        val v = values(raw)

        assertEquals(227.05, v.field(FieldTypes.MAIN_BALANCE)!!.numericValue!!, 0.01)
        assertEquals("CUP", v.field(FieldTypes.MAIN_BALANCE)!!.unit)

        assertEquals(4.03 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)

        // 00:45:00 = 45 minutes
        assertEquals(45.0, v.field(FieldTypes.VOICE_MINUTES_REMAINING)!!.numericValue!!, 0.01)
        assertEquals("00:45:00", v.field(FieldTypes.VOICE_MINUTES_REMAINING)!!.textValue)

        assertEquals(57.0, v.field(FieldTypes.SMS_COUNT_REMAINING)!!.numericValue!!, 0.01)

        assertEquals(epochOf(2027, 8, 17), v.field(FieldTypes.LINE_ACTIVE_UNTIL)!!.dateValue)
        assertEquals(epochOf(2028, 2, 17), v.field(FieldTypes.ACCOUNT_DUE_DATE)!!.dateValue)
    }

    // ── Sample 2: full response with large voice and SMS ──

    @Test
    fun `full response - large voice and sms`() {
        val raw = "Saldo: 15.01 CUP. Datos: 57.35 MB. Voz: 119:51:13. SMS: 8401 Linea activa hasta 20-08-27 vence 16-02-28."
        val v = values(raw)

        assertEquals(15.01, v.field(FieldTypes.MAIN_BALANCE)!!.numericValue!!, 0.01)
        assertEquals(57.35, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.01)

        // 119*60 + 51 + 13/60 = 7191.2167
        assertEquals(7191.22, v.field(FieldTypes.VOICE_MINUTES_REMAINING)!!.numericValue!!, 0.01)
        assertEquals("119:51:13", v.field(FieldTypes.VOICE_MINUTES_REMAINING)!!.textValue)

        assertEquals(8401.0, v.field(FieldTypes.SMS_COUNT_REMAINING)!!.numericValue!!, 0.01)

        assertEquals(epochOf(2027, 8, 20), v.field(FieldTypes.LINE_ACTIVE_UNTIL)!!.dateValue)
        assertEquals(epochOf(2028, 2, 16), v.field(FieldTypes.ACCOUNT_DUE_DATE)!!.dateValue)
    }

    // ── Sample 3: balance only, no plans ──

    @Test
    fun `balance only - no datos voz sms`() {
        val raw = "Saldo: 199.35 CUP. Linea activa hasta 13-08-27 vence 09-02-28."
        val v = values(raw)

        assertEquals(199.35, v.field(FieldTypes.MAIN_BALANCE)!!.numericValue!!, 0.01)
        assertNull(v.field(FieldTypes.DATA_DATOS_MB))
        assertNull(v.field(FieldTypes.VOICE_MINUTES_REMAINING))
        assertNull(v.field(FieldTypes.SMS_COUNT_REMAINING))
        assertEquals(epochOf(2027, 8, 13), v.field(FieldTypes.LINE_ACTIVE_UNTIL)!!.dateValue)
        assertEquals(epochOf(2028, 2, 9), v.field(FieldTypes.ACCOUNT_DUE_DATE)!!.dateValue)
    }

    // ── Cross-card field reuse ──

    @Test
    fun `datos field reuses DATA_DATOS_MB from data-plan parser`() {
        val v = values("Saldo: 100.00 CUP. Datos: 2.50 GB. Linea activa hasta 01-01-27 vence 01-07-27.")
        val mb = v.field(FieldTypes.DATA_DATOS_MB)!!
        assertEquals(2.50 * 1024, mb.numericValue!!, 0.1)
        assertEquals("MB", mb.unit)
    }

    @Test
    fun `voice field reuses VOICE_MINUTES_REMAINING from voice-balance parser`() {
        val v = values("Saldo: 100.00 CUP. Voz: 02:30:00. Linea activa hasta 01-01-27 vence 01-07-27.")
        val mins = v.field(FieldTypes.VOICE_MINUTES_REMAINING)!!
        assertEquals(150.0, mins.numericValue!!, 0.01)
        assertEquals("MIN NAC", mins.unit)
    }

    @Test
    fun `sms field reuses SMS_COUNT_REMAINING from sms-balance parser`() {
        val v = values("Saldo: 100.00 CUP. SMS: 500 Linea activa hasta 01-01-27 vence 01-07-27.")
        assertEquals(500.0, v.field(FieldTypes.SMS_COUNT_REMAINING)!!.numericValue!!, 0.01)
    }

    // ── Edge cases ──

    @Test
    fun `comma decimal separator in saldo`() {
        val v = values("Saldo: 227,05 CUP. Linea activa hasta 17-08-27 vence 17-02-28.")
        assertEquals(227.05, v.field(FieldTypes.MAIN_BALANCE)!!.numericValue!!, 0.01)
    }

    @Test
    fun `unrecognized text - no Saldo`() {
        val result = parser.parse("Servicio no disponible")
        assertTrue(result is ParseResult.Unrecognized)
    }

    @Test
    fun `empty string`() {
        val result = parser.parse("")
        assertTrue(result is ParseResult.Unrecognized)
    }

    @Test
    fun `ussdCodeId is main-balance`() {
        assertEquals("main-balance", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers`() {
        val registered = UssdParsers.forCode("main-balance")
        assertTrue(registered is MainBalanceParser)
    }
}
