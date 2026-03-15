package com.lockin.app.data.remote.wger

import com.lockin.app.data.local.models.ExerciseDetailEntity

/**
 * Maps a wger exercise DTO into our local Room entity.
 *
 * wger category names: Abs, Arms, Back, Calves, Chest, Legs, Shoulders, Cardio
 * We prefix IDs with "wger_" to avoid collisions with ExerciseDB IDs.
 */
fun WgerExerciseDto.toEntity(): ExerciseDetailEntity {
    val primaryMuscle = category?.name?.uppercase()?.replace(" ", "_") ?: ""

    // Pick the best image: prefer main image, otherwise first available
    val imageUrl = images
        .sortedByDescending { it.isMain }
        .firstOrNull()?.image

    val secondaryMuscleList = musclesSecondary.map {
        (it.nameEn.ifBlank { it.name }).uppercase().replace(" ", "_")
    }

    val equipmentList = equipment.map {
        it.name.uppercase().replace(" ", "_")
    }

    // Strip HTML tags from wger description
    val cleanDescription = description
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .trim()

    return ExerciseDetailEntity(
        id = "wger_$id",
        name = name.trim().replaceFirstChar { it.uppercase() },
        primaryMuscleGroup = primaryMuscle,
        secondaryMuscles = secondaryMuscleList,
        equipment = equipmentList,
        movementPattern = "",
        difficulty = "MODERATE",
        instructions = cleanDescription,
        tips = emptyList(),
        gifUrl = imageUrl
    )
}

