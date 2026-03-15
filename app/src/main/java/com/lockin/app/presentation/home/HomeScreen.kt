package com.lockin.app.presentation.home

import androidx.compose.runtime.Composable

/**
 * HomeScreen – delegates entirely to TodayScreen (the daily dashboard).
 *
 * Navigation callbacks are forwarded so TodayScreen can trigger
 * route changes via the NavHost without knowing about it directly.
 */
@Composable
fun HomeScreen(
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onRequestHealthPermissions: () -> Unit = {}
) {
    TodayScreen(
        onRequestHealthPermissions = onRequestHealthPermissions,
        onNavigateToWorkout = onNavigateToWorkout,
        onNavigateToCoach = onNavigateToCoach,
        onNavigateToProgress = onNavigateToProgress
    )
}
