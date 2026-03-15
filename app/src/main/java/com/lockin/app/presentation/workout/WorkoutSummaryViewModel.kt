package com.lockin.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.local.models.ExerciseSetEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.local.models.WorkoutExerciseEntity
import com.lockin.app.data.repository.WorkoutBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    private val repo: WorkoutBuilderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        loadLastCompletedWorkout()
    }

    private fun loadLastCompletedWorkout() {
        viewModelScope.launch {
            val workouts = repo.observeAllWorkouts().firstOrNull() ?: emptyList()
            val lastCompleted = workouts
                .filter { it.status == "COMPLETED" }
                .maxByOrNull { it.endTime ?: it.createdAt }

            if (lastCompleted == null) {
                _uiState.value = WorkoutSummaryUiState(isLoading = false)
                return@launch
            }

            val exercises = repo.getExercisesForWorkout(lastCompleted.id)
            val exerciseSummaries = exercises.mapNotNull { we ->
                val detail = repo.getExerciseDetail(we.exerciseId) ?: return@mapNotNull null
                val sets = repo.getSetsForExercise(we.id).filter { it.status == "COMPLETED" }
                val bestSet = sets.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
                ExerciseSummary(
                    name = detail.name,
                    primaryMuscle = detail.primaryMuscleGroup,
                    totalSets = sets.size,
                    bestWeight = bestSet?.weightKg,
                    bestReps = bestSet?.reps,
                    avgRpe = sets.mapNotNull { it.rpe }.average().takeIf { !it.isNaN() }
                )
            }

            val durationMinutes = if (lastCompleted.startTime != null && lastCompleted.endTime != null) {
                java.time.Duration.between(lastCompleted.startTime, lastCompleted.endTime).toMinutes().toInt()
            } else null

            val allSets = exercises.flatMap { we -> repo.getSetsForExercise(we.id).filter { it.status == "COMPLETED" } }
            val overallAvgRpe = allSets.mapNotNull { it.rpe }.average().takeIf { !it.isNaN() }

            _uiState.value = WorkoutSummaryUiState(
                isLoading = false,
                workout = lastCompleted,
                durationMinutes = durationMinutes,
                avgRpe = overallAvgRpe,
                exerciseSummaries = exerciseSummaries
            )
        }
    }
}

data class ExerciseSummary(
    val name: String,
    val primaryMuscle: String,
    val totalSets: Int,
    val bestWeight: Double?,
    val bestReps: Int?,
    val avgRpe: Double?
)

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val workout: WorkoutEntity? = null,
    val durationMinutes: Int? = null,
    val avgRpe: Double? = null,
    val exerciseSummaries: List<ExerciseSummary> = emptyList()
)

