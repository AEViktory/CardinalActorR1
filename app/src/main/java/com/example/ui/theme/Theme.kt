package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CardinalDarkColorScheme = darkColorScheme(
    primary = CardinalScarlet,
    onPrimary = Color.White,
    primaryContainer = CardinalCrimsonDark,
    onPrimaryContainer = Color(0xFFFFDADE),
    secondary = CardinalGlow,
    onSecondary = Color.White,
    secondaryContainer = ObsidianSurfaceHover,
    onSecondaryContainer = Color(0xFFFFD8DE),
    tertiary = CardinalAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF4A3400),
    onTertiaryContainer = Color(0xFFFFDFA0),
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianSurfaceHover
)

private val CardinalLightColorScheme = lightColorScheme(
    primary = CardinalCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADE),
    onPrimaryContainer = CardinalCrimsonDark,
    secondary = CardinalScarlet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECEF),
    onSecondaryContainer = CardinalCrimsonDark,
    tertiary = CardinalAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF1D6),
    onTertiaryContainer = Color(0xFF7A4E00),
    background = CardinalLightBg,
    onBackground = Color(0xFF0F172A),
    surface = CardinalLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = CardinalLightBorder
)

@Composable
fun AetherealCardinalTheme(
    darkTheme: Boolean = true, // Default to obsidian dark for Aethereal Cardinal aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CardinalDarkColorScheme else CardinalLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AetherealCardinalTheme(darkTheme = darkTheme, content = content)
}
