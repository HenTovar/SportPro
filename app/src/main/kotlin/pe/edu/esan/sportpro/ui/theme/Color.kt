package pe.edu.esan.sportpro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Paleta de colores verde de fútbol
val PrimaryGreen = Color(0xFF1B5E20)
val PrimaryGreenLight = Color(0xFF2E7D32)
val PrimaryGreenDark = Color(0xFF0D3B1A)
val SecondaryGreen = Color(0xFF4CAF50)
val TertiaryGreen = Color(0xFF81C784)
val AccentWhite = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF5F5F5)
val BackgroundDark = Color(0xFF121212)
val ErrorRed = Color(0xFFB3261E)
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF1F1F1F)

/**
 * Esquema de colores claro para SportPro.
 * Basado en una paleta verde de fútbol.
 */
val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = AccentWhite,
    primaryContainer = TertiaryGreen,
    onPrimaryContainer = PrimaryGreenDark,
    secondary = SecondaryGreen,
    onSecondary = AccentWhite,
    secondaryContainer = TertiaryGreen,
    onSecondaryContainer = PrimaryGreenDark,
    tertiary = TertiaryGreen,
    onTertiary = PrimaryGreenDark,
    tertiaryContainer = TertiaryGreen,
    onTertiaryContainer = PrimaryGreenDark,
    error = ErrorRed,
    onError = AccentWhite,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = ErrorRed,
    background = BackgroundLight,
    onBackground = Color(0xFF1C1C1C),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF79747E),
)

/**
 * Esquema de colores oscuro para SportPro.
 * Basado en una paleta verde de fútbol.
 */
val DarkColorScheme = darkColorScheme(
    primary = TertiaryGreen,
    onPrimary = PrimaryGreenDark,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = AccentWhite,
    secondary = SecondaryGreen,
    onSecondary = PrimaryGreenDark,
    secondaryContainer = PrimaryGreenLight,
    onSecondaryContainer = AccentWhite,
    tertiary = TertiaryGreen,
    onTertiary = PrimaryGreenDark,
    tertiaryContainer = PrimaryGreenLight,
    onTertiaryContainer = AccentWhite,
    error = Color(0xFFF2B8B5),
    onError = ErrorRed,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF2B8B5),
    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC7D0),
    outline = Color(0xFF938F99),
)
