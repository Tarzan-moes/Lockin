package com.lockin.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Lockin Color System
 *
 * A dark-first color palette designed for a fitness app.
 * Colors are organized into functional groups:
 * - Primary: Dark backgrounds with neon accents for energy and focus
 * - Text: High-contrast text hierarchy for readability on dark surfaces
 * - Functional: Semantic colors for success, warning, error, and achievements
 * - Border: Subtle separators that maintain the dark aesthetic
 */

// ── Primary Colors (Dark UI, Neon Accents) ──────────────────────────────────
/** App background – the deepest layer of the UI */
val LockinBackground = Color(0xFF0D0D0F)
/** Cards, elevated containers – slightly lifted from background */
val LockinSurface = Color(0xFF18181B)
/** Modals, bottom sheets, dialogs – highest elevation surface */
val LockinSurfaceElevated = Color(0xFF222226)
/** Primary accent – CTAs, active states, timers, highlights (electric cyan) */
val LockinPrimaryAccent = Color(0xFF00D4FF)
/** Secondary accent – success, PRs, completed sets, progress (neon green) */
val LockinSecondaryAccent = Color(0xFF34D399)

// ── Text Colors ─────────────────────────────────────────────────────────────
/** Headlines, important data, exercise names */
val LockinTextPrimary = Color(0xFFF0F0F3)
/** Descriptions, labels, hints */
val LockinTextSecondary = Color(0xFF9CA3AF)
/** Inactive elements, placeholder text */
val LockinTextDisabled = Color(0xFF52525B)

// ── Functional Colors ───────────────────────────────────────────────────────
/** Completed sets, goals achieved */
val LockinSuccess = Color(0xFF34D399)
/** Deload reminders, form warnings */
val LockinWarning = Color(0xFFFBBF24)
/** Failed sets, input errors */
val LockinError = Color(0xFFF87171)
/** Personal records, achievements – gold highlight */
val LockinPRGold = Color(0xFFFFD700)

// ── Border & Divider Colors ─────────────────────────────────────────────────
/** Card borders, dividers, subtle separators */
val LockinSubtleBorder = Color(0xFF27272A)
/** Text field borders, inactive toggles */
val LockinInputBorder = Color(0xFF3F3F46)
/** Active input fields, selected items – matches primary accent */
val LockinFocusBorder = Color(0xFF00D4FF)

// ── Gradient Anchors ────────────────────────────────────────────────────────
// These are used as start/end points in Brush.linearGradient / radialGradient.
// The composable utilities for these live in the theme's GradientUtils.

/** On-primary text color – dark text on bright primary accent */
val LockinOnPrimary = Color(0xFF001F26)
/** On-secondary text color – dark text on green accent */
val LockinOnSecondary = Color(0xFF002E1B)
