package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int = 0,
    val plannedSets: Int? = null,
    val plannedReps: Int? = null,
    val plannedWeightKg: Double? = null,
    val targetRpe: Double? = null,
    val restSeconds: Int? = null,
    val notes: String = ""
)

