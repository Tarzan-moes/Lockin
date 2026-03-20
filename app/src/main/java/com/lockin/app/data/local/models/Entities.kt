package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity classes – the database-layer representations of domain models.
 *
 * These are annotated with @Entity so Room can generate SQL tables for them.
 * They mirror the domain models but are allowed to have Room-specific
 * annotations. Mapping functions (toEntity / toDomain) will be added later.
 */

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val description: String = "",
    val goal: String = "",
    val estimatedDurationMinutes: Int = 0,
    val difficulty: String = "MODERATE",
    val type: String = "",
    val caloriesBurned: Int = 0
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val muscleGroup: String = "",
    val equipment: String = "",
    val instructions: String = ""
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val iconEmoji: String = "✅",
    val isCompletedToday: Boolean = false,
    val streak: Int = 0
)

