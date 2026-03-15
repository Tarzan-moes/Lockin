package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.app.data.local.models.WorkoutEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface WorkoutEntityDao {
    @Query("SELECT * FROM workouts ORDER BY plannedDate DESC NULLS LAST, createdAt DESC")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE plannedDate = :date LIMIT 1")
    suspend fun getByDate(date: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY endTime DESC")
    suspend fun getCompletedWorkouts(): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT startTime FROM workouts WHERE status = 'COMPLETED' AND startTime IS NOT NULL ORDER BY startTime DESC")
    fun getCompletedWorkoutDateTimes(): Flow<List<LocalDateTime>>
}
