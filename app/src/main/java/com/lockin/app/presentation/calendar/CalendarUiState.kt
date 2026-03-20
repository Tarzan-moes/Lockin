package com.lockin.app.presentation.calendar

import com.lockin.app.data.local.models.WorkoutPlanEntity
import java.time.LocalDate

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val schedules: Map<LocalDate, List<WorkoutScheduleUi>> = emptyMap(),
    val availableTemplates: List<WorkoutPlanEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WorkoutScheduleUi(
    val id: Long,
    val date: LocalDate,
    val workoutTemplateId: String,
    val workoutName: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val notes: String?,
    val durationMinutes: Int,
    val difficulty: String
)
