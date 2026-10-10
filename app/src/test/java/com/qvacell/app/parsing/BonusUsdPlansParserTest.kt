package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class BonusUsdPlansParserTest {

    private val parser = BonusUsdPlansParser()

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    // ── Variant 1: single Datos.cu bonus ──

    @Test
    fun `variant 1 - datos cu 287 MB`() {
        val raw = "Datos.cu: 287 MB vence 31-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val active = values.first { it.fieldType == FieldTypes.BONUS_ACTIVE }
        assertEquals(1.0, active.numericValue!!, 0.01)

        val mb = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB }
        assertEquals(287.0, mb.numericValue!!, 0.01)
        assertEquals("MB", mb.unit)

        val expiry = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_EXPIRY }
        assertEquals(epochOf(2026, 10, 31), expiry.dateValue)

        assertNull(values.firstOrNull { it.fieldType == FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY })
    }

    @Test
    fun `variant 1 - datos cu 300 MB`() {
        val raw = "Datos.cu: 300 MB vence 29-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val mb = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB }
        assertEquals(300.0, mb.numericValue!!, 0.01)
    }

    // ── Variant 2: no bonuses ──

    @Test
    fun `variant 2 - no active bonuses`() {
        val raw = "Usted no dispone de bonos activos."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        assertEquals(1, values.size)
        val active = values.first { it.fieldType == FieldTypes.BONUS_ACTIVE }
        assertEquals(0.0, active.numericValue!!, 0.01)
    }

    // ── Variant 3: multi-bonus (nocturno + datos.cu) ──

    @Test
    fun `variant 3 - nocturno and datos cu combined`() {
        val raw = "Datos: ilimitados: Nocturno vence 24-10-26. Datos.cu 285 MB vence 27-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        val active = values.first { it.fieldType == FieldTypes.BONUS_ACTIVE }
        assertEquals(1.0, active.numericValue!!, 0.01)

        val nocturno = values.first { it.fieldType == FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY }
        assertEquals(epochOf(2026, 10, 24), nocturno.dateValue)

        val mb = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB }
        assertEquals(285.0, mb.numericValue!!, 0.01)

        val cuExpiry = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_EXPIRY }
        assertEquals(epochOf(2026, 10, 27), cuExpiry.dateValue)
    }

    // ── Edge cases ──

    @Test
    fun `nocturno only without datos cu`() {
        val raw = "Datos: ilimitados: Nocturno vence 24-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value

        assertNotNull(values.firstOrNull { it.fieldType == FieldTypes.BONUS_DATOS_NOCTURNO_EXPIRY })
        assertNull(values.firstOrNull { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB })
    }

    @Test
    fun `datos cu without colon`() {
        val raw = "Datos.cu 285 MB vence 27-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val mb = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB }
        assertEquals(285.0, mb.numericValue!!, 0.01)
    }

    @Test
    fun `datos cu in GB converts to MB`() {
        val raw = "Datos.cu: 1 GB vence 31-10-26."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val mb = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_MB }
        assertEquals(1024.0, mb.numericValue!!, 0.01)
        assertEquals("MB", mb.unit)
    }

    @Test
    fun `4-digit year date`() {
        val raw = "Datos.cu: 287 MB vence 31-10-2026."
        val result = parser.parse(raw)
        assertTrue(result is ParseResult.Success)
        val values = (result as ParseResult.Success).value
        val expiry = values.first { it.fieldType == FieldTypes.BONUS_DATOS_CU_EXPIRY }
        assertEquals(epochOf(2026, 10, 31), expiry.dateValue)
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
    fun `ussdCodeId is bonus-usd-plans`() {
        assertEquals("bonus-usd-plans", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers`() {
        val registered = UssdParsers.forCode("bonus-usd-plans")
        assertTrue(registered is BonusUsdPlansParser)
    }
}
