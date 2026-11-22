package com.vitor.blockapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta própria do BlockApp: índigo/violeta. Sem cores dinâmicas, para manter a identidade.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B9BFF),
    onPrimary = Color(0xFF1B1766),
    primaryContainer = Color(0xFF2B2870),
    onPrimaryContainer = Color(0xFFE2E1FF),
    secondary = Color(0xFFC3C2DD),
    background = Color(0xFF0E0E16),
    onBackground = Color(0xFFE6E4F0),
    surface = Color(0xFF0E0E16),
    onSurface = Color(0xFFE6E4F0),
    surfaceVariant = Color(0xFF23232F),
    onSurfaceVariant = Color(0xFFA9A8BA),
    surfaceContainer = Color(0xFF181822),
    surfaceContainerHigh = Color(0xFF20202C),
    outline = Color(0xFF3A3A4A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2FF),
    onPrimaryContainer = Color(0xFF16135C),
    secondary = Color(0xFF5C5D72),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE9E6F2),
    onSurfaceVariant = Color(0xFF5E5D6B),
    surfaceContainer = Color(0xFFF3F0FA),
    surfaceContainerHigh = Color(0xFFEDEAF5),
)

@Composable
fun BlockAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
