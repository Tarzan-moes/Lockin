package com.lockin.app.data.repository

import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.presentation.calendar.WorkoutScheduleUi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CalendarRepository {
    suspend fun getWorkoutPlans(): List<WorkoutPlanEntity>
    fun getSchedules(date: LocalDate): Flow<List<WorkoutScheduleUi>>
    fun getSchedulesForMonth(start: LocalDate, end: LocalDate): Flow<Map<LocalDate, List<WorkoutScheduleUi>>>
    suspend fun scheduleWorkout(date: LocalDate, planId: String, notes: String? = null): Long
    suspend fun deleteSchedule(id: Long)
    suspend fun completeSchedule(id: Long)
    suspend fun seedWorkoutPlansIfEmpty()
}
