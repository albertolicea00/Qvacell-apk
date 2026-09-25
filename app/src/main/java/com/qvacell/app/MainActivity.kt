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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.ui.navigation.QvacellNavHost
import com.qvacell.app.ui.theme.QvacellTheme
import com.qvacell.app.data.ThemeMode

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: reminders/caller-ID notifications simply won't show if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val settings = SettingsDataStore(applicationContext)

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val accentColor by settings.accentColor.collectAsStateWithLifecycle(
                initialValue = SettingsDataStore.DEFAULT_ACCENT_COLOR
            )
            val defaultTab by settings.defaultTab.collectAsStateWithLifecycle(initialValue = "home")

            QvacellTheme(themeMode = themeMode, accentColorHex = accentColor) {
                // enableEdgeToEdge() already makes the system navigation bar transparent on
                // modern Android, letting our own background show through — but pre-API-29
                // devices draw a scrim over that transparency instead of a true match. Setting
                // this explicitly (like WhatsApp does) guarantees the 3-button/gesture bar is
                // always the exact same color as our own bottom bar, on every Android version.
                val navigationBarColor = MaterialTheme.colorScheme.surface
                SideEffect {
                    window.navigationBarColor = navigationBarColor.toArgb()
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    QvacellNavHost(startTabRoute = defaultTab)
                }
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
