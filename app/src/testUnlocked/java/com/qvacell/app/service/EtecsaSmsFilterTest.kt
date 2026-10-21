package com.qvacell.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtecsaSmsFilterTest {
    @Test
    fun acceptsShortCodeMessagesWithKnownEtecsaTermsCaseInsensitively() {
        assertTrue(EtecsaSmsFilter.isEtecsaMessage("2266", "Su saldo es 25 CUP"))
        assertTrue(EtecsaSmsFilter.isEtecsaMessage("12345", "Mensaje de ETECSA"))
    }

    @Test
    fun rejectsLongOrNonNumericSendersAndUnrelatedBodies() {
        assertFalse(EtecsaSmsFilter.isEtecsaMessage("ETECSA", "Saldo disponible"))
        assertFalse(EtecsaSmsFilter.isEtecsaMessage("1234567", "ETECSA"))
        assertFalse(EtecsaSmsFilter.isEtecsaMessage("2266", "Su paquete vence mañana"))
        assertFalse(EtecsaSmsFilter.isEtecsaMessage("12A4", "Saldo"))
    }
}
