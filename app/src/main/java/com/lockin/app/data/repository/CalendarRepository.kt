package com.lockin.app.data.repository

import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * CalendarRepository – abstraction over Room DAOs for the calendar feature.
 *
 * Decouples CalendarViewModel from direct DAO dependencies, making the
 * ViewModel independently testable with a fake implementation.
 */
interface CalendarRepository {
    /** Returns all workout plan templates as a reactive stream. */
    fun getWorkoutPlans(): Flow<List<WorkoutPlanEntity>>

    /** Returns schedules for a single day as a reactive stream. */
    fun getSchedulesForDate(date: LocalDate): Flow<List<WorkoutScheduleEntity>>

    /** Returns schedules across an inclusive date range as a reactive stream. */
    fun getSchedulesBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkoutScheduleEntity>>

    /** Inserts a new schedule entry, returning the generated row id. */
    suspend fun insertSchedule(schedule: WorkoutScheduleEntity): Long

    /** Deletes a schedule entry by its row id. */
    suspend fun deleteSchedule(id: Long)

    /** Marks a schedule entry as completed or not. */
    suspend fun setScheduleCompleted(id: Long, isCompleted: Boolean)

    /**
     * Inserts a workout plan template; replaces on conflict so seeding is
     * idempotent across app restarts.
     */
    suspend fun insertWorkoutPlan(plan: WorkoutPlanEntity)

    /** Returns how many workout plan templates are currently stored. */
    suspend fun workoutPlanCount(): Int
}
