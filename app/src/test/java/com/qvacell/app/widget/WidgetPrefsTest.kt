package com.qvacell.app.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetPrefsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun usesDefaultsAndKeepsConfigurationPerWidget() {
        assertEquals("main-balance", WidgetPrefs.getCodeId(context, 71001))
        assertEquals(-1, WidgetPrefs.getSimSlot(context, 71001))

        WidgetPrefs.saveCodeId(context, 71001, "corp-balance")
        WidgetPrefs.saveSimSlot(context, 71001, 1)

        assertEquals("corp-balance", WidgetPrefs.getCodeId(context, 71001))
        assertEquals(1, WidgetPrefs.getSimSlot(context, 71001))
        assertEquals("main-balance", WidgetPrefs.getCodeId(context, 71002))
        assertEquals(-1, WidgetPrefs.getSimSlot(context, 71002))
    }

    @Test
    fun perWidgetStylesOverrideGlobalDefaultsAndCanBeCleared() {
        WidgetSettings.setIconShape(context, "circle")
        WidgetPrefs.saveIconShape(context, 71003, "square")
        WidgetPrefs.saveBackgroundColor(context, 71003, 0xFF123456.toInt())
        assertEquals("square", WidgetPrefs.getIconShape(context, 71003))
        assertEquals("circle", WidgetPrefs.getIconShape(context, 71004))
        assertEquals(0xFF123456.toInt(), WidgetPrefs.getBackgroundColor(context, 71003))

        WidgetPrefs.deleteConfig(context, 71003)

        assertEquals("main-balance", WidgetPrefs.getCodeId(context, 71003))
        assertEquals(-1, WidgetPrefs.getSimSlot(context, 71003))
        assertEquals("circle", WidgetPrefs.getIconShape(context, 71003))
        assertTrue(WidgetPrefs.getBackgroundColor(context, 71003) != 0xFF123456.toInt())
        assertNull(context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .getString("71003_code", null))
    }
}
