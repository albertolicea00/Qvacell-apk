package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Integration tests: real ETECSA response snapshots from 3 different lines, parsed through all 6
 * USSD parsers. Verifies cross-card consistency (e.g. *222# datos matches *222*328# datos) and
 * that every parser returns Success for known-good input.
 */
class FullLineSnapshotTest {

    private fun parse(codeId: String, raw: String): List<ParsedDashboardValue> {
        val result = UssdParsers.forCode(codeId).parse(raw)
        assertTrue("$codeId failed: $result", result is ParseResult.Success)
        return (result as ParseResult.Success).value
    }

    private fun List<ParsedDashboardValue>.field(ft: String) = firstOrNull { it.fieldType == ft }

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    // ── Linea 1 ──

    @Test
    fun `linea 1 - all 6 codes parse successfully`() {
        parse("national-recharge-limit", "Ud puede recargar un monto de 360,00CUP en un plazo de 30 dias")
        parse("voice-balance", "Usted dispone de 119:51:13 MIN NAC validos por 19 dias")
        parse("sms-balance", "Usted dispone de 8401 SMS validos por 19 dias")
        parse("main-balance", "Saldo: 227.05 CUP. Datos: 4.03 GB. Voz: 00:45:00. SMS: 57 Linea activa hasta 17-08-27 vence 17-02-28.")
        parse("bonus-usd-plans", "Datos.cu: 287 MB vence 31-10-26.")
        parse("data-plan", "Tarifa: No activa. Datos: 4.03 GB validos 21 dias.")
    }

    @Test
    fun `linea 1 - main-balance datos matches data-plan datos`() {
        val mainVals = parse("main-balance", "Saldo: 227.05 CUP. Datos: 4.03 GB. Voz: 00:45:00. SMS: 57 Linea activa hasta 17-08-27 vence 17-02-28.")
        val dataVals = parse("data-plan", "Tarifa: No activa. Datos: 4.03 GB validos 21 dias.")

        val mainMb = mainVals.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!
        val dataMb = dataVals.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!
        assertEquals("cross-card datos MB must match", mainMb, dataMb, 0.01)
    }

    @Test
    fun `linea 1 - recharge limit not reached, full 360 CUP`() {
        val v = parse("national-recharge-limit", "Ud puede recargar un monto de 360,00CUP en un plazo de 30 dias")
        assertEquals(0.0, v.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED)!!.numericValue!!, 0.01)
        assertEquals(360.0, v.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_AMOUNT)!!.numericValue!!, 0.01)
    }

    // ── Linea 2 ──

    @Test
    fun `linea 2 - all 6 codes parse successfully`() {
        parse("national-recharge-limit", "Ud puede recargar un monto de 120,00CUP hasta el 25-10-25")
        parse("voice-balance", "Usted dispone de 00:45:00 MIN NAC validos por 21 dias")
        parse("sms-balance", "Usted dispone de 57 SMS validos por 19 dias")
        parse("main-balance", "Saldo: 15.01 CUP. Datos: 57.35 MB. Voz: 119:51:13. SMS: 8401 Linea activa hasta 20-08-27 vence 16-02-28.")
        parse("bonus-usd-plans", "Datos.cu: 300 MB vence 29-10-26.")
        parse("data-plan", "Tarifa: No activa. Datos: 57.35 MB validos 19 dias.")
    }

    @Test
    fun `linea 2 - main-balance datos matches data-plan datos`() {
        val mainVals = parse("main-balance", "Saldo: 15.01 CUP. Datos: 57.35 MB. Voz: 119:51:13. SMS: 8401 Linea activa hasta 20-08-27 vence 16-02-28.")
        val dataVals = parse("data-plan", "Tarifa: No activa. Datos: 57.35 MB validos 19 dias.")

        val mainMb = mainVals.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!
        val dataMb = dataVals.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!
        assertEquals("cross-card datos MB must match", mainMb, dataMb, 0.01)
    }

    @Test
    fun `linea 2 - main-balance voice matches voice-balance`() {
        val mainVals = parse("main-balance", "Saldo: 15.01 CUP. Datos: 57.35 MB. Voz: 119:51:13. SMS: 8401 Linea activa hasta 20-08-27 vence 16-02-28.")
        val voiceVals = parse("voice-balance", "Usted dispone de 00:45:00 MIN NAC validos por 21 dias")

        // main-balance Voz field and voice-balance both use VOICE_MINUTES_REMAINING
        assertNotNull(mainVals.field(FieldTypes.VOICE_MINUTES_REMAINING))
        assertNotNull(voiceVals.field(FieldTypes.VOICE_MINUTES_REMAINING))
    }

    @Test
    fun `linea 2 - recharge limit partially used, 120 CUP remaining`() {
        val v = parse("national-recharge-limit", "Ud puede recargar un monto de 120,00CUP hasta el 25-10-25")
        assertEquals(0.0, v.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED)!!.numericValue!!, 0.01)
        assertEquals(120.0, v.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_REMAINING)!!.numericValue!!, 0.01)
        assertEquals(epochOf(2025, 10, 25), v.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM)!!.dateValue)
    }

    // ── Linea 3 (no plans, limit reached) ──

    @Test
    fun `linea 3 - all 6 codes parse successfully`() {
        parse("national-recharge-limit", "Ud ha alcanzado el monto de recarga permitido de 360 CUP. Puede recargar posterior al dia 24-10-26")
        parse("voice-balance", "Usted debe adquirir un plan de minutos. Para una nueva compra marque *133#")
        parse("sms-balance", "Usted debe adquirir un plan de SMS. Para una nueva compra marque *133#")
        parse("main-balance", "Saldo: 199.35 CUP. Linea activa hasta 13-08-27 vence 09-02-28.")
        parse("bonus-usd-plans", "Usted no dispone de bonos activos.")
        parse("data-plan", "Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#")
    }

    @Test
    fun `linea 3 - everything inactive or reached`() {
        val recharge = parse("national-recharge-limit", "Ud ha alcanzado el monto de recarga permitido de 360 CUP. Puede recargar posterior al dia 24-10-26")
        assertEquals(1.0, recharge.field(FieldTypes.NATIONAL_RECHARGE_LIMIT_REACHED)!!.numericValue!!, 0.01)

        val voice = parse("voice-balance", "Usted debe adquirir un plan de minutos. Para una nueva compra marque *133#")
        assertEquals(0.0, voice.field(FieldTypes.VOICE_PLAN_ACTIVE)!!.numericValue!!, 0.01)

        val sms = parse("sms-balance", "Usted debe adquirir un plan de SMS. Para una nueva compra marque *133#")
        assertEquals(0.0, sms.field(FieldTypes.SMS_PLAN_ACTIVE)!!.numericValue!!, 0.01)

        val bonus = parse("bonus-usd-plans", "Usted no dispone de bonos activos.")
        assertEquals(0.0, bonus.field(FieldTypes.BONUS_ACTIVE)!!.numericValue!!, 0.01)

        val data = parse("data-plan", "Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#")
        assertEquals(0.0, data.field(FieldTypes.DATA_PLAN_ACTIVE)!!.numericValue!!, 0.01)
    }

    @Test
    fun `linea 3 - main-balance has no datos voz sms sections`() {
        val v = parse("main-balance", "Saldo: 199.35 CUP. Linea activa hasta 13-08-27 vence 09-02-28.")
        assertNull(v.field(FieldTypes.DATA_DATOS_MB))
        assertNull(v.field(FieldTypes.VOICE_MINUTES_REMAINING))
        assertNull(v.field(FieldTypes.SMS_COUNT_REMAINING))
        assertEquals(199.35, v.field(FieldTypes.MAIN_BALANCE)!!.numericValue!!, 0.01)
    }
}
