package com.qvacell.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CubanPhoneNumberTest {
    @Test
    fun normalizesLocalAndCountryPrefixedMobileNumbers() {
        assertEquals("51234567", CubanPhoneNumber.normalize("51234567"))
        assertEquals("61234567", CubanPhoneNumber.normalize("+53 6123-4567"))
    }

    @Test
    fun rejectsLandlinesWrongLengthsAndUnsupportedPrefixes() {
        listOf("71234567", "21234567", "1234567", "512345678", "5212345678", "abc").forEach {
            assertNull("Should reject $it", CubanPhoneNumber.normalize(it))
        }
    }
}
