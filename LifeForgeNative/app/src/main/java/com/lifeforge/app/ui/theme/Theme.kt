package com.lifeforge.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Theme mode enum
enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

// Composition local for accessing theme mode
val LocalThemeMode = compositionLocalOf { ThemeMode.DARK }

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = White,
    primaryContainer = AccentDark,
    onPrimaryContainer = White,
    
    secondary = AccentGlow,
    onSecondary = Black,
    secondaryContainer = Surface,
    onSecondaryContainer = TextPrimary,
    
    tertiary = Success,
    onTertiary = White,
    
    background = Primary,
    onBackground = TextPrimary,
    
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHighlight,
    onSurfaceVariant = TextSecondary,
    
    error = Alert,
    onError = White,
    
    outline = BorderGlow,
    outlineVariant = Surface
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = White,
    primaryContainer = AccentGlow,
    onPrimaryContainer = Black,
    
    secondary = AccentDark,
    onSecondary = White,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = TextPrimaryLight,
    
    tertiary = Success,
    onTertiary = White,
    
    background = PrimaryLight,
    onBackground = TextPrimaryLight,
    
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceHighlightLight,
    onSurfaceVariant = TextSecondaryLight,
    
    error = Alert,
    onError = White,
    
    outline = BorderGlow,
    outlineVariant = SurfaceLight
)

// Gradient Brush for Backgrounds
@Composable
fun getAppBackgroundBrush(isDark: Boolean = isSystemInDarkTheme()): androidx.compose.ui.graphics.Brush {
    return if (isDark) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(PrimaryGradientStart, PrimaryGradientEnd)
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(PrimaryLightGradientStart, PrimaryLightGradientEnd)
        )
    }
}

@Composable
fun StoppyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    val statusBarColor = if (isDarkTheme) Primary else PrimaryLight
    val isLightBars = !isDarkTheme
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor.toArgb()
            window.navigationBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightBars
        }
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

