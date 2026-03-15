package com.lockin.app.data.remote.wger

import com.google.gson.annotations.SerializedName

/**
 * Top-level paginated response from wger API.
 * { "count": N, "next": "...", "previous": "...", "results": [...] }
 */
data class WgerResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("next") val next: String? = null,
    @SerializedName("results") val results: List<WgerExerciseDto> = emptyList()
)

/**
 * DTO for a single exercise from wger /exerciseinfo/ endpoint.
 */
data class WgerExerciseDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("category") val category: WgerCategory? = null,
    @SerializedName("muscles") val muscles: List<WgerMuscle> = emptyList(),
    @SerializedName("muscles_secondary") val musclesSecondary: List<WgerMuscle> = emptyList(),
    @SerializedName("equipment") val equipment: List<WgerEquipment> = emptyList(),
    @SerializedName("images") val images: List<WgerImage> = emptyList()
)

data class WgerCategory(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = ""
)

data class WgerMuscle(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("name_en") val nameEn: String = ""
)

data class WgerEquipment(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = ""
)

data class WgerImage(
    @SerializedName("image") val image: String = "",
    @SerializedName("is_main") val isMain: Boolean = false
)

