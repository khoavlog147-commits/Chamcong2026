package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonBlue,
    secondary = DarkContainer,
    tertiary = AccentGreen,
    background = DarkBackground,
    surface = DarkContainer,
    onPrimary = White,
    onSecondary = LightGray,
    onTertiary = White,
    onBackground = LightGray,
    onSurface = LightGray,
    error = AccentRed
  )

private val LightColorScheme = DarkColorScheme // Force dark theme throughout the entire app

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode by default
  dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium branding intact
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Use DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
