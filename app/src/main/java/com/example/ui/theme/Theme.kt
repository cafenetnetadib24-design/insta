package com.example.ui.theme

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

private val SleekLightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    secondary = SleekSecondary,
    onSecondary = Color.White,
    tertiary = SleekDarkBtn,
    background = SleekBackground,
    onBackground = TextPrimary,
    surface = SleekSurface,
    onSurface = TextPrimary,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

private val SleekDarkColorScheme = darkColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    secondary = SleekSecondary,
    onSecondary = Color.White,
    tertiary = SleekDarkBtn,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun VideoDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SleekDarkColorScheme else SleekLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
