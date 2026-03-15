package com.lockin.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * LockinColorScheme – Material 3 dark color scheme mapped to the Lockin palette.
 *
 * Mapping logic:
 * - primary → LockinPrimaryAccent (#00D9FF) – CTAs, active highlights
 * - onPrimary → LockinOnPrimary – text/icons on top of the primary accent
 * - secondary → LockinSecondaryAccent (#39FF14) – success states, progress
 * - onSecondary → LockinOnSecondary – text/icons on top of green accent
 * - background → LockinBackground (#121212) – deepest layer
 * - surface → LockinSurface (#1E1E1E) – cards, containers
 * - surfaceVariant → LockinSurfaceElevated (#252525) – dialogs, sheets
 * - onBackground / onSurface → LockinTextPrimary (#E0E0E0)
 * - error → LockinError (#CF6679)
 * - outline → LockinSubtleBorder – default divider/border color
 * - outlineVariant → LockinInputBorder
 */
private val LockinDarkColorScheme = darkColorScheme(
    primary = LockinPrimaryAccent,
    onPrimary = LockinOnPrimary,
    primaryContainer = LockinPrimaryAccent.copy(alpha = 0.15f),
    onPrimaryContainer = LockinPrimaryAccent,

    secondary = LockinSecondaryAccent,
    onSecondary = LockinOnSecondary,
    secondaryContainer = LockinSecondaryAccent.copy(alpha = 0.15f),
    onSecondaryContainer = LockinSecondaryAccent,

    tertiary = LockinPRGold,
    onTertiary = LockinBackground,
    tertiaryContainer = LockinPRGold.copy(alpha = 0.15f),
    onTertiaryContainer = LockinPRGold,

    background = LockinBackground,
    onBackground = LockinTextPrimary,

    surface = LockinSurface,
    onSurface = LockinTextPrimary,
    surfaceVariant = LockinSurfaceElevated,
    onSurfaceVariant = LockinTextSecondary,

    error = LockinError,
    onError = LockinBackground,
    errorContainer = LockinError.copy(alpha = 0.15f),
    onErrorContainer = LockinError,

    outline = LockinSubtleBorder,
    outlineVariant = LockinInputBorder,

    inverseSurface = LockinTextPrimary,
    inverseOnSurface = LockinBackground,
    inversePrimary = LockinPrimaryAccent,

    scrim = LockinBackground.copy(alpha = 0.8f)
)

/**
 * LockinTheme – The single entry-point for theming the entire app.
 *
 * This is always dark (no light theme toggle) to match the fitness aesthetic.
 * It also colors the system status bar to blend with the dark background.
 */
@Composable
fun LockinTheme(
    // We force dark; parameter kept for future flexibility
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = LockinDarkColorScheme

    // Color the system bars to match our dark background
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LockinTypography,
        content = content
    )
}

