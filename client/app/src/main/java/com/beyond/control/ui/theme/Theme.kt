package com.beyond.control.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = FreshGreen,
    onPrimary = Color.White,
    primaryContainer = MintGreen,
    onPrimaryContainer = TextPrimary,
    secondary = FreshBlue,
    onSecondary = Color.White,
    secondaryContainer = SoftSky,
    onSecondaryContainer = TextPrimary,
    tertiary = FreshPink,
    onTertiary = Color.White,
    tertiaryContainer = SoftPeach,
    onTertiaryContainer = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Lavender,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFEF5350),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FreshGreen,
    onPrimary = Color.White,
    primaryContainer = MintGreen.copy(alpha = 0.3f),
    secondary = FreshBlue,
    onSecondary = Color.White,
    tertiary = FreshPink,
    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF16213E),
    onSurface = Color(0xFFE0E0E0),
)

@Composable
fun RemoteAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor -> {
            val context = LocalContext.current
            if (context is Activity) {
                dynamicLightColorScheme(context)
            } else {
                LightColorScheme
            }
        }
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
