package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String? = null,
    val name: String = "",
    val type: String = "HYBRID", // STRENGTH/CARDIO/HYBRID
    val goal: String = "HYPERTROPHY", // STRENGTH/HYPERTROPHY/ENDURANCE
    val plannedDate: LocalDate? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val status: String = "PLANNED", // PLANNED/IN_PROGRESS/COMPLETED
    val totalVolumeKg: Double? = null,
    val totalReps: Int? = null,
    val totalSets: Int? = null,
    val calories: Int? = null,
    val averageHeartRate: Int? = null,
    val notes: String = "",
    val isAiGenerated: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

