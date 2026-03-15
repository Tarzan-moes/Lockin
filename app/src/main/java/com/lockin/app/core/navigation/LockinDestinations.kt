package com.lockin.app.core.navigation

/**
 * LockinDestinations – Sealed hierarchy defining every screen in the app.
 *
 * Using a sealed class lets the compiler enforce exhaustive when-expressions,
 * so we can never forget to handle a route. Routes are grouped into:
 *
 * 1. Onboarding – First-time user flow (welcome → profile → permissions → tutorial)
 * 2. Main – Daily-use screens accessible from the bottom navigation bar
 * 3. Detail / Deep-link – Screens reached by drilling into main sections
 *
 * Each destination carries a [route] string used by Navigation Compose's NavHost.
 */
sealed class LockinDestinations(val route: String) {

    // ── Onboarding Flow ──────────────────────────────────────────────────
    /** Welcome splash – app branding and "Get Started" CTA */
    data object Welcome : LockinDestinations("onboarding/welcome")
    /** Profile setup – goals, experience level, available equipment */
    data object ProfileSetup : LockinDestinations("onboarding/profile_setup")
    /** Health permissions – request Health Connect & sensor access */
    data object HealthPermissions : LockinDestinations("onboarding/health_permissions")
    /** Optional watch pairing – connect Galaxy Watch / Wear OS */
    data object WatchPairing : LockinDestinations("onboarding/watch_pairing")
    /** AI Coach intro – explain what the AI can do */
    data object AiCoachIntro : LockinDestinations("onboarding/ai_coach_intro")
    /** First workout generation – AI creates the user's first plan */
    data object FirstWorkout : LockinDestinations("onboarding/first_workout")
    /** Quick tutorial on logging sets and completing workouts */
    data object LoggingTutorial : LockinDestinations("onboarding/logging_tutorial")

    // ── Main Screens (Bottom Nav) ────────────────────────────────────────
    /** Dashboard – Energy Score, AI suggestions, today's workout entry */
    data object Home : LockinDestinations("main/home")
    /** Workout execution – active workout with set logging */
    data object Workout : LockinDestinations("main/workout")
    /** Exercise library – browse / search exercises */
    data object ExerciseLibrary : LockinDestinations("main/exercise_library")
    /** AI Coach chat – conversational coaching interface */
    data object Coach : LockinDestinations("main/coach")
    /** Progress – charts, PRs, weekly/monthly reviews */
    data object Progress : LockinDestinations("main/progress")
    /** Habits – daily habit checklist */
    data object Habits : LockinDestinations("main/habits")
    /** Calendar – workout scheduler */
    data object Calendar : LockinDestinations("main/calendar")
    /** Settings – preferences, units, account, integrations */
    data object Settings : LockinDestinations("main/settings")

    // ── Detail / Deep-link Screens ───────────────────────────────────────
    /** Workout summary shown after completing a session */
    data object WorkoutSummary : LockinDestinations("detail/workout_summary")
    /** Create / edit a workout plan */
    data object WorkoutEditor : LockinDestinations("detail/workout_editor/{workoutId}") {
        fun createRoute(workoutId: String = "new") = "detail/workout_editor/$workoutId"
    }
    /** Active workout session – log sets */
    data object WorkoutSession : LockinDestinations("detail/workout_session/{workoutId}") {
        fun createRoute(workoutId: String) = "detail/workout_session/$workoutId"
    }
    /** Pick exercises to add to a workout */
    data object ExercisePicker : LockinDestinations("detail/exercise_picker/{workoutId}") {
        fun createRoute(workoutId: String) = "detail/exercise_picker/$workoutId"
    }
    /** Deload suggestion review */
    data object DeloadReview : LockinDestinations("detail/deload_review")
    /** Health analysis – deeper dive into health data trends */
    data object HealthAnalysis : LockinDestinations("detail/health_analysis")
    /** Achievements gallery */
    data object Achievements : LockinDestinations("detail/achievements")
    /** Health debug – quick Health Connect steps test */
    data object HealthDebug : LockinDestinations("detail/health_debug")
    /** Workout tracking debug – start/stop service */
    data object WorkoutTrackingDebug : LockinDestinations("detail/workout_tracking_debug")
    /** Health connect diagnostic */
    data object HealthConnectDiag : LockinDestinations("detail/health_connect_diag")
    /** Exercise detail – shows GIF, instructions, tips */
    data object ExerciseDetail : LockinDestinations("detail/exercise/{exerciseId}") {
        fun createRoute(exerciseId: String) = "detail/exercise/$exerciseId"
    }
}
