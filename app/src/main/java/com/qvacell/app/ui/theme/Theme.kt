package com.qvacell.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.qvacell.app.data.ThemeMode

// Matches the logo mark and iOS's `Color.brandCyan` (rgb(0,153,204)) — the app's actual
// brand color, not Material You's per-device dynamic color.
private val QvacellCyan = Color(0xFF0099CC)
private val QvacellCyanDark = Color(0xFF5BC8E8)

// `lightColorScheme(primary = ...)`/`darkColorScheme(primary = ...)` only override `primary` —
// every other role (primaryContainer, secondaryContainer, etc.) silently falls back to Material's
// default violet baseline, which is why price/subscription chips (colorScheme.primaryContainer /
// secondaryContainer in CodeRow) rendered purple instead of on-brand. Derive those explicitly too.
private val LightColors = lightColorScheme(
    primary = QvacellCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E6F5),
    onPrimaryContainer = Color(0xFF002A38),
    secondaryContainer = Color(0xFFD6EFF7),
    onSecondaryContainer = Color(0xFF10404D)
)
private val DarkColors = darkColorScheme(
    primary = QvacellCyanDark,
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004D66),
    onPrimaryContainer = Color(0xFFB8E6F5),
    secondaryContainer = Color(0xFF29424C),
    onSecondaryContainer = Color(0xFFD6EFF7)
)

@Composable
fun QvacellTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Off by default so the brand cyan above is what actually renders — Material You's dynamic
    // color would otherwise silently replace it with a per-device/wallpaper color on API 31+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
