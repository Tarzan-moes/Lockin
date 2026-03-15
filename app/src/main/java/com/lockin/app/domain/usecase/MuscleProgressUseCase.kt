package com.lockin.app.domain.usecase

import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class MuscleProgressPoint(
    val date: LocalDate,
    val totalVolume: Float,
    val bestSetVolume: Float,
    val exercises: List<ExerciseSetDetail>
)

data class ExerciseSetDetail(
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int,
    val rpe: Int?
)

data class WeeklyVolumeRow(
    val muscleGroup: MuscleGroup,
    val thisWeekVolume: Float,
    val lastWeekVolume: Float
)

class MuscleProgressUseCase @Inject constructor(
    private val workoutDao: WorkoutDao
) {

    fun getProgressForMuscle(muscleGroup: MuscleGroup): Flow<List<MuscleProgressPoint>> {
        return workoutDao.getAllCompletedSetsWithDates().map { rows ->
            val filtered = rows.filter { row ->
                val detail = workoutDao.getExerciseById(row.exerciseId)
                detail.matchesMuscle(muscleGroup)
            }

            filtered
                .groupBy { it.workoutDate.toLocalDate() }
                .map { (date, dayRows) ->
                    val setVolumes = dayRows.map { row ->
                        val weight = (row.set.weightKg ?: 0.0).toFloat()
                        val reps = (row.set.reps ?: 0)
                        weight * reps
                    }

                    val details = dayRows.mapNotNull { row ->
                        val detail = workoutDao.getExerciseById(row.exerciseId) ?: return@mapNotNull null
                        ExerciseSetDetail(
                            exerciseName = detail.name,
                            weightKg = (row.set.weightKg ?: 0.0).toFloat(),
                            reps = row.set.reps ?: 0,
                            rpe = row.set.rpe?.toInt()
                        )
                    }

                    MuscleProgressPoint(
                        date = date,
                        totalVolume = setVolumes.sum(),
                        bestSetVolume = setVolumes.maxOrNull() ?: 0f,
                        exercises = details
                    )
                }
                .sortedBy { it.date }
        }
    }

    fun getWeeklyVolumeSummary(): Flow<List<WeeklyVolumeRow>> {
        return workoutDao.getAllCompletedSetsWithDates().map { rows ->
            val today = LocalDate.now()
            val thisWeekStart = today.minusDays(6)
            val lastWeekStart = thisWeekStart.minusDays(7)
            val lastWeekEnd = thisWeekStart.minusDays(1)

            MuscleGroup.entries.map { muscle ->
                var thisWeek = 0f
                var lastWeek = 0f

                rows.forEach { row ->
                    val detail = workoutDao.getExerciseById(row.exerciseId) ?: return@forEach
                    if (!detail.matchesMuscle(muscle)) return@forEach

                    val date = row.workoutDate.toLocalDate()
                    val volume = ((row.set.weightKg ?: 0.0) * (row.set.reps ?: 0)).toFloat()
                    if (date in thisWeekStart..today) thisWeek += volume
                    if (date in lastWeekStart..lastWeekEnd) lastWeek += volume
                }

                WeeklyVolumeRow(
                    muscleGroup = muscle,
                    thisWeekVolume = thisWeek,
                    lastWeekVolume = lastWeek
                )
            }.filter { it.thisWeekVolume > 0f || it.lastWeekVolume > 0f }
        }
    }

    private fun com.lockin.app.data.local.models.ExerciseDetailEntity?.matchesMuscle(
        muscleGroup: MuscleGroup
    ): Boolean {
        if (this == null) return false
        val token = muscleGroup.name
        val primary = primaryMuscleGroup.uppercase().replace(" ", "_")
        val secondary = secondaryMuscles.map { it.uppercase().replace(" ", "_") }
        return primary.contains(token) || secondary.any { it.contains(token) }
    }
}

