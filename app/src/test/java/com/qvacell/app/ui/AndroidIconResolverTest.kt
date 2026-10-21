package com.qvacell.app.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.io.File

class AndroidIconResolverTest {
    @Test
    fun `every icon in codes json resolves without fallback`() {
        val codes = Json.parseToJsonElement(File("src/main/assets/codes.json").readText())
        val icons = mutableSetOf<String>()

        fun collect(element: JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (key, value) ->
                    if (key == "icon" && value is JsonPrimitive) icons += value.content
                    collect(value)
                }
                is JsonArray -> element.forEach(::collect)
                else -> Unit
            }
        }

        collect(codes)
        val fallback = resolveAndroidIcon(null)
        icons.forEach { icon ->
            assertNotSame("Icon $icon fell back to Circle", fallback, resolveAndroidIcon(icon))
        }
    }
}
