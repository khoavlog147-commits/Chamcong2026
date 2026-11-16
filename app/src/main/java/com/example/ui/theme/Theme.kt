package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    secondary = AccentGreen,
    onSecondary = TextSecondary,
    tertiary = NightPurple,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkContainer,
    onSurface = TextPrimary,
    error = AccentRed,
    onError = White
)

@Composable
fun TimeSnapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
