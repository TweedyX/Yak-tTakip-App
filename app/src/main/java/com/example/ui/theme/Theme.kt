package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CustomDarkColorScheme = darkColorScheme(
    primary = AccentColorDark,
    onPrimary = TextDark,
    secondary = GrayLight,
    onSecondary = TextDark,
    tertiary = AccentColorDark,
    onTertiary = TextDark,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray,
    outline = DarkBorder
)

private val CustomLightColorScheme = lightColorScheme(
    primary = AccentColorLight,
    onPrimary = TextWhite,
    secondary = GrayMedium,
    onSecondary = TextWhite,
    tertiary = AccentColorLight,
    onTertiary = TextWhite,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set default to false (Light Theme by default)
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
