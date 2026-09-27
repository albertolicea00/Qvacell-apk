package com.qvacell.app.parsing

import com.qvacell.app.model.UssdActionType
import com.qvacell.app.model.UssdCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards against catalog/registry drift: if app/src/main/assets/codes.json renames a dashboard
 * code id or the registry falls out of sync, this fails loudly instead of the field silently
 * staying "Unresolved" forever.
 *
 * Reads codes.json straight off disk rather than through Android's AssetManager/Context — this
 * test only needs the JSON content, not real Android asset-resolution behavior, and Robolectric's
 * resource/asset shadow layer doesn't reliably support this project's compileSdk 37 yet. A plain
 * file read sidesteps that entirely and needs no Robolectric runtime.
 */
class UssdParserCoverageTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `every known dashboard code id exists in codes json as a no-input ussd entry`() {
        // Gradle's unit-test working directory is the module dir (`app/`).
        val text = File("src/main/assets/codes.json").readText()
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
