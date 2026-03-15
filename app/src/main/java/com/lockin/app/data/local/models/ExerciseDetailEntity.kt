package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises_full")
data class ExerciseDetailEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val primaryMuscleGroup: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val movementPattern: String = "",
    val difficulty: String = "MODERATE",
    val instructions: String = "",
    val tips: List<String> = emptyList(),
    val gifUrl: String? = null
)
