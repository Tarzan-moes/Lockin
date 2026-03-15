package com.lockin.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient utilities for the Lockin design system.
 *
 * These reusable Brush factories and Modifiers implement the three gradient
 * combinations defined in the Lockin style guide:
 *
 * 1. Primary Gradient (#121212 → #1E1E1E) – screen backgrounds with depth
 * 2. Accent Glow (#00D9FF 100% → 0%) – timer glows, button highlights
 * 3. Progress Fill (#39FF14 30% → 0%) – chart fills, progress bars
 */

// ── Brush factories ─────────────────────────────────────────────────────────

/** Vertical gradient from deep background to card surface – gives depth to screens */
fun primaryGradientBrush(): Brush = Brush.verticalGradient(
    colors = listOf(LockinBackground, LockinSurface)
)

/**
 * Radial "glow" emanating from [center] using the primary cyan accent.
 * Use behind buttons/timers to create the neon halo effect.
 */
fun accentGlowBrush(
    center: Offset = Offset.Unspecified,
    radius: Float = 300f
): Brush = Brush.radialGradient(
    colors = listOf(
        LockinPrimaryAccent.copy(alpha = 0.45f),
        LockinPrimaryAccent.copy(alpha = 0.0f)
    ),
    center = center,
    radius = radius
)

/**
 * Horizontal linear gradient for progress bars and chart fills.
 * Starts at 30% opacity green and fades to transparent.
 */
fun progressFillBrush(): Brush = Brush.horizontalGradient(
    colors = listOf(
        LockinSecondaryAccent.copy(alpha = 0.30f),
        LockinSecondaryAccent.copy(alpha = 0.0f)
    )
)

// ── Modifier extensions ─────────────────────────────────────────────────────

/** Apply the primary depth gradient as a background */
fun Modifier.primaryGradientBackground(): Modifier =
    this.background(primaryGradientBrush())

/** Apply the cyan accent glow as a background (e.g., behind a CTA button) */
fun Modifier.accentGlowBackground(
    center: Offset = Offset.Unspecified,
    radius: Float = 300f
): Modifier = this.background(accentGlowBrush(center, radius))

/** Apply the green progress fill gradient as a background */
fun Modifier.progressFillBackground(): Modifier =
    this.background(progressFillBrush())

