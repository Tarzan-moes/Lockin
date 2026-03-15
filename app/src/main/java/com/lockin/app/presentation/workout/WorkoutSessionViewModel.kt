package com.lockin.app.presentation.workout

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: WorkoutBuilderRepository
) : ViewModel() {

    private val workoutId: String = savedStateHandle.get<String>("workoutId") ?: ""

    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val workout = repo.getWorkout(workoutId) ?: return@launch
            // Start workout if still PLANNED
            if (workout.status == "PLANNED") {
                repo.startWorkout(workoutId)
            }
            val updatedWorkout = repo.getWorkout(workoutId) ?: workout
            val exercises = repo.getExercisesForWorkout(workoutId)
            val items = exercises.map { we ->
                val detail = repo.getExerciseDetail(we.exerciseId)
                val sets = repo.getSetsForExercise(we.id)
                SessionExercise(we, detail, sets)
            }
            _uiState.update {
                it.copy(
                    workout = updatedWorkout,
                    exercises = items,
                    isLoading = false
                )
            }
        }
    }

    fun updateSet(setId: String, weight: Double?, reps: Int?, rpe: Double?) {
        viewModelScope.launch {
            val exercises = _uiState.value.exercises.toMutableList()
            for (i in exercises.indices) {
                val sIdx = exercises[i].sets.indexOfFirst { it.id == setId }
                if (sIdx >= 0) {
                    val old = exercises[i].sets[sIdx]
                    val updated = old.copy(
                        weightKg = weight ?: old.weightKg,
                        reps = reps ?: old.reps,
                        rpe = rpe ?: old.rpe
                    )
                    repo.saveSet(updated)
                    val newSets = exercises[i].sets.toMutableList().apply { set(sIdx, updated) }
                    exercises[i] = exercises[i].copy(sets = newSets)
                    break
                }
            }
            _uiState.update { it.copy(exercises = exercises) }
        }
    }

    fun completeSet(setId: String) {
        viewModelScope.launch {
            val exercises = _uiState.value.exercises.toMutableList()
            for (i in exercises.indices) {
                val sIdx = exercises[i].sets.indexOfFirst { it.id == setId }
                if (sIdx >= 0) {
                    val old = exercises[i].sets[sIdx]
                    val updated = old.copy(
                        status = "COMPLETED",
                        completedAt = LocalDateTime.now()
                    )
                    repo.saveSet(updated)
                    val newSets = exercises[i].sets.toMutableList().apply { set(sIdx, updated) }
                    exercises[i] = exercises[i].copy(sets = newSets)
                    break
                }
            }
            _uiState.update { it.copy(exercises = exercises) }
        }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            repo.completeWorkout(workoutId)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}

data class SessionExercise(
    val workoutExercise: WorkoutExerciseEntity,
    val detail: ExerciseDetailEntity?,
    val sets: List<ExerciseSetEntity>
)

data class WorkoutSessionUiState(
    val workout: WorkoutEntity? = null,
    val exercises: List<SessionExercise> = emptyList(),
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false
)

