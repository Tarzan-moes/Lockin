package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutScheduleDao {
    @Query("SELECT * FROM workout_schedules WHERE scheduledDate BETWEEN :startDate AND :endDate")
    fun getSchedulesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedules WHERE scheduledDate = :date")
    fun getSchedulesForDate(date: LocalDate): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedules WHERE userId = :userId")
    fun getSchedulesForUser(userId: Long): Flow<List<WorkoutScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: WorkoutScheduleEntity): Long

    @Query("DELETE FROM workout_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("UPDATE workout_schedules SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean)

    @Query("UPDATE workout_schedules SET isCompleted = 1, completedAt = :timestamp WHERE id = :id")
    suspend fun markComplete(id: Long, timestamp: Long)
}

