package com.example.dataguard.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentCyan,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = PrimaryBlue,
    secondary = AccentCyan,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder
)

@Composable
fun DataGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our sleek custom palette for distinctive security feel
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
