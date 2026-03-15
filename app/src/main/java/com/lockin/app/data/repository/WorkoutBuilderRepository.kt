package com.lockin.app.data.repository

import com.lockin.app.data.local.database.dao.ExerciseDetailDao
import com.lockin.app.data.local.database.dao.ExerciseSetDao
import com.lockin.app.data.local.database.dao.WorkoutEntityDao
import com.lockin.app.data.local.database.dao.WorkoutExerciseDao
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.local.models.ExerciseSetEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.local.models.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkoutBuilderRepository – Orchestrates creating, editing, and completing workouts.
 * Thin layer over the DAOs; keeps ViewModels free of direct DAO calls.
 */
@Singleton
class WorkoutBuilderRepository @Inject constructor(
    private val workoutDao: WorkoutEntityDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val exerciseDetailDao: ExerciseDetailDao
) {
    // ── Workout CRUD ─────────────────────────────────────────────

    fun observeAllWorkouts(): Flow<List<WorkoutEntity>> =
        workoutDao.observeWorkouts()

    suspend fun getWorkout(id: String): WorkoutEntity? =
        workoutDao.getById(id)

    suspend fun saveWorkout(workout: WorkoutEntity) =
        workoutDao.upsert(workout)

    suspend fun deleteWorkout(id: String) {
        // Delete child exercises and their sets first
        val exercises = workoutExerciseDao.getForWorkout(id)
        exercises.forEach { exerciseSetDao.deleteForWorkoutExercise(it.id) }
        workoutExerciseDao.deleteForWorkout(id)
        workoutDao.delete(id)
    }

    // ── Workout Exercises ────────────────────────────────────────

    suspend fun getExercisesForWorkout(workoutId: String): List<WorkoutExerciseEntity> =
        workoutExerciseDao.getForWorkout(workoutId)

    fun observeExercisesForWorkout(workoutId: String): Flow<List<WorkoutExerciseEntity>> =
        workoutExerciseDao.observeForWorkout(workoutId)

    suspend fun addExerciseToWorkout(
        workoutId: String,
        exerciseDetail: ExerciseDetailEntity,
        plannedSets: Int = 3,
        plannedReps: Int = 10,
        targetRpe: Double? = null
    ): WorkoutExerciseEntity {
        val existingCount = workoutExerciseDao.getForWorkout(workoutId).size
        val we = WorkoutExerciseEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exerciseId = exerciseDetail.id,
            orderIndex = existingCount,
            plannedSets = plannedSets,
            plannedReps = plannedReps,
            targetRpe = targetRpe
        )
        workoutExerciseDao.upsert(we)
        return we
    }

    suspend fun updateWorkoutExercise(item: WorkoutExerciseEntity) =
        workoutExerciseDao.upsert(item)

    // ── Sets ─────────────────────────────────────────────────────

    suspend fun getSetsForExercise(workoutExerciseId: String): List<ExerciseSetEntity> =
        exerciseSetDao.getForWorkoutExercise(workoutExerciseId)

    fun observeSetsForExercise(workoutExerciseId: String): Flow<List<ExerciseSetEntity>> =
        exerciseSetDao.observeForWorkoutExercise(workoutExerciseId)

    suspend fun saveSet(set: ExerciseSetEntity) =
        exerciseSetDao.upsert(set)

    // ── Workout lifecycle ────────────────────────────────────────

    suspend fun startWorkout(workoutId: String) {
        val workout = workoutDao.getById(workoutId) ?: return
        workoutDao.upsert(
            workout.copy(
                status = "IN_PROGRESS",
                startTime = LocalDateTime.now()
            )
        )
        // Pre-create empty sets for each exercise
        val exercises = workoutExerciseDao.getForWorkout(workoutId)
        for (we in exercises) {
            val existing = exerciseSetDao.getForWorkoutExercise(we.id)
            if (existing.isEmpty()) {
                val sets = (1..(we.plannedSets ?: 3)).map { num ->
                    ExerciseSetEntity(
                        id = UUID.randomUUID().toString(),
                        workoutExerciseId = we.id,
                        setNumber = num
                    )
                }
                exerciseSetDao.upsertAll(sets)
            }
        }
    }

    suspend fun completeWorkout(workoutId: String) {
        val workout = workoutDao.getById(workoutId) ?: return
        val exercises = workoutExerciseDao.getForWorkout(workoutId)
        var totalVolume = 0.0
        var totalReps = 0
        var totalSets = 0
        for (we in exercises) {
            val sets = exerciseSetDao.getForWorkoutExercise(we.id)
            for (s in sets) {
                if (s.status == "COMPLETED") {
                    totalSets++
                    val r = s.reps ?: 0
                    val w = s.weightKg ?: 0.0
                    totalReps += r
                    totalVolume += w * r
                }
            }
        }
        workoutDao.upsert(
            workout.copy(
                status = "COMPLETED",
                endTime = LocalDateTime.now(),
                totalVolumeKg = totalVolume,
                totalReps = totalReps,
                totalSets = totalSets
            )
        )
    }

    // ── Exercise library ─────────────────────────────────────────

    fun observeAllExerciseDetails(): Flow<List<ExerciseDetailEntity>> =
        exerciseDetailDao.observeAll()

    suspend fun getExerciseDetail(id: String): ExerciseDetailEntity? =
        exerciseDetailDao.getById(id)

    suspend fun seedExercisesIfEmpty(exercises: List<ExerciseDetailEntity>) {
        val existing = exerciseDetailDao.observeAll().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return
        exerciseDetailDao.upsertAll(exercises)
    }
}

