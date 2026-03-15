package com.lockin.app.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lockin.app.core.navigation.LockinAppState
import com.lockin.app.core.navigation.LockinDestinations
import com.lockin.app.core.navigation.LockinNavGraph
import com.lockin.app.core.navigation.rememberLockinAppState
import com.lockin.app.ui.theme.*

/**
 * LockinApp – The root composable that wraps the navigation graph
 * with a Scaffold containing a bottom navigation bar.
 *
 * The bottom nav is only visible on main screens (not during onboarding
 * or detail screens). It uses the Lockin color system for a consistent look.
 */
@Composable
fun LockinApp(
    appState: LockinAppState = rememberLockinAppState()
) {
    val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav on main screens
    val showBottomBar = currentRoute?.startsWith("main/") == true

    Scaffold(
        containerColor = LockinBackground,
        bottomBar = {
            if (showBottomBar) {
                LockinBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        appState.navigateToBottomNavDestination(destination)
                    }
                )
            }
        }
    ) { innerPadding ->
        LockinNavGraph(
            navController = appState.navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Bottom navigation bar items – maps destinations to icons and labels.
 */
private data class BottomNavItem(
    val destination: LockinDestinations,
    val icon: ImageVector,
    val label: String
)

private val bottomNavItems = listOf(
    BottomNavItem(LockinDestinations.Home, Icons.Default.Home, "Home"),
    BottomNavItem(LockinDestinations.Workout, Icons.Default.FitnessCenter, "Workout"),
    BottomNavItem(LockinDestinations.Coach, Icons.Default.Psychology, "Coach"),
    BottomNavItem(LockinDestinations.Progress, Icons.AutoMirrored.Filled.TrendingUp, "Progress"),
    BottomNavItem(LockinDestinations.Settings, Icons.Default.Settings, "Settings")
)

@Composable
private fun LockinBottomBar(
    currentRoute: String?,
    onNavigate: (LockinDestinations) -> Unit
) {
    NavigationBar(
        containerColor = LockinSurface,
        contentColor = LockinTextPrimary
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.destination.route

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LockinPrimaryAccent,
                    selectedTextColor = LockinPrimaryAccent,
                    unselectedIconColor = LockinTextDisabled,
                    unselectedTextColor = LockinTextDisabled,
                    indicatorColor = LockinPrimaryAccent.copy(alpha = 0.12f)
                )
            )
        }
    }
}

