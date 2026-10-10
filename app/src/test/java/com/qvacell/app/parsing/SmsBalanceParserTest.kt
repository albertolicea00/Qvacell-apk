package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsBalanceParserTest {

    private val parser = SmsBalanceParser()

    // ── Variant 1: large SMS count ──

    @Test
    fun `variant 1 - large SMS count with days`() {
        val raw = "Usted dispone de 8401 SMS validos por 19 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val active = values.first { it.fieldType == FieldTypes.SMS_PLAN_ACTIVE }
        assertEquals(1.0, active.numericValue!!, 0.01)

        val count = values.first { it.fieldType == FieldTypes.SMS_COUNT_REMAINING }
        assertEquals(8401.0, count.numericValue!!, 0.01)
        assertEquals("SMS", count.unit)

        val days = values.first { it.fieldType == FieldTypes.SMS_DAYS_REMAINING }
        assertEquals(19.0, days.numericValue!!, 0.01)
    }

    // ── Variant 2: small SMS count ──

    @Test
    fun `variant 2 - small SMS count`() {
        val raw = "Usted dispone de 57 SMS validos por 19 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val count = values.first { it.fieldType == FieldTypes.SMS_COUNT_REMAINING }
        assertEquals(57.0, count.numericValue!!, 0.01)
    }

    // ── Variant 3: no plan ──

    @Test
    fun `variant 3 - no plan active`() {
        val raw = "Usted debe adquirir un plan de SMS. Para una nueva compra marque *133#"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        assertEquals(1, values.size)
        val active = values.first { it.fieldType == FieldTypes.SMS_PLAN_ACTIVE }
        assertEquals(0.0, active.numericValue!!, 0.01)
    }

    // ── Edge cases ──

    @Test
    fun `zero SMS remaining`() {
        val raw = "Usted dispone de 0 SMS validos por 1 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val count = values.first { it.fieldType == FieldTypes.SMS_COUNT_REMAINING }
        assertEquals(0.0, count.numericValue!!, 0.01)
    }

    @Test
    fun `unrecognized text`() {
        val result = parser.parse("Servicio no disponible")
        assertTrue(result is ParseResult.Unrecognized)
    }

    @Test
    fun `empty string`() {
        val result = parser.parse("")
        assertTrue(result is ParseResult.Unrecognized)
    }

    @Test
    fun `ussdCodeId is sms-balance`() {
        assertEquals("sms-balance", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers`() {
        val registered = UssdParsers.forCode("sms-balance")
        assertTrue(registered is SmsBalanceParser)
    }
}
