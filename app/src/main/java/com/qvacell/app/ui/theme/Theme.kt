package com.qvacell.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

/** Builds a full color scheme from a single user-picked accent color (Ajustes › Color de acento). */
private fun accentColorScheme(accent: Color, dark: Boolean): ColorScheme {
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = lerp(accent, Color.Black, 0.55f),
            onPrimaryContainer = lerp(accent, Color.White, 0.85f),
            secondaryContainer = lerp(accent, Color.Black, 0.7f),
            onSecondaryContainer = lerp(accent, Color.White, 0.85f)
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.White, 0.8f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.75f),
            secondaryContainer = lerp(accent, Color.White, 0.85f),
            onSecondaryContainer = lerp(accent, Color.Black, 0.7f)
        )
    }
}

@Composable
fun QvacellTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Hex string from Ajustes › Color de acento (e.g. "#0099CC"); null/unparseable falls back to
    // the hardcoded brand cyan scheme above.
    accentColorHex: String? = null,
    // Off by default so the accent color above is what actually renders — Material You's dynamic
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
    val accent = accentColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        accent != null -> accentColorScheme(accent, useDark)
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
