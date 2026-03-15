package com.lockin.app.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.local.models.WorkoutExerciseEntity
import com.lockin.app.data.repository.WorkoutBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: WorkoutBuilderRepository
) : ViewModel() {

    val workoutId: String = savedStateHandle.get<String>("workoutId") ?: "new"

    private val _uiState = MutableStateFlow(WorkoutEditorUiState())
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    init {
        if (workoutId != "new") {
            loadWorkoutMeta()
            observeExercises()
        }
    }

    /** Load workout metadata once. */
    private fun loadWorkoutMeta() {
        viewModelScope.launch {
            val workout = repo.getWorkout(workoutId) ?: return@launch
            _uiState.update {
                it.copy(
                    workout = workout,
                    name = workout.name,
                    goal = workout.goal,
                    notes = workout.notes,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Observe exercises for this workout reactively.
     * When the picker adds new exercises to the DB and the user navigates back,
     * this Flow automatically emits the updated list.
     */
    private fun observeExercises() {
        viewModelScope.launch {
            repo.observeExercisesForWorkout(workoutId).collect { exercises ->
                val withDetails = exercises.map { we ->
                    val detail = repo.getExerciseDetail(we.exerciseId)
                    ExerciseWithDetail(we, detail)
                }
                _uiState.update { it.copy(exercises = withDetails) }
            }
        }
    }

    fun updateName(name: String) { _uiState.update { it.copy(name = name) } }
    fun updateGoal(goal: String) { _uiState.update { it.copy(goal = goal) } }
    fun updateNotes(notes: String) { _uiState.update { it.copy(notes = notes) } }

    fun saveWorkout() {
        viewModelScope.launch {
            val current = _uiState.value
            val workout = (current.workout ?: return@launch).copy(
                name = current.name,
                goal = current.goal,
                notes = current.notes
            )
            repo.saveWorkout(workout)
        }
    }

    fun addExercise(detail: ExerciseDetailEntity) {
        viewModelScope.launch {
            val workout = _uiState.value.workout ?: return@launch
            repo.addExerciseToWorkout(workout.id, detail)
            // No manual UI update needed — observeExercises() will emit the new list
        }
    }

    fun updateExerciseTargets(exerciseId: String, sets: Int?, reps: Int?, rpe: Double?) {
        viewModelScope.launch {
            val idx = _uiState.value.exercises.indexOfFirst { it.workoutExercise.id == exerciseId }
            if (idx < 0) return@launch
            val old = _uiState.value.exercises[idx].workoutExercise
            val updated = old.copy(
                plannedSets = sets ?: old.plannedSets,
                plannedReps = reps ?: old.plannedReps,
                targetRpe = rpe ?: old.targetRpe
            )
            repo.updateWorkoutExercise(updated)
            // Flow will update automatically, but also update locally for instant feedback
            val list = _uiState.value.exercises.toMutableList()
            list[idx] = list[idx].copy(workoutExercise = updated)
            _uiState.update { it.copy(exercises = list) }
        }
    }
}

data class ExerciseWithDetail(
    val workoutExercise: WorkoutExerciseEntity,
    val detail: ExerciseDetailEntity?
)

data class WorkoutEditorUiState(
    val workout: WorkoutEntity? = null,
    val exercises: List<ExerciseWithDetail> = emptyList(),
    val name: String = "",
    val goal: String = "HYPERTROPHY",
    val notes: String = "",
    val isLoading: Boolean = true
)
