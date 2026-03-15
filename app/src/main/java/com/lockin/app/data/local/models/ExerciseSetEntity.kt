package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "exercise_sets")
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val setNumber: Int = 1,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val status: String = "PLANNED" // PLANNED/COMPLETED/SKIPPED
)

