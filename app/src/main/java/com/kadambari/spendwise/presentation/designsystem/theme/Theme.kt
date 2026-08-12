package com.kadambari.spendwise.presentation.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryText,
    secondary = SecondaryBlue,
    onSecondary = White,
    secondaryContainer = SecondaryBlueContainer,
    onSecondaryContainer = PrimaryText,
    background = Background,
    onBackground = PrimaryText,
    surface = Surface,
    onSurface = PrimaryText,
    surfaceVariant = SecondaryBlueContainer,
    onSurfaceVariant = DarkGray,
    outline = LightGray,
    outlineVariant = LightGray,
    error = Error,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueContainer,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = White,
    secondary = SecondaryBlueContainer,
    onSecondary = DarkBackground,
    secondaryContainer = SecondaryBlue,
    onSecondaryContainer = White,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = SecondaryBlue,
    onSurfaceVariant = LightGray,
    outline = DarkGray,
    outlineVariant = DarkGray,
    error = Error,
    onError = White
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpendWiseTypography,
        shapes = SpendWiseShapes,
        content = content
    )
}
