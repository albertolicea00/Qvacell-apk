package com.qvacell.app.ui.theme

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.qvacell.app.R
import com.qvacell.app.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class LaunchThemeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun darkAppOnLightSystemUsesDarkLaunchBackground() {
        val lightSystem = withNightMode(Configuration.UI_MODE_NIGHT_NO)
        val launch = launchThemeContext(lightSystem, ThemeMode.DARK)
        assertEquals(0xFF1C1C1E.toInt(), launch.getColor(R.color.launch_background))
        syncLaunchTheme(context, ThemeMode.DARK)
        assertEquals(UiModeManager.MODE_NIGHT_YES, applicationNightMode())
    }

    @Test
    fun lightAppOnDarkSystemUsesLightLaunchBackground() {
        val darkSystem = withNightMode(Configuration.UI_MODE_NIGHT_YES)
        val launch = launchThemeContext(darkSystem, ThemeMode.LIGHT)
        assertEquals(0xFFEDEEF1.toInt(), launch.getColor(R.color.launch_background))
        syncLaunchTheme(context, ThemeMode.LIGHT)
        assertEquals(UiModeManager.MODE_NIGHT_NO, applicationNightMode())
    }

    @Test
    fun returningToSystemClearsForcedNightMode() {
        syncLaunchTheme(context, ThemeMode.DARK)
        syncLaunchTheme(context, ThemeMode.SYSTEM)
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, applicationNightMode())
        for (systemNight in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val system = withNightMode(systemNight)
            assertEquals(system.getColor(R.color.launch_background),
                launchThemeContext(system, ThemeMode.SYSTEM).getColor(R.color.launch_background))
        }
    }

    private fun applicationNightMode() =
        shadowOf(context.getSystemService(UiModeManager::class.java)).applicationNightMode

    private fun withNightMode(nightMode: Int): Context {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        return context.createConfigurationContext(config)
    }
}
