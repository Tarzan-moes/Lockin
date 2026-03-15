package com.lockin.app.data.repository

import com.lockin.app.data.local.database.dao.WorkoutEntityDao
import com.lockin.app.data.local.models.WorkoutEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class WorkoutEntityRepository @Inject constructor(
    private val workoutEntityDao: WorkoutEntityDao
) {
    suspend fun getRecentWorkouts(limit: Int = 5): List<WorkoutEntity> {
        return workoutEntityDao.observeWorkouts()
            .firstOrNull()
            ?.take(limit)
            ?: emptyList()
    }

    suspend fun getTodayWorkout(date: LocalDate = LocalDate.now()): WorkoutEntity? {
        return workoutEntityDao.getByDate(date.toString())
    }
}
