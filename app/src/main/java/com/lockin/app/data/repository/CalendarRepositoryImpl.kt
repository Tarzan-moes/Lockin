package com.lockin.app.data.repository

import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.data.local.database.dao.WorkoutScheduleDao
import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [CalendarRepository].
 *
 * Delegates all storage operations to [WorkoutScheduleDao] and [WorkoutDao].
 * The ViewModel only sees the interface, making it straightforward to swap
 * this implementation in tests with a fake.
 */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val scheduleDao: WorkoutScheduleDao,
    private val workoutDao: WorkoutDao
) : CalendarRepository {

    override fun getWorkoutPlans(): Flow<List<WorkoutPlanEntity>> =
        workoutDao.getAllWorkoutPlans()

    override fun getSchedulesForDate(date: LocalDate): Flow<List<WorkoutScheduleEntity>> =
        scheduleDao.getSchedulesForDate(date)

    override fun getSchedulesBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<WorkoutScheduleEntity>> =
        scheduleDao.getSchedulesBetweenDates(startDate, endDate)

    override suspend fun insertSchedule(schedule: WorkoutScheduleEntity): Long =
        scheduleDao.insertSchedule(schedule)

    override suspend fun deleteSchedule(id: Long) =
        scheduleDao.deleteSchedule(id)

    override suspend fun setScheduleCompleted(id: Long, isCompleted: Boolean) =
        scheduleDao.updateCompletionStatus(id, isCompleted)

    override suspend fun insertWorkoutPlan(plan: WorkoutPlanEntity) =
        workoutDao.insertWorkoutPlan(plan)

    override suspend fun workoutPlanCount(): Int =
        workoutDao.getWorkoutPlanCount()
}
