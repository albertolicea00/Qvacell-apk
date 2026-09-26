package com.qvacell.app.parsing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParsersTest {

    @Test
    fun `stub matches always false pre-rules`() {
        SmsParsers.all().forEach { parser ->
            assertFalse(parser.matches("cualquier mensaje de saldo ETECSA", "12345"))
        }
    }

    @Test
    fun `stub never crashes on empty body or non-ETECSA sender`() {
        SmsParsers.all().forEach { parser ->
            assertFalse(parser.matches("", ""))
            val result = parser.parse("")
            assertTrue(result is ParseResult.Unresolved)
        }
    }

    @Test
    fun `registry has one parser per known signal id, no duplicates`() {
        val ids = SmsParsers.all().map { it.smsSignalId }
        assertTrue(ids.toSet().size == ids.size)
        assertTrue(ids.containsAll(SmsParsers.KNOWN_SIGNAL_IDS))
    }
}
