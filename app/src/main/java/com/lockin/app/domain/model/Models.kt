package com.lockin.app.domain.model

/**
 * Domain models for the Lockin app.
 *
 * These are pure Kotlin data classes with no framework dependencies (no Room
 * annotations, no Compose). They represent the core business concepts and are
 * used by use cases, repositories, and mapped to/from data-layer entities.
 *
 * Keeping them framework-free follows Clean Architecture: the domain layer
 * has zero dependencies on Android, Room, or Compose.
 */

// ── User ─────────────────────────────────────────────────────────────────────

/** The user's profile, gathered during onboarding and editable in Settings. */
data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val primaryGoal: FitnessGoal = FitnessGoal.GENERAL_FITNESS,
    val availableEquipment: List<Equipment> = emptyList(),
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val birthYear: Int? = null,
    val onboardingCompleted: Boolean = false
)

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED, ELITE }

enum class FitnessGoal {
    STRENGTH, HYPERTROPHY, ENDURANCE, WEIGHT_LOSS, GENERAL_FITNESS, ATHLETIC_PERFORMANCE
}

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, PULL_UP_BAR, RESISTANCE_BANDS,
    CABLE_MACHINE, SMITH_MACHINE, BODYWEIGHT_ONLY, FULL_GYM
}

// ── Workout ──────────────────────────────────────────────────────────────────

/** A planned workout (template) – what the user is supposed to do today. */
data class WorkoutPlan(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val exercises: List<PlannedExercise> = emptyList(),
    val estimatedDurationMinutes: Int = 0,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val tags: List<String> = emptyList()
)

/** A single exercise slot inside a WorkoutPlan with target sets/reps. */
data class PlannedExercise(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val targetSets: Int = 0,
    val targetReps: IntRange = 0..0,
    val targetRpe: Float? = null,
    val restSeconds: Int = 60,
    val notes: String = ""
)

enum class Difficulty { EASY, MODERATE, HARD, BRUTAL }

// ── Exercise Library ─────────────────────────────────────────────────────────

/** An exercise in the library (e.g., "Barbell Back Squat"). */
data class Exercise(
    val id: String = "",
    val name: String = "",
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.BODYWEIGHT_ONLY,
    val instructions: String = "",
    val videoUrl: String? = null,
    val imageUrl: String? = null
)

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES, ABS, FULL_BODY
}

// ── Logging (what the user actually did) ─────────────────────────────────────

/** A completed workout session with all logged sets. */
data class WorkoutSession(
    val id: String = "",
    val workoutPlanId: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val setEntries: List<SetEntry> = emptyList(),
    val notes: String = ""
)

/** A single logged set – the atomic unit of training data. */
data class SetEntry(
    val id: String = "",
    val exerciseId: String = "",
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val rpe: Float? = null,
    val isPersonalRecord: Boolean = false,
    val completedAt: Long = 0L
)

// ── Habits ───────────────────────────────────────────────────────────────────

/** A daily habit the user tracks (e.g., "Drink 3L water", "8h sleep"). */
data class Habit(
    val id: String = "",
    val name: String = "",
    val iconEmoji: String = "✅",
    val isCompletedToday: Boolean = false,
    val streak: Int = 0
)

// ── Progress & Health ────────────────────────────────────────────────────────

/** A snapshot of progress metrics on a given date. */
data class ProgressEntry(
    val date: Long = 0L,
    val bodyWeightKg: Float? = null,
    val totalVolumeKg: Float = 0f,
    val personalRecords: Int = 0,
    val workoutsCompleted: Int = 0
)

/**
 * Energy Score – A 0-100 composite metric derived from sleep, HRV,
 * recovery, and recent training load. Displayed prominently on the Home screen.
 */
data class EnergyScore(
    val score: Int = 0,
    val label: String = "Unknown",
    val sleepQualityPercent: Int = 0,
    val hrvScore: Int = 0,
    val recoveryPercent: Int = 0
)

// ── AI Coach ─────────────────────────────────────────────────────────────────

/** A single message in the AI Coach conversation. */
data class CoachMessage(
    val id: String = "",
    val content: String = "",
    val isFromUser: Boolean = false,
    val timestamp: Long = 0L
)

