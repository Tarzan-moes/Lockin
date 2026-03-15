package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "workout_schedules")
data class WorkoutScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val workoutTemplateId: String,
    val scheduledDate: LocalDate,
    val isCompleted: Boolean = false,
    val recurrence: String? = null // "WEEKLY" or "NONE"
)

