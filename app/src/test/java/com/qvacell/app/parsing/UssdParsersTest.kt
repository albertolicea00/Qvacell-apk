package com.qvacell.app.parsing

import org.junit.Assert.assertTrue
import org.junit.Test

class UssdParsersTest {

    @Test
    fun `every known code id has a registered parser`() {
        UssdParsers.KNOWN_CODE_IDS.forEach { id ->
            val parser = UssdParsers.forCode(id)
            assertTrue(
                "expected a real parser for '$id', got ${parser::class.simpleName}",
                parser !is StubUssdResponseParser
            )
        }
    }

    @Test
    fun `forCode on an unknown id still returns a safe stub, never throws`() {
        val result = UssdParsers.forCode("not-a-real-code").parse("texto")
        assertTrue(result is ParseResult.Unresolved)
    }

    @Test
    fun `real parsers return Unrecognized for gibberish, not crash`() {
        UssdParsers.KNOWN_CODE_IDS.forEach { id ->
            val result = UssdParsers.forCode(id).parse("cualquier texto de respuesta USSD")
            assertTrue(
                "expected Unrecognized for '$id', got $result",
                result is ParseResult.Unrecognized
            )
        }
    }
}
