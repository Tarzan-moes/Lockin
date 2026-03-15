package com.lockin.app.data.remote.exercisedb

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for all ExerciseDB paginated responses.
 * The API always returns: { "success": true, "metadata": {...}, "data": [...] }
 */
data class ExerciseDbResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: List<ExerciseDbDto> = emptyList()
)

/**
 * DTO for a single exercise from the ExerciseDB API.
 * Field names match the actual API response (verified March 2026).
 */
data class ExerciseDbDto(
    @SerializedName("exerciseId") val exerciseId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("bodyParts") val bodyParts: List<String> = emptyList(),
    @SerializedName("targetMuscles") val targetMuscles: List<String> = emptyList(),
    @SerializedName("secondaryMuscles") val secondaryMuscles: List<String> = emptyList(),
    @SerializedName("equipments") val equipments: List<String> = emptyList(),
    @SerializedName("gifUrl") val gifUrl: String = "",
    @SerializedName("instructions") val instructions: List<String> = emptyList()
)
