package com.lockin.app.data.repository

import com.lockin.app.domain.model.*
import com.lockin.app.domain.repository.*
import com.lockin.app.domain.usecase.CalculateEnergyScoreUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub Repository Implementations
 *
 * These fulfill the domain repository contracts with fake/TODO data.
 * They exist so Hilt can satisfy the dependency graph and the app compiles.
 * Real implementations backed by Room, Health Connect, etc. come in Part 2+.
 */

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {
    private var profile = UserProfile()

    override suspend fun getUserProfile(): UserProfile = profile
    override suspend fun saveUserProfile(profile: UserProfile) { this.profile = profile }
    override suspend fun isOnboardingCompleted(): Boolean = profile.onboardingCompleted
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        profile = profile.copy(onboardingCompleted = completed)
    }
}

@Singleton
class WorkoutRepositoryImpl @Inject constructor() : WorkoutRepository {
    override suspend fun getTodayWorkoutPlan(): WorkoutPlan = WorkoutPlan(
        id = "demo_1",
        name = "Push Day – Chest & Shoulders",
        description = "Hypertrophy-focused push session",
        estimatedDurationMinutes = 55,
        difficulty = Difficulty.MODERATE,
        exercises = listOf(
            PlannedExercise(exerciseId = "1", exerciseName = "Bench Press", targetSets = 4, targetReps = 8..10),
            PlannedExercise(exerciseId = "2", exerciseName = "OHP", targetSets = 3, targetReps = 8..12),
            PlannedExercise(exerciseId = "3", exerciseName = "Incline DB Press", targetSets = 3, targetReps = 10..12),
            PlannedExercise(exerciseId = "4", exerciseName = "Lateral Raises", targetSets = 4, targetReps = 12..15)
        )
    )

    override suspend fun getWorkoutPlanById(id: String): WorkoutPlan? = getTodayWorkoutPlan()
    override suspend fun getAllWorkoutPlans(): List<WorkoutPlan> = listOf(getTodayWorkoutPlan())
    override suspend fun saveWorkoutPlan(plan: WorkoutPlan) { /* TODO */ }
    override suspend fun logWorkoutSession(session: WorkoutSession) { /* TODO */ }
    override suspend fun getRecentSessions(limit: Int): List<WorkoutSession> = emptyList()
}

@Singleton
class ExerciseRepositoryImpl @Inject constructor() : ExerciseRepository {
    override suspend fun getAllExercises(): List<Exercise> = emptyList()
    override suspend fun getExerciseById(id: String): Exercise? = null
    override suspend fun searchExercises(query: String): List<Exercise> = emptyList()
    override suspend fun getExercisesByMuscleGroup(group: MuscleGroup): List<Exercise> = emptyList()
}

@Singleton
class HabitRepositoryImpl @Inject constructor() : HabitRepository {
    private val habits = mutableListOf(
        Habit(id = "1", name = "Drink 3L Water", iconEmoji = "💧", streak = 5),
        Habit(id = "2", name = "8 Hours Sleep", iconEmoji = "😴", streak = 3),
        Habit(id = "3", name = "10K Steps", iconEmoji = "🚶", streak = 12),
        Habit(id = "4", name = "Stretch 10 min", iconEmoji = "🧘", streak = 2)
    )

    override suspend fun getHabitsForToday(): List<Habit> = habits.toList()
    override suspend fun toggleHabitCompletion(habitId: String) {
        val index = habits.indexOfFirst { it.id == habitId }
        if (index != -1) {
            habits[index] = habits[index].copy(isCompletedToday = !habits[index].isCompletedToday)
        }
    }
    override suspend fun addHabit(habit: Habit) { habits.add(habit) }
    override suspend fun removeHabit(habitId: String) { habits.removeAll { it.id == habitId } }
}

@Singleton
class ProgressRepositoryImpl @Inject constructor() : ProgressRepository {
    override suspend fun getProgressEntries(fromDate: Long, toDate: Long): List<ProgressEntry> = emptyList()
    override suspend fun getLatestProgressEntry(): ProgressEntry? = null
    override suspend fun addProgressEntry(entry: ProgressEntry) { /* TODO */ }
    override suspend fun getPersonalRecords(): List<SetEntry> = emptyList()
}

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val calculateEnergyScore: CalculateEnergyScoreUseCase
) : HealthRepository {
    override suspend fun getEnergyScore(): EnergyScore {
        return try {
            calculateEnergyScore.calculate(userId = "default")
        } catch (_: Exception) {
            // Fallback if Health Connect is unavailable
            EnergyScore(
                score = 78,
                label = "Good",
                sleepQualityPercent = 82,
                hrvScore = 65,
                recoveryPercent = 85
            )
        }
    }
    override suspend fun getSleepData(fromDate: Long, toDate: Long): Any? = null
    override suspend fun getHeartRateData(fromDate: Long, toDate: Long): Any? = null
    override suspend fun hasHealthPermissions(): Boolean = false
    override suspend fun requestHealthPermissions(): Boolean = false
}

@Singleton
class AiCoachRepositoryImpl @Inject constructor() : AiCoachRepository {
    override suspend fun sendMessage(message: String): CoachMessage = CoachMessage(
        id = "stub",
        content = "AI Coach response will appear here once the inference engine is connected.",
        isFromUser = false,
        timestamp = System.currentTimeMillis()
    )
    override suspend fun getConversationHistory(): List<CoachMessage> = emptyList()
    override suspend fun clearConversation() { /* TODO */ }
}

