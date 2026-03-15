package com.lockin.app.domain.usecase

import com.lockin.app.domain.model.*
import com.lockin.app.domain.repository.*
import javax.inject.Inject

/**
 * Domain Use Cases
 *
 * Each use case encapsulates a single piece of business logic.
 * They follow the "operator fun invoke()" convention so callers can
 * use them like functions: `val data = getEnergyScore()`.
 *
 * Use cases are injected into ViewModels via Hilt. They depend only on
 * domain repository interfaces — never on data-layer implementations.
 */

// ── Dashboard / Home ─────────────────────────────────────────────────────────

/** Fetches the composite Energy Score displayed on the Home dashboard. */
class GetEnergyScoreUseCase @Inject constructor(
    private val healthRepository: HealthRepository
) {
    suspend operator fun invoke(): EnergyScore {
        // TODO: add business rules (e.g., clamp score, apply modifiers)
        return healthRepository.getEnergyScore()
    }
}

/** Fetches today's workout plan so the Home screen can show a summary. */
class GetTodayWorkoutPlanUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(): WorkoutPlan? {
        return workoutRepository.getTodayWorkoutPlan()
    }
}

/** Bundles the data needed for the daily dashboard in one call. */
data class DashboardData(
    val energyScore: EnergyScore,
    val todayWorkout: WorkoutPlan?,
    val habits: List<Habit>,
    val recentPRs: List<SetEntry>
)

class GetDailyDashboardDataUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val workoutRepository: WorkoutRepository,
    private val habitRepository: HabitRepository,
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(): DashboardData {
        // TODO: run these in parallel with coroutines for performance
        return DashboardData(
            energyScore = healthRepository.getEnergyScore(),
            todayWorkout = workoutRepository.getTodayWorkoutPlan(),
            habits = habitRepository.getHabitsForToday(),
            recentPRs = progressRepository.getPersonalRecords().take(5)
        )
    }
}

// ── Habits ───────────────────────────────────────────────────────────────────

/** Toggles a habit's completion state for today. */
class CompleteHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) {
        habitRepository.toggleHabitCompletion(habitId)
    }
}

// ── Workout ──────────────────────────────────────────────────────────────────

/** Logs a completed workout session to the repository. */
class LogWorkoutSessionUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(session: WorkoutSession) {
        // TODO: validate session data, calculate volume, detect PRs
        workoutRepository.logWorkoutSession(session)
    }
}

// ── Progress ─────────────────────────────────────────────────────────────────

/** Retrieves progress entries within a date range for charting. */
class GetProgressDataUseCase @Inject constructor(
    private val progressRepository: ProgressRepository
) {
    suspend operator fun invoke(fromDate: Long, toDate: Long): List<ProgressEntry> {
        return progressRepository.getProgressEntries(fromDate, toDate)
    }
}

