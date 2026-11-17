package com.gamempires.messenger.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val DarkAppColors = darkColorScheme(
    primary = Color(0xFF2563EB), // Corporate Blue #2563EB
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A), // Blue 900
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color.White,
    background = Color(0xFF020617), // Neutral Slate-950 #020617
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A), // Slate-900 #0F172A
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B), // Slate-800
    onSurfaceVariant = Color(0xFF94A3B8), // Slate-400
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color.White,
    outline = Color(0xFF1E293B) // Slate-800 crisp border
)

internal val LightAppColors = lightColorScheme(
    primary = Color(0xFF2563EB), // Corporate Blue #2563EB
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF), // Blue 50
    onPrimaryContainer = Color(0xFF1D4ED8),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    background = Color(0xFFFAFAF9), // Neutral Stone-50 #FAFAF9
    onBackground = Color(0xFF0F172A), // Slate-900
    surface = Color(0xFFFFFFFF), // Pure White #FFFFFF
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9), // Slate-100
    onSurfaceVariant = Color(0xFF64748B), // Slate-500
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = Color(0xFF1D4ED8),
    outline = Color(0xFFE2E8F0) // Slate-200 border
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf(AppearanceMode.fromPreference(preferences.getString("mode", null))) }
    val dark = when (mode) {
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalAppearanceMode provides mode,
        LocalSetAppearance provides { selected ->
            mode = selected
            preferences.edit().putString("mode", selected.name).apply()
        }
    ) {
        MaterialTheme(colorScheme = if (dark) DarkAppColors else LightAppColors, typography = Typography, content = content)
    }
}
