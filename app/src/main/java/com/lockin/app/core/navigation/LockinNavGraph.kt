package com.lockin.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.presentation.coach.CoachScreen
import com.lockin.app.presentation.exercise.ExerciseDetailScreen
import com.lockin.app.presentation.exercise.ExerciseLibraryScreen
import com.lockin.app.presentation.habits.HabitsScreen
import com.lockin.app.presentation.home.HomeScreen
import com.lockin.app.presentation.onboarding.*
import com.lockin.app.presentation.permissions.HealthPermissionsScreen
import com.lockin.app.presentation.permissions.HealthPermissionsViewModel
import com.lockin.app.presentation.progress.ProgressScreen
import com.lockin.app.presentation.settings.SettingsScreen
import com.lockin.app.presentation.workout.WorkoutHomeScreen
import com.lockin.app.presentation.workout.WorkoutEditorScreen
import com.lockin.app.presentation.workout.WorkoutSessionScreen
import com.lockin.app.presentation.workout.WorkoutSummaryScreen
import com.lockin.app.presentation.workout.ExercisePickerScreen
import com.lockin.app.presentation.debug.HealthDebugScreen
import com.lockin.app.presentation.debug.HealthDebugViewModel
import com.lockin.app.presentation.debug.WorkoutTrackingDebugScreen
import com.lockin.app.presentation.debug.WorkoutTrackingDebugViewModel
import com.lockin.app.presentation.debug.HealthConnectDiagScreen
import com.lockin.app.presentation.debug.HealthConnectDiagViewModel
import com.lockin.app.presentation.calendar.CalendarScreen

/**
 * LockinNavGraph – The central navigation graph for the app.
 *
 * Compose Navigation uses a NavHost with string routes to move between screens.
 * We reference our [LockinDestinations] sealed objects for type-safe routing.
 *
 * The [startDestination] defaults to Home (daily flow). In a real scenario,
 * the app checks a "first-run" flag in DataStore and redirects to Welcome if needed.
 *
 * @param navController  The NavHostController that drives navigation.
 * @param startDestination  The route to show first.
 * @param modifier  Optional modifier applied to the NavHost.
 */
@Composable
fun LockinNavGraph(
    navController: NavHostController,
    startDestination: String = LockinDestinations.Home.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ── Onboarding ───────────────────────────────────────────────────
        composable(LockinDestinations.Welcome.route) {
            WelcomeScreen(onContinue = {
                navController.navigate(LockinDestinations.ProfileSetup.route)
            })
        }
        composable(LockinDestinations.ProfileSetup.route) {
            ProfileSetupScreen(onContinue = {
                navController.navigate(LockinDestinations.HealthPermissions.route)
            })
        }
        composable(LockinDestinations.HealthPermissions.route) {
            val viewModel: HealthPermissionsViewModel = hiltViewModel()
            HealthPermissionsScreen(
                healthConnectManager = viewModel.healthConnectManager,
                onPermissionsGranted = {
                    navController.navigate(LockinDestinations.WatchPairing.route)
                }
            )
        }
        composable(LockinDestinations.WatchPairing.route) {
            WatchPairingScreen(onContinue = {
                navController.navigate(LockinDestinations.AiCoachIntro.route)
            })
        }
        composable(LockinDestinations.AiCoachIntro.route) {
            AiCoachIntroScreen(onContinue = {
                navController.navigate(LockinDestinations.FirstWorkout.route)
            })
        }
        composable(LockinDestinations.FirstWorkout.route) {
            FirstWorkoutScreen(onContinue = {
                navController.navigate(LockinDestinations.LoggingTutorial.route)
            })
        }
        composable(LockinDestinations.LoggingTutorial.route) {
            LoggingTutorialScreen(onFinish = {
                // After onboarding, navigate to the main Home and clear the back stack
                navController.navigate(LockinDestinations.Home.route) {
                    popUpTo(LockinDestinations.Welcome.route) { inclusive = true }
                }
            })
        }

        // ── Main Screens ─────────────────────────────────────────────────
        composable(LockinDestinations.Home.route) {
            HomeScreen(
                onNavigateToWorkout = {
                    navController.navigate(LockinDestinations.Workout.route)
                },
                onNavigateToCoach = {
                    navController.navigate(LockinDestinations.Coach.route)
                },
                onNavigateToProgress = {
                    navController.navigate(LockinDestinations.Progress.route)
                },
                onRequestHealthPermissions = {
                    navController.navigate(LockinDestinations.HealthPermissions.route)
                }
            )
        }
        composable(LockinDestinations.Workout.route) {
            WorkoutHomeScreen(
                onCreateWorkout = { workoutId ->
                    navController.navigate(LockinDestinations.WorkoutEditor.createRoute(workoutId))
                },
                onOpenWorkout = { workoutId ->
                    navController.navigate(LockinDestinations.WorkoutEditor.createRoute(workoutId))
                },
                onStartWorkout = { workoutId ->
                    navController.navigate(LockinDestinations.WorkoutSession.createRoute(workoutId))
                }
            )
        }
        composable(LockinDestinations.ExerciseLibrary.route) {
            ExerciseLibraryScreen()
        }
        composable(LockinDestinations.Coach.route) {
            CoachScreen()
        }
        composable(LockinDestinations.Progress.route) {
            ProgressScreen()
        }
        composable(LockinDestinations.Habits.route) {
            HabitsScreen()
        }
        composable(LockinDestinations.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() }, // Assuming it might be accessed from somewhere reachable back, or it's a main screen
                onStartWorkout = { workoutId ->
                    navController.navigate(LockinDestinations.WorkoutSession.createRoute(workoutId))
                }
            )
        }
        composable(LockinDestinations.Settings.route) {
            SettingsScreen(
                onNavigateToHealthPermissions = {
                    navController.navigate(LockinDestinations.HealthPermissions.route)
                },
                onNavigateToHealthDebug = {
                    navController.navigate(LockinDestinations.HealthDebug.route)
                },
                onNavigateToWatchDebug = {
                    navController.navigate(LockinDestinations.WorkoutTrackingDebug.route)
                },
                onNavigateToHealthDiag = {
                    navController.navigate(LockinDestinations.HealthConnectDiag.route)
                }
            )
        }

        // ── Detail / Deep-link ───────────────────────────────────────────
        composable(
            route = LockinDestinations.WorkoutEditor.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) {
            WorkoutEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddExercise = {
                    val workoutId = it.arguments?.getString("workoutId") ?: "new"
                    navController.navigate(LockinDestinations.ExercisePicker.createRoute(workoutId))
                },
                onStartWorkout = { workoutId ->
                    navController.navigate(LockinDestinations.WorkoutSession.createRoute(workoutId)) {
                        popUpTo(LockinDestinations.Workout.route)
                    }
                }
            )
        }
        composable(
            route = LockinDestinations.ExercisePicker.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) {
            ExercisePickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onExerciseClick = { exerciseId ->
                    navController.navigate(LockinDestinations.ExerciseDetail.createRoute(exerciseId))
                }
            )
        }
        composable(
            route = LockinDestinations.WorkoutSession.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) {
            WorkoutSessionScreen(
                onWorkoutCompleted = {
                    navController.navigate(LockinDestinations.WorkoutSummary.route) {
                        popUpTo(LockinDestinations.Workout.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(LockinDestinations.WorkoutSummary.route) {
            WorkoutSummaryScreen(
                onNavigateHome = {
                    navController.navigate(LockinDestinations.Home.route) {
                        popUpTo(LockinDestinations.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(LockinDestinations.HealthDebug.route) {
            val viewModel: HealthDebugViewModel = hiltViewModel()
            HealthDebugScreen(
                viewModel = viewModel,
                onRequestPermissions = {
                    navController.navigate(LockinDestinations.HealthPermissions.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(LockinDestinations.WorkoutTrackingDebug.route) {
            val viewModel: WorkoutTrackingDebugViewModel = hiltViewModel()
            WorkoutTrackingDebugScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(LockinDestinations.HealthConnectDiag.route) {
            val viewModel: HealthConnectDiagViewModel = hiltViewModel()
            HealthConnectDiagScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = LockinDestinations.ExerciseDetail.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) {
            ExerciseDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
