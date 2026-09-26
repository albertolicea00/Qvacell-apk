package com.qvacell.app.parsing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qvacell.app.model.UssdActionType
import com.qvacell.app.model.UssdCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader

/**
 * Guards against catalog/registry drift: if app/src/main/assets/codes.json renames a dashboard
 * code id or the registry falls out of sync, this fails loudly instead of the field silently
 * staying "Unresolved" forever.
 */
@RunWith(RobolectricTestRunner::class)
class UssdParserCoverageTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `every known dashboard code id exists in codes json as a no-input ussd entry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val text = context.assets.open("codes.json").bufferedReader().use(BufferedReader::readText)
        val catalog = json.decodeFromString<UssdCatalog>(text)
        val noInputUssdIds = catalog.categories
            .flatMap { it.groups }
            .flatMap { it.codes }
            .filter { it.type == UssdActionType.USSD && !it.requiresInput }
            .map { it.id }

        UssdParsers.KNOWN_CODE_IDS.forEach { knownId ->
            assertTrue(
                "UssdParsers.KNOWN_CODE_IDS has '$knownId' but codes.json has no matching no-input USSD entry",
                noInputUssdIds.contains(knownId)
            )
        }
    }
}
