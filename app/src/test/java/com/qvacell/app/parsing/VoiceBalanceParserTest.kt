package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceBalanceParserTest {

    private val parser = VoiceBalanceParser()

    // ── Variant 1: large balance ──

    @Test
    fun `variant 1 - large balance with days remaining`() {
        val raw = "Usted dispone de 119:51:13 MIN NAC validos por 19 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val active = values.first { it.fieldType == FieldTypes.VOICE_PLAN_ACTIVE }
        assertEquals(1.0, active.numericValue!!, 0.01)

        val mins = values.first { it.fieldType == FieldTypes.VOICE_MINUTES_REMAINING }
        // 119*60 + 51 + 13/60 = 7191.2167
        assertEquals(7191.22, mins.numericValue!!, 0.01)
        assertEquals("119:51:13", mins.textValue)
        assertEquals("MIN NAC", mins.unit)

        val days = values.first { it.fieldType == FieldTypes.VOICE_DAYS_REMAINING }
        assertEquals(19.0, days.numericValue!!, 0.01)
    }

    // ── Variant 2: small balance ──

    @Test
    fun `variant 2 - small balance 45 minutes`() {
        val raw = "Usted dispone de 00:45:00 MIN NAC validos por 21 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val mins = values.first { it.fieldType == FieldTypes.VOICE_MINUTES_REMAINING }
        assertEquals(45.0, mins.numericValue!!, 0.01)
        assertEquals("00:45:00", mins.textValue)

        val days = values.first { it.fieldType == FieldTypes.VOICE_DAYS_REMAINING }
        assertEquals(21.0, days.numericValue!!, 0.01)
    }

    // ── Variant 3: no plan ──

    @Test
    fun `variant 3 - no plan active`() {
        val raw = "Usted debe adquirir un plan de minutos. Para una nueva compra marque *133#"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        assertEquals(1, values.size)
        val active = values.first { it.fieldType == FieldTypes.VOICE_PLAN_ACTIVE }
        assertEquals(0.0, active.numericValue!!, 0.01)
    }

    // ── Edge cases ──

    @Test
    fun `zero balance`() {
        val raw = "Usted dispone de 0:00:00 MIN NAC validos por 1 dias"
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val mins = values.first { it.fieldType == FieldTypes.VOICE_MINUTES_REMAINING }
        assertEquals(0.0, mins.numericValue!!, 0.01)
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
    fun `ussdCodeId is voice-balance`() {
        assertEquals("voice-balance", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers`() {
        val registered = UssdParsers.forCode("voice-balance")
        assertTrue(registered is VoiceBalanceParser)
    }
}
