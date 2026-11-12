package com.mobile360.healthcare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.onPrimary,
    primaryContainer = AppColors.primaryContainer,
    onPrimaryContainer = AppColors.onPrimaryContainer,
    inversePrimary = AppColors.inversePrimary,
    secondary = AppColors.secondary,
    onSecondary = AppColors.onSecondary,
    secondaryContainer = AppColors.secondaryContainer,
    onSecondaryContainer = AppColors.onSecondaryContainer,
    tertiary = AppColors.tertiary,
    onTertiary = AppColors.onTertiary,
    tertiaryContainer = AppColors.tertiaryContainer,
    onTertiaryContainer = AppColors.onTertiaryContainer,
    background = AppColors.background,
    onBackground = AppColors.onBackground,
    surface = AppColors.surface,
    onSurface = AppColors.onSurface,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.onSurfaceVariant,
    surfaceTint = AppColors.surfaceTint,
    inverseSurface = AppColors.inverseSurface,
    inverseOnSurface = AppColors.inverseOnSurface,
    error = AppColors.error,
    onError = AppColors.onError,
    errorContainer = AppColors.errorContainer,
    onErrorContainer = AppColors.onErrorContainer,
    outline = AppColors.outline,
    outlineVariant = AppColors.outlineVariant,
    surfaceBright = AppColors.surfaceBright,
    surfaceDim = AppColors.surfaceDim,
    surfaceContainer = AppColors.surfaceContainer,
    surfaceContainerHigh = AppColors.surfaceContainerHigh,
    surfaceContainerHighest = AppColors.surfaceContainerHighest,
    surfaceContainerLow = AppColors.surfaceContainerLow,
    surfaceContainerLowest = AppColors.surfaceContainerLowest
)

@Composable
fun CareConnectTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) CareConnectDarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// CR-002: dark colour scheme
private val CareConnectDarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF00296E),
    primaryContainer = Color(0xFF1D3E8F),
    onPrimaryContainer = Color(0xFFDAE2FF),
    secondary = Color(0xFFB9C7E8),
    onSecondary = Color(0xFF233048),
    secondaryContainer = Color(0xFF394760),
    onSecondaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF121317),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121317),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E909A),
    outlineVariant = Color(0xFF44474F),
    surfaceContainer = Color(0xFF1F2024),
    surfaceContainerLow = Color(0xFF1A1B20),
    surfaceContainerLowest = Color(0xFF0D0E12),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34353A)
)

/** CR-007: applies the chosen accent over the current theme; "system" uses the dynamic palette on Android 12+. */
@Composable
fun AccentTheme(accent: String, darkTheme: Boolean, content: @Composable () -> Unit) {
    val choice = AccentChoice.fromId(accent)
    val base = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scheme = when {
        choice == AccentChoice.System && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        choice.lightPrimary == null -> base
        darkTheme -> base.copy(
            primary = choice.darkPrimary!!,
            onPrimary = Color.Black,
            primaryContainer = choice.darkContainer!!,
            onPrimaryContainer = Color.White,
        )
        else -> base.copy(
            primary = choice.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = choice.lightContainer!!,
            onPrimaryContainer = Color.Black,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
