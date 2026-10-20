package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorporateBalanceParserTest {
    private val parser = CorporateBalanceParser()

    @Test
    fun `parses the supplied corporate voice sms and data response without inventing a main balance`() {
        val response = "Voz: 335 MIN. Saldo consumido 0.00 CUP. SMS 60. Saldo consumido 0.00 CUP. Datos: 0 MB. Roaming:"
        val result = parser.parse(response)
        assertTrue(result is ParseResult.Success)
        val fields = (result as ParseResult.Success).value.associateBy { it.fieldType }

        assertEquals(335.0, fields[FieldTypes.VOICE_MINUTES_REMAINING]?.numericValue)
        assertEquals("335 MIN", fields[FieldTypes.VOICE_MINUTES_REMAINING]?.textValue)
        assertEquals(0.0, fields[FieldTypes.CORPORATE_VOICE_CONSUMED_CUP]?.numericValue)
        assertEquals(60.0, fields[FieldTypes.SMS_COUNT_REMAINING]?.numericValue)
        assertEquals(0.0, fields[FieldTypes.CORPORATE_SMS_CONSUMED_CUP]?.numericValue)
        assertEquals(0.0, fields[FieldTypes.DATA_DATOS_MB]?.numericValue)
        assertEquals("", fields[FieldTypes.CORPORATE_ROAMING]?.textValue)
        assertFalse(fields.containsKey(FieldTypes.MAIN_BALANCE))
    }

    @Test
    fun `parses data-only corporate lines and blank roaming from the supplied examples`() {
        val firstResponse = "Example Corp Line\nLinea 1: Datos: 1164 MB. Roaming:"
        val secondResponse = "Example Corp Line\nLinea 2: Datos: 20223 MB. Roaming:"

        listOf(firstResponse to 1164.0, secondResponse to 20223.0).forEach { (response, expectedMb) ->
            val result = parser.parse(response)
            assertTrue(result is ParseResult.Success)
            val fields = (result as ParseResult.Success).value.associateBy { it.fieldType }

            assertEquals(expectedMb, fields[FieldTypes.DATA_DATOS_MB]?.numericValue)
            assertEquals("", fields[FieldTypes.CORPORATE_ROAMING]?.textValue)
            assertFalse(fields.containsKey(FieldTypes.MAIN_BALANCE))
        }
    }

    @Test
    fun `rejects a response with no corporate metrics`() {
        assertTrue(parser.parse("Error de consulta") is ParseResult.Unrecognized)
    }

    @Test
    fun `assigns each consumed amount to its own section`() {
        val response = "Voz: 10 MIN. Saldo consumido 1.25 CUP. SMS: 3 Saldo consumido 2.50 CUP. Datos: 1GB. Roaming: Activo"
        val fields = (parser.parse(response) as ParseResult.Success).value.associateBy { it.fieldType }

        assertEquals(1.25, fields[FieldTypes.CORPORATE_VOICE_CONSUMED_CUP]?.numericValue)
        assertEquals(2.5, fields[FieldTypes.CORPORATE_SMS_CONSUMED_CUP]?.numericValue)
        assertEquals(1024.0, fields[FieldTypes.DATA_DATOS_MB]?.numericValue)
        assertEquals("Activo", fields[FieldTypes.CORPORATE_ROAMING]?.textValue)
    }

    @Test
    fun `tolerates case accents spacing and punctuation in corporate fields`() {
        val raw = "VOZ . 335 MIN. saldo : consumido 0,00 cup. SMS . 60. Saldo consumido 0,00 CUP. DATOS : 0 MB. ROAMING :"
        val result = parser.parse(raw)
        assertTrue("expected Success, got $result", result is ParseResult.Success)
        val fields = (result as ParseResult.Success).value.associateBy { it.fieldType }

        assertEquals(335.0, fields[FieldTypes.VOICE_MINUTES_REMAINING]?.numericValue)
        assertEquals(60.0, fields[FieldTypes.SMS_COUNT_REMAINING]?.numericValue)
        assertEquals(0.0, fields[FieldTypes.CORPORATE_VOICE_CONSUMED_CUP]?.numericValue)
        assertEquals("", fields[FieldTypes.CORPORATE_ROAMING]?.textValue)
    }
}
