package com.example.codemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightLiquidGlassColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF001A33),
    secondary = Color(0xFF8A4DE6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDDCFF),
    onSecondaryContainer = Color(0xFF2A0054),
    tertiary = Color(0xFF00B894),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA8F5E4),
    onTertiaryContainer = Color(0xFF00201A),
    background = Color(0xFFF8FBFF),
    onBackground = Color(0xFF001F3A),
    surface = Color(0xFFF8FBFF),
    onSurface = Color(0xFF001F3A),
    surfaceVariant = Color(0xFFE6F0FF),
    onSurfaceVariant = Color(0xFF3E4A59),
    outline = Color(0xFF6F7A8A),
    outlineVariant = Color(0xFFC4CAD5),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF003257),
    inverseOnSurface = Color(0xFFE6F2FF),
    inversePrimary = Color(0xFFA8C8FF),
    surfaceDim = Color(0xFFD8E2EC),
    surfaceBright = Color(0xFFF8FBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5FF),
    surfaceContainer = Color(0xFFECF0FA),
    surfaceContainerHigh = Color(0xFFE6EAF4),
    surfaceContainerHighest = Color(0xFFE0E4EE)
)

private val DarkLiquidGlassColorScheme = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFFD6BAFF),
    onSecondary = Color(0xFF42008C),
    secondaryContainer = Color(0xFF5D22B9),
    onSecondaryContainer = Color(0xFFEDDCFF),
    tertiary = Color(0xFF8CDECF),
    onTertiary = Color(0xFF00382E),
    tertiaryContainer = Color(0xFF005144),
    onTertiaryContainer = Color(0xFFA8F5E4),
    background = Color(0xFF0F151C),
    onBackground = Color(0xFFD6E3F0),
    surface = Color(0xFF0F151C),
    onSurface = Color(0xFFD6E3F0),
    surfaceVariant = Color(0xFF1F2830),
    onSurfaceVariant = Color(0xFFC4CAD5),
    outline = Color(0xFF8E949F),
    outlineVariant = Color(0xFF3E4A59),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFD6E3F0),
    inverseOnSurface = Color(0xFF002A42),
    inversePrimary = Color(0xFF0066CC),
    surfaceDim = Color(0xFF0F151C),
    surfaceBright = Color(0xFF353B43),
    surfaceContainerLowest = Color(0xFF0A1017),
    surfaceContainerLow = Color(0xFF171D24),
    surfaceContainer = Color(0xFF1B2128),
    surfaceContainerHigh = Color(0xFF252B32),
    surfaceContainerHighest = Color(0xFF30363D)
)

@Composable
fun CodeManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado para mantener colores líquidos personalizados
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkLiquidGlassColorScheme
        else -> LightLiquidGlassColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiquidGlassTypography,
        content = content
    )
}