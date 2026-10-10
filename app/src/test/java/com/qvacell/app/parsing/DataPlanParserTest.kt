package com.qvacell.app.parsing

import com.qvacell.app.data.FieldTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPlanParserTest {

    private val parser = DataPlanParser()

    private fun values(raw: String): List<ParsedDashboardValue> {
        val result = parser.parse(raw)
        assertTrue("expected Success, got $result", result is ParseResult.Success)
        return (result as ParseResult.Success).value
    }

    private fun List<ParsedDashboardValue>.field(ft: String) = firstOrNull { it.fieldType == ft }

    // ── Sample 1: datos only, inactive ──

    @Test
    fun `datos GB no activos`() {
        val v = values("Tarifa: No activa. Datos: 4.49 GB no activos.")
        assertEquals(0.0, v.field(FieldTypes.DATA_TARIFF_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(1.0, v.field(FieldTypes.DATA_PLAN_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(0.0, v.field(FieldTypes.DATA_DATOS_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(4.49 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)
        assertNull(v.field(FieldTypes.DATA_DATOS_DAYS))
    }

    // ── Sample 2: diaria active + paquetes empty ──

    @Test
    fun `diaria active 24h plus paquetes empty`() {
        val v = values("Tarifa: No activa. Diaria: 195 MB validos 24 horas. Paquetes: No dispone de MB.")
        assertEquals(1.0, v.field(FieldTypes.DATA_DIARIA_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(195.0, v.field(FieldTypes.DATA_DIARIA_MB)!!.numericValue!!, 0.01)
        assertNull(v.field(FieldTypes.DATA_DATOS_MB))
    }

    // ── Sample 3: diaria inactive + datos active ──

    @Test
    fun `diaria inactive plus datos active`() {
        val v = values("Tarifa: No activa. Diaria: 200 MB no activos. Datos: 4.03 GB validos 21 dias.")
        assertEquals(0.0, v.field(FieldTypes.DATA_DIARIA_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(200.0, v.field(FieldTypes.DATA_DIARIA_MB)!!.numericValue!!, 0.01)
        assertEquals(1.0, v.field(FieldTypes.DATA_DATOS_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(4.03 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)
        assertEquals(21.0, v.field(FieldTypes.DATA_DATOS_DAYS)!!.numericValue!!, 0.01)
    }

    // ── Sample 4: diaria + todus + datos (triple) ──

    @Test
    fun `triple section - diaria inactive plus todus plus datos`() {
        val v = values("Tarifa: No activa. Diaria: 200 MB no activos. toDus: 1024.00 MB validos 34 dias. Datos: 4.03 GB validos 21 dias.")
        assertEquals(0.0, v.field(FieldTypes.DATA_DIARIA_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(1024.0, v.field(FieldTypes.DATA_TODUS_MB)!!.numericValue!!, 0.01)
        assertEquals(34.0, v.field(FieldTypes.DATA_TODUS_DAYS)!!.numericValue!!, 0.01)
        assertEquals(4.03 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)
        assertEquals(21.0, v.field(FieldTypes.DATA_DATOS_DAYS)!!.numericValue!!, 0.01)
    }

    // ── Sample 5: diaria only, inactive ──

    @Test
    fun `diaria only inactive`() {
        val v = values("Tarifa: No activa. Diaria: 200 MB no activos.")
        assertEquals(0.0, v.field(FieldTypes.DATA_DIARIA_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(200.0, v.field(FieldTypes.DATA_DIARIA_MB)!!.numericValue!!, 0.01)
        assertNull(v.field(FieldTypes.DATA_DATOS_MB))
        assertNull(v.field(FieldTypes.DATA_TODUS_MB))
    }

    // ── Sample 6: diaria only, active ──

    @Test
    fun `diaria only active`() {
        val v = values("Tarifa: No activa. Diaria: 200 MB validos 24 horas.")
        assertEquals(1.0, v.field(FieldTypes.DATA_DIARIA_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(200.0, v.field(FieldTypes.DATA_DIARIA_MB)!!.numericValue!!, 0.01)
    }

    // ── Sample 7: datos GB active ──

    @Test
    fun `datos GB active 21 dias`() {
        val v = values("Tarifa: No activa. Datos: 4.03 GB validos 21 dias.")
        assertEquals(1.0, v.field(FieldTypes.DATA_DATOS_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(4.03 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)
        assertEquals(21.0, v.field(FieldTypes.DATA_DATOS_DAYS)!!.numericValue!!, 0.01)
    }

    // ── Sample 8: datos MB active (small) ──

    @Test
    fun `datos MB active small amount`() {
        val v = values("Tarifa: No activa. Datos: 57.35 MB validos 19 dias.")
        assertEquals(57.35, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.01)
        assertEquals("MB", v.field(FieldTypes.DATA_DATOS_MB)!!.unit)
        assertEquals(19.0, v.field(FieldTypes.DATA_DATOS_DAYS)!!.numericValue!!, 0.01)
    }

    // ── Sample 9: tarifa activa ──

    @Test
    fun `tarifa activa`() {
        val v = values("Tarifa: Activa. Datos: 57.35 MB validos 19 dias.")
        assertEquals(1.0, v.field(FieldTypes.DATA_TARIFF_ACTIVE)!!.numericValue!!, 0.01)
    }

    // ── Sample 10: no plan ──

    @Test
    fun `no plan - debe adquirir`() {
        val v = values("Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#")
        assertEquals(0.0, v.field(FieldTypes.DATA_TARIFF_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(0.0, v.field(FieldTypes.DATA_PLAN_ACTIVE)!!.numericValue!!, 0.01)
        assertEquals(2, v.size)
    }

    // ── Edge cases ──

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
    fun `ussdCodeId is data-plan`() {
        assertEquals("data-plan", parser.ussdCodeId)
    }

    @Test
    fun `parser is registered in UssdParsers`() {
        val registered = UssdParsers.forCode("data-plan")
        assertTrue(registered is DataPlanParser)
    }

    @Test
    fun `datos with comma decimal separator`() {
        val v = values("Tarifa: No activa. Datos: 4,03 GB validos 21 dias.")
        assertEquals(4.03 * 1024, v.field(FieldTypes.DATA_DATOS_MB)!!.numericValue!!, 0.1)
    }
}
