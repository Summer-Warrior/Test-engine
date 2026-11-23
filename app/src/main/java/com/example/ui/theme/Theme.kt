package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
  primary = CyberCyan,
  onPrimary = StudioDeepNavy,
  primaryContainer = StudioCardDark,
  onPrimaryContainer = CyberCyan,
  secondary = ElectricCrimson,
  onSecondary = StudioTextPrimary,
  secondaryContainer = StudioBorderDark,
  onSecondaryContainer = ElectricCrimson,
  tertiary = SuperFxGold,
  onTertiary = StudioDeepNavy,
  background = StudioDeepNavy,
  onBackground = StudioTextPrimary,
  surface = StudioSurfaceDark,
  onSurface = StudioTextPrimary,
  surfaceVariant = StudioCardDark,
  onSurfaceVariant = StudioTextSecondary,
  outline = StudioBorderDark,
  error = StudioErrorRed,
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StudioDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
