package com.qvacell.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.data.ThemeMode
import com.qvacell.app.ui.navigation.QvacellNavHost
import com.qvacell.app.ui.theme.QvacellTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: reminders/caller-ID notifications simply won't show if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val settings = SettingsDataStore(applicationContext)

        setContent {
            // DataStore reads are async, so collectAsStateWithLifecycle's `initialValue` briefly
            // renders BEFORE the real persisted value loads — a visible flash of the default
            // theme/tab that then swaps to the user's actual choice a frame or two later. Loading
            // the real values once with a one-shot `.first()` before rendering anything closes
            // that gap; the window background (or system splash) covers this brief wait instead.
            var initialThemeMode by remember { mutableStateOf<ThemeMode?>(null) }
            var initialAccentColor by remember { mutableStateOf<String?>(null) }
            var initialDefaultTab by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                initialThemeMode = settings.themeMode.first()
                initialAccentColor = settings.accentColor.first()
                initialDefaultTab = settings.defaultTab.first()
            }

            val loadedThemeMode = initialThemeMode
            val loadedAccentColor = initialAccentColor
            val loadedDefaultTab = initialDefaultTab
            if (loadedThemeMode != null && loadedAccentColor != null && loadedDefaultTab != null) {
                QvacellApp(
                    settings = settings,
                    initialThemeMode = loadedThemeMode,
                    initialAccentColor = loadedAccentColor,
                    startTabRoute = loadedDefaultTab,
                    onNavigationBarColor = { window.navigationBarColor = it }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * Theme mode and accent color still collect live (Ajustes changes them and the whole app should
 * react instantly), but `startTabRoute` is a plain snapshot, passed once — [QvacellNavHost] never
 * receives a changed value that could yank the user out of wherever they navigated to.
 */
@Composable
private fun QvacellApp(
    settings: SettingsDataStore,
    initialThemeMode: ThemeMode,
    initialAccentColor: String,
    startTabRoute: String,
    onNavigationBarColor: (Int) -> Unit
) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = initialThemeMode)
    val accentColor by settings.accentColor.collectAsStateWithLifecycle(initialValue = initialAccentColor)

    QvacellTheme(themeMode = themeMode, accentColorHex = accentColor) {
        // enableEdgeToEdge() already makes the system navigation bar transparent on modern
        // Android, letting our own background show through — but pre-API-29 devices draw a scrim
        // over that transparency instead of a true match. Setting this explicitly (like WhatsApp
        // does) guarantees the 3-button/gesture bar is always the exact same color as our own
        // bottom bar, on every Android version.
        val navigationBarColor = MaterialTheme.colorScheme.surface
        SideEffect { onNavigationBarColor(navigationBarColor.toArgb()) }
        Surface(modifier = Modifier.fillMaxSize()) {
            QvacellNavHost(startTabRoute = startTabRoute)
        }
    }
}
