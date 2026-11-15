package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DomDeckDarkColorScheme = darkColorScheme(
    primary = DomDeckAccent,
    onPrimary = DomDeckOnAccent,
    primaryContainer = DomDeckAccentMuted,
    onPrimaryContainer = DomDeckAccent,
    secondary = DomDeckSurfaceElevated,
    onSecondary = DomDeckTextPrimary,
    secondaryContainer = DomDeckSurfaceElevated,
    onSecondaryContainer = DomDeckTextPrimary,
    tertiary = DomDeckAccent,
    onTertiary = DomDeckOnAccent,
    background = DomDeckBackground,
    onBackground = DomDeckTextPrimary,
    surface = DomDeckSurface,
    onSurface = DomDeckTextPrimary,
    surfaceVariant = DomDeckSurfaceElevated,
    onSurfaceVariant = DomDeckTextSecondary,
    surfaceContainerLowest = DomDeckBackground,
    surfaceContainerLow = DomDeckSurfaceSubtle,
    surfaceContainer = DomDeckSurface,
    surfaceContainerHigh = DomDeckSurfaceElevated,
    surfaceContainerHighest = DomDeckSurfaceElevated,
    outline = DomDeckBorder,
    outlineVariant = DomDeckBorder,
    error = DomDeckError,
    onError = DomDeckBackground,
    errorContainer = DomDeckErrorContainer,
    onErrorContainer = DomDeckError
)

private val DomDeckShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DomDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DomDeckDarkColorScheme,
        typography = DomDeckTypography,
        shapes = DomDeckShapes,
        content = content
    )
}
