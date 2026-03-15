package com.lockin.app.domain.repository

import com.lockin.app.domain.model.*

/**
 * Domain Repository Interfaces
 *
 * These define the contracts that the data layer must fulfill.
 * The domain layer only knows about these interfaces — never the concrete
 * implementations. This is the Dependency Inversion Principle in action:
 * high-level business logic depends on abstractions, not on Room DAOs
 * or network clients.
 *
 * Each interface groups related data operations for a single bounded context.
 */

/** Access and mutate the user's profile data. */
interface UserRepository {
    suspend fun getUserProfile(): UserProfile
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)
}

/** CRUD operations for workout plans and logged sessions. */
interface WorkoutRepository {
    suspend fun getTodayWorkoutPlan(): WorkoutPlan?
    suspend fun getWorkoutPlanById(id: String): WorkoutPlan?
    suspend fun getAllWorkoutPlans(): List<WorkoutPlan>
    suspend fun saveWorkoutPlan(plan: WorkoutPlan)
    suspend fun logWorkoutSession(session: WorkoutSession)
    suspend fun getRecentSessions(limit: Int = 10): List<WorkoutSession>
}

/** Browse and search the exercise library. */
interface ExerciseRepository {
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun searchExercises(query: String): List<Exercise>
    suspend fun getExercisesByMuscleGroup(group: MuscleGroup): List<Exercise>
}

/** Read and write daily habits. */
interface HabitRepository {
    suspend fun getHabitsForToday(): List<Habit>
    suspend fun toggleHabitCompletion(habitId: String)
    suspend fun addHabit(habit: Habit)
    suspend fun removeHabit(habitId: String)
}

/** Progress and historical data for charts and reviews. */
interface ProgressRepository {
    suspend fun getProgressEntries(fromDate: Long, toDate: Long): List<ProgressEntry>
    suspend fun getLatestProgressEntry(): ProgressEntry?
    suspend fun addProgressEntry(entry: ProgressEntry)
    suspend fun getPersonalRecords(): List<SetEntry>
}

/**
 * Health data from external sources (Health Connect, Samsung Health, sensors).
 * This bridges the domain layer to device-specific health APIs.
 */
interface HealthRepository {
    suspend fun getEnergyScore(): EnergyScore
    suspend fun getSleepData(fromDate: Long, toDate: Long): Any? // TODO: define SleepData model
    suspend fun getHeartRateData(fromDate: Long, toDate: Long): Any? // TODO: define HRData model
    suspend fun hasHealthPermissions(): Boolean
    suspend fun requestHealthPermissions(): Boolean
}

/** AI Coach conversation and response generation. */
interface AiCoachRepository {
    suspend fun sendMessage(message: String): CoachMessage
    suspend fun getConversationHistory(): List<CoachMessage>
    suspend fun clearConversation()
}

