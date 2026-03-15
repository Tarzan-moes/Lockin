package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.ColumnInfo
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.local.models.ExerciseSetEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.local.models.WorkoutExerciseEntity
import com.lockin.app.data.local.models.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * WorkoutDao – Data Access Object for workout-related tables.
 *
 * Room generates the implementation from these method signatures.
 * For Part 1 these are minimal stubs; full queries come later.
 */
@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_plans")
    suspend fun getAllWorkoutPlans(): List<WorkoutPlanEntity>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getWorkoutPlanById(id: String): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlanEntity)

    // --- AI workout review queries ---
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutById(workoutId: String): WorkoutEntity?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getWorkoutExercisesForWorkout(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercise_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun getSetsForWorkoutExercise(workoutExerciseId: String): List<ExerciseSetEntity>

    @Query("SELECT * FROM exercises_full WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): ExerciseDetailEntity?

    @Query("SELECT * FROM workouts WHERE status = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    suspend fun getLastCompletedWorkouts(limit: Int): List<WorkoutEntity>

    // --- Muscle progress queries ---
    @Query(
        """
        SELECT es.*, we.exerciseId AS exerciseId, w.startTime AS workoutDate
        FROM exercise_sets es
        JOIN workout_exercises we ON es.workoutExerciseId = we.id
        JOIN workouts w ON we.workoutId = w.id
        WHERE w.status = 'COMPLETED' AND w.startTime IS NOT NULL AND es.status = 'COMPLETED'
        ORDER BY w.startTime ASC
        """
    )
    fun getAllCompletedSetsWithDates(): Flow<List<CompletedSetWithDate>>
}

data class CompletedSetWithDate(
    @Embedded val set: ExerciseSetEntity,
    @ColumnInfo(name = "exerciseId") val exerciseId: String,
    @ColumnInfo(name = "workoutDate") val workoutDate: LocalDateTime
)

