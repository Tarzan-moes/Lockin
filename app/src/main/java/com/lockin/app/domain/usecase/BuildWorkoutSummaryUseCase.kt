package com.lockin.app.domain.usecase

import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.data.local.models.ExerciseSetEntity
import java.time.Duration
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WorkoutSummaryForAI(
    val workoutName: String,
    val goal: String,
    val date: String,
    val durationMinutes: Int,
    val totalVolumeKg: Float,
    val totalSets: Int,
    val averageRpe: Float?,
    val exercises: List<ExerciseSummaryForAI>
)

data class ExerciseSummaryForAI(
    val name: String,
    val primaryMuscle: String,
    val equipment: String,
    val sets: List<SetSummaryForAI>
)

data class SetSummaryForAI(
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val rpe: Int?
)

class BuildWorkoutSummaryUseCase @Inject constructor(
    private val workoutDao: WorkoutDao
) {

    suspend fun build(workoutId: String): WorkoutSummaryForAI? {
        val workout = workoutDao.getWorkoutById(workoutId) ?: return null
        val workoutExercises = workoutDao.getWorkoutExercisesForWorkout(workoutId)

        val exerciseSummaries = workoutExercises.mapNotNull { we ->
            val detail = workoutDao.getExerciseById(we.exerciseId) ?: return@mapNotNull null
            val sets = workoutDao
                .getSetsForWorkoutExercise(we.id)
                .filter { it.status == "COMPLETED" }
                .sortedBy { it.setNumber }

            ExerciseSummaryForAI(
                name = detail.name.ifBlank { "Unknown Exercise" },
                primaryMuscle = detail.primaryMuscleGroup.ifBlank { "Unknown" },
                equipment = detail.equipment.firstOrNull().orEmpty().ifBlank { "Bodyweight" },
                sets = sets.map { it.toSetSummary() }
            )
        }

        val allSets = exerciseSummaries.flatMap { it.sets }
        val volume = allSets.sumOf { it.weightKg.toDouble() * it.reps }.toFloat()
        val totalSets = allSets.size
        val avgRpe = allSets.mapNotNull { it.rpe?.toFloat() }.average().toFloat().takeIf { !it.isNaN() }

        val durationMinutes = if (workout.startTime != null && workout.endTime != null) {
            Duration.between(workout.startTime, workout.endTime).toMinutes().toInt().coerceAtLeast(0)
        } else {
            0
        }

        val dateString = workout.startTime?.toLocalDate()?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: workout.plannedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: "Unknown"

        return WorkoutSummaryForAI(
            workoutName = workout.name.ifBlank { "Workout" },
            goal = workout.goal,
            date = dateString,
            durationMinutes = durationMinutes,
            totalVolumeKg = volume,
            totalSets = totalSets,
            averageRpe = avgRpe,
            exercises = exerciseSummaries
        )
    }

    fun buildPromptString(summary: WorkoutSummaryForAI): String = buildString {
        appendLine("Workout: ${summary.workoutName}")
        appendLine("Goal: ${summary.goal}")
        appendLine("Date: ${summary.date}")
        appendLine("Duration: ${summary.durationMinutes} min")
        appendLine("Total Volume: ${"%.1f".format(summary.totalVolumeKg)} kg")
        appendLine("Total Sets: ${summary.totalSets}")
        appendLine("Average RPE: ${summary.averageRpe?.let { "%.1f".format(it) } ?: "N/A"}")
        appendLine()
        appendLine("Exercises:")
        summary.exercises.forEachIndexed { index, exercise ->
            appendLine("${index + 1}. ${exercise.name} (${exercise.primaryMuscle} | ${exercise.equipment})")
            exercise.sets.forEach { set ->
                val rpePart = set.rpe?.let { " @ RPE $it" } ?: ""
                appendLine("   Set ${set.setNumber}: ${formatWeight(set.weightKg)}kg × ${set.reps} reps$rpePart")
            }
            appendLine()
        }
    }.trim()

    private fun ExerciseSetEntity.toSetSummary(): SetSummaryForAI {
        val safeWeight = (weightKg ?: 0.0).toFloat().coerceAtLeast(0f)
        val safeReps = (reps ?: 0).coerceAtLeast(0)
        val safeRpe = rpe?.toInt()?.coerceIn(1, 10)
        return SetSummaryForAI(
            setNumber = setNumber,
            weightKg = safeWeight,
            reps = safeReps,
            rpe = safeRpe
        )
    }

    private fun formatWeight(weightKg: Float): String {
        return if (weightKg % 1f == 0f) weightKg.toInt().toString() else "%.1f".format(weightKg)
    }
}

