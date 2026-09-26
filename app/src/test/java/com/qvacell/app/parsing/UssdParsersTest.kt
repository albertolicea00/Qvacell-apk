package com.qvacell.app.parsing

import org.junit.Assert.assertTrue
import org.junit.Test

class UssdParsersTest {

    @Test
    fun `stub parser returns Unresolved for arbitrary input across all known code ids`() {
        UssdParsers.KNOWN_CODE_IDS.forEach { id ->
            val result = UssdParsers.forCode(id).parse("cualquier texto de respuesta USSD")
            assertTrue("expected Unresolved for '$id', got $result", result is ParseResult.Unresolved)
        }
    }

    @Test
    fun `forCode on an unknown id still returns a safe stub, never throws`() {
        val result = UssdParsers.forCode("not-a-real-code").parse("texto")
        assertTrue(result is ParseResult.Unresolved)
    }

    @Test
    fun `stub tolerates empty input`() {
        val result = UssdParsers.forCode("main-balance").parse("")
        assertTrue(result is ParseResult.Unresolved)
    }
}
