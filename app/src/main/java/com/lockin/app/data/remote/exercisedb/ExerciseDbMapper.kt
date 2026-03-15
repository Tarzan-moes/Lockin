package com.lockin.app.data.remote.exercisedb

import com.lockin.app.data.local.models.ExerciseDetailEntity

/**
 * Maps an ExerciseDB API response DTO into our local Room entity.
 */
fun ExerciseDbDto.toEntity(): ExerciseDetailEntity = ExerciseDetailEntity(
    id = exerciseId,
    name = name.replaceFirstChar { it.uppercase() },
    primaryMuscleGroup = targetMuscles.firstOrNull()?.uppercase()?.replace(" ", "_") ?: "",
    secondaryMuscles = secondaryMuscles.map { it.uppercase().replace(" ", "_") },
    equipment = equipments.map { it.uppercase().replace(" ", "_") },
    movementPattern = bodyParts.firstOrNull()?.uppercase()?.replace(" ", "_") ?: "",
    difficulty = "MODERATE",
    instructions = instructions.joinToString("\n"),
    tips = emptyList(),
    gifUrl = gifUrl
)
