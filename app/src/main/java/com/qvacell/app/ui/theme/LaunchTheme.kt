package com.qvacell.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.qvacell.app.data.ThemeMode

/** Persist the app's choice in Android too: the system paints the splash before our code runs. */
fun syncLaunchTheme(context: Context, mode: ThemeMode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                // For application night mode AUTO clears the package override, following system.
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            }
        )
    }
}

/** Resolve launch resources using the app choice, including on Android versions before 12. */
fun launchThemeContext(context: Context, mode: ThemeMode): Context {
    val nightMode = when (mode) {
        ThemeMode.DARK -> Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> Configuration.UI_MODE_NIGHT_NO
        ThemeMode.SYSTEM -> return context
    }
    val configuration = Configuration(context.resources.configuration)
    configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
    return context.createConfigurationContext(configuration)
}
