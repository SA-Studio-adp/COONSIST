package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ConsistDarkColorScheme = darkColorScheme(
    primary = ConsistCyan,
    onPrimary = ConsistBlack,
    primaryContainer = ConsistCyanContainer,
    onPrimaryContainer = ConsistCyan,
    secondary = ConsistCyanDim,
    onSecondary = ConsistBlack,
    secondaryContainer = ConsistCharcoalElevated,
    onSecondaryContainer = ConsistWhite,
    tertiary = ConsistWhite,
    onTertiary = ConsistBlack,
    background = ConsistBlack,
    onBackground = ConsistWhite,
    surface = ConsistCharcoalDark,
    onSurface = ConsistWhite,
    surfaceVariant = ConsistCharcoalCard,
    onSurfaceVariant = ConsistGreyLight,
    surfaceTint = ConsistCyan,
    outline = ConsistBorder,
    outlineVariant = ConsistCharcoalElevated,
    error = ConsistError,
    onError = ConsistWhite
)

@Composable
fun ConsistTheme(
    content: @Composable () -> Unit
) {
    // We enforce CONSIST's signature Black & Cyan theme regardless of system dynamic color
    MaterialTheme(
        colorScheme = ConsistDarkColorScheme,
        typography = Typography,
        content = content
    )
}
