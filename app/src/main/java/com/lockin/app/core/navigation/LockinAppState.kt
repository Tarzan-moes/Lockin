package com.lockin.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * LockinAppState – Single source of truth for top-level app UI state.
 *
 * Holds the [navController] and exposes convenience helpers for
 * checking the current route, deciding which bottom-nav items are visible, etc.
 *
 * This pattern is recommended by the official Compose navigation docs:
 * it keeps the Activity/Composable layer thin and testable.
 */
@Stable
class LockinAppState(
    val navController: NavHostController
) {
    /** The route string of the currently displayed destination (nullable on first frame). */
    val currentRoute: String?
        get() = navController.currentBackStackEntry?.destination?.route

    /** The five destinations shown in the bottom navigation bar. */
    val bottomNavDestinations = listOf(
        LockinDestinations.Home,
        LockinDestinations.Workout,
        LockinDestinations.Coach,
        LockinDestinations.Progress,
        LockinDestinations.Settings
    )

    /** Navigate to a bottom-nav destination, reusing the existing back stack entry. */
    fun navigateToBottomNavDestination(destination: LockinDestinations) {
        navController.navigate(destination.route) {
            // Pop up to the start destination to avoid building up a large stack
            popUpTo(LockinDestinations.Home.route) {
                inclusive = (destination == LockinDestinations.Home)
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }
}

/** Remember a [LockinAppState] scoped to the composable lifecycle. */
@Composable
fun rememberLockinAppState(
    navController: NavHostController = rememberNavController()
): LockinAppState = remember(navController) {
    LockinAppState(navController)
}

