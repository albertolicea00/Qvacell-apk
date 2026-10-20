package com.qvacell.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {
    private val settings = SettingsDataStore(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun simSwitchActionDefaultsToSheetAndPersistsValidChoices() = runBlocking {
        settings.setSimSwitchAction("sheet")
        assertEquals("sheet", settings.simSwitchAction.first())

        settings.setSimSwitchAction("cycle")
        assertEquals("cycle", settings.simSwitchAction.first())

        settings.setSimSwitchAction("menu")
        assertEquals("menu", settings.simSwitchAction.first())
    }

    @Test
    fun simSwitchActionRejectsUnknownValuesWithoutChangingPreference() = runBlocking {
        settings.setSimSwitchAction("cycle")
        val error = runCatching { settings.setSimSwitchAction("unknown") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("cycle", settings.simSwitchAction.first())
    }
}
