package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.VaultThemePreset

val VaultShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

private fun buildDarkScheme(
  primary: Color,
  onPrimary: Color,
  secondary: Color,
): ColorScheme =
  darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primary.copy(alpha = 0.16f),
    onPrimaryContainer = primary,
    secondary = secondary,
    onSecondary = onPrimary,
    secondaryContainer = InkDarkSurfaceVar,
    onSecondaryContainer = InkOnDark,
    tertiary = VaultGold,
    background = InkDarkBg,
    onBackground = InkOnDark,
    surface = InkDarkSurface,
    onSurface = InkOnDark,
    surfaceVariant = InkDarkSurfaceVar,
    onSurfaceVariant = InkOnDarkMuted,
    outline = InkDarkOutline,
    outlineVariant = InkDarkOutline.copy(alpha = 0.55f),
    error = VaultError,
  )

private fun buildLightScheme(
  primary: Color,
  onPrimary: Color,
  secondary: Color,
): ColorScheme =
  lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primary.copy(alpha = 0.12f),
    onPrimaryContainer = primary,
    secondary = secondary,
    onSecondary = onPrimary,
    secondaryContainer = ChalkLightSurfaceVar,
    onSecondaryContainer = InkOnLight,
    tertiary = AmberPrimaryLight,
    background = ChalkLightBg,
    onBackground = InkOnLight,
    surface = ChalkLightSurface,
    onSurface = InkOnLight,
    surfaceVariant = ChalkLightSurfaceVar,
    onSurfaceVariant = InkOnLightMuted,
    outline = ChalkLightOutline,
    outlineVariant = ChalkLightOutline.copy(alpha = 0.65f),
    error = VaultError,
  )

@Composable
fun VaultPlayTheme(
  darkTheme: Boolean = true,
  themePreset: VaultThemePreset = VaultThemePreset.CYBER_MINT,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when (themePreset) {
      VaultThemePreset.OBSIDIAN_MINIMAL ->
        if (darkTheme) {
          buildDarkScheme(ObsidianPrimaryDark, ObsidianOnPrimaryDark, Color(0xFF94A3B8))
        } else {
          buildLightScheme(ObsidianPrimaryLight, ObsidianOnPrimaryLight, Color(0xFF475569))
        }
      VaultThemePreset.CYBER_MINT ->
        if (darkTheme) {
          buildDarkScheme(MintPrimaryDark, MintOnPrimaryDark, MintSecondaryDark)
        } else {
          buildLightScheme(MintPrimaryLight, MintOnPrimaryLight, MintSecondaryLight)
        }
      VaultThemePreset.SOLAR_AMBER ->
        if (darkTheme) {
          buildDarkScheme(AmberPrimaryDark, AmberOnPrimaryDark, AmberSecondaryDark)
        } else {
          buildLightScheme(AmberPrimaryLight, AmberOnPrimaryLight, AmberSecondaryLight)
        }
      VaultThemePreset.COBALT_PULSE ->
        if (darkTheme) {
          buildDarkScheme(CobaltPrimaryDark, CobaltOnPrimaryDark, CobaltSecondaryDark)
        } else {
          buildLightScheme(CobaltPrimaryLight, CobaltOnPrimaryLight, CobaltSecondaryLight)
        }
      VaultThemePreset.CRIMSON_VELVET ->
        if (darkTheme) {
          buildDarkScheme(CrimsonPrimaryDark, CrimsonOnPrimaryDark, CrimsonSecondaryDark)
        } else {
          buildLightScheme(CrimsonPrimaryLight, CrimsonOnPrimaryLight, CrimsonSecondaryLight)
        }
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = VaultShapes,
    content = content,
  )
}
