package com.lockin.app.presentation.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.repository.ExerciseLibraryRepository
import com.lockin.app.data.repository.WorkoutBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: WorkoutBuilderRepository,
    private val exerciseLibrary: ExerciseLibraryRepository
) : ViewModel() {

    val workoutId: String = savedStateHandle.get<String>("workoutId") ?: ""

    private val _uiState = MutableStateFlow(ExercisePickerUiState())
    val uiState: StateFlow<ExercisePickerUiState> = _uiState.asStateFlow()

    private var allExercises: List<ExerciseDetailEntity> = emptyList()

    init {
        // Trigger API sync in background
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatus = "Starting sync…") }
            try {
                val success = exerciseLibrary.syncFromApi()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = exerciseLibrary.lastSyncMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = "Sync failed: ${e.message}"
                    )
                }
            }
        }
        // Observe Room as single source of truth
        viewModelScope.launch {
            exerciseLibrary.observeAll().collect { list ->
                allExercises = list
                applyFilters()
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun toggleMuscleFilter(muscle: String) {
        _uiState.update { current ->
            val filters = current.selectedMuscles.toMutableSet()
            if (muscle in filters) filters.remove(muscle) else filters.add(muscle)
            current.copy(selectedMuscles = filters)
        }
        applyFilters()
    }

    fun toggleEquipmentFilter(eq: String) {
        _uiState.update { current ->
            val filters = current.selectedEquipment.toMutableSet()
            if (eq in filters) filters.remove(eq) else filters.add(eq)
            current.copy(selectedEquipment = filters)
        }
        applyFilters()
    }

    fun addExerciseToWorkout(exercise: ExerciseDetailEntity) {
        viewModelScope.launch {
            repo.addExerciseToWorkout(workoutId, exercise)
            _uiState.update { it.copy(addedIds = it.addedIds + exercise.id) }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = allExercises.filter { ex ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    ex.name.contains(state.searchQuery, ignoreCase = true) ||
                    ex.primaryMuscleGroup.contains(state.searchQuery, ignoreCase = true)
            val matchesMuscle = state.selectedMuscles.isEmpty() ||
                    state.selectedMuscles.any { filter -> exerciseMatchesMuscle(ex, filter) }
            val matchesEquipment = state.selectedEquipment.isEmpty() ||
                    state.selectedEquipment.any { filter -> exerciseMatchesEquipment(ex, filter) }
            matchesSearch && matchesMuscle && matchesEquipment
        }
        _uiState.update { it.copy(exercises = filtered) }
    }

    /**
     * Flexible muscle group matching that handles both ExerciseDB and wger naming.
     * E.g. filter "ABS" matches primaryMuscleGroup "ABS", "WAIST", movementPattern "WAIST", etc.
     */
    private fun exerciseMatchesMuscle(ex: ExerciseDetailEntity, filter: String): Boolean {
        val synonyms = muscleSynonyms[filter] ?: setOf(filter)
        val allExFields = listOf(ex.primaryMuscleGroup) + ex.secondaryMuscles + listOf(ex.movementPattern)
        return allExFields.any { field ->
            synonyms.any { syn -> field.equals(syn, ignoreCase = true) }
        }
    }

    private fun exerciseMatchesEquipment(ex: ExerciseDetailEntity, filter: String): Boolean {
        return ex.equipment.any { it.equals(filter, ignoreCase = true) }
    }

    companion object {
        /** Maps a filter chip label to all equivalent values from ExerciseDB + wger. */
        private val muscleSynonyms = mapOf(
            "CHEST" to setOf("CHEST", "PECTORALS"),
            "BACK" to setOf("BACK", "UPPER_BACK", "LATS"),
            "SHOULDERS" to setOf("SHOULDERS", "DELTS", "REAR_DELTOIDS"),
            "ABS" to setOf("ABS", "WAIST", "ABDOMINALS"),
            "CORE" to setOf("CORE", "WAIST", "ABS", "ABDOMINALS"),
            "BICEPS" to setOf("BICEPS"),
            "TRICEPS" to setOf("TRICEPS"),
            "QUADS" to setOf("QUADS", "QUADRICEPS", "UPPER_LEGS"),
            "HAMSTRINGS" to setOf("HAMSTRINGS", "UPPER_LEGS"),
            "GLUTES" to setOf("GLUTES", "UPPER_LEGS"),
            "CALVES" to setOf("CALVES", "LOWER_LEGS"),
            "UPPER_BACK" to setOf("UPPER_BACK", "LATS", "BACK"),
            "LATS" to setOf("LATS", "UPPER_BACK"),
            "FOREARMS" to setOf("FOREARMS", "LOWER_ARMS"),
            "ARMS" to setOf("ARMS", "BICEPS", "TRICEPS", "FOREARMS"),
            "LEGS" to setOf("LEGS", "QUADS", "HAMSTRINGS", "GLUTES", "CALVES", "UPPER_LEGS", "LOWER_LEGS"),
            "CARDIO" to setOf("CARDIO", "CARDIOVASCULAR_SYSTEM")
        )
    }
}

data class ExercisePickerUiState(
    val exercises: List<ExerciseDetailEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedMuscles: Set<String> = emptySet(),
    val selectedEquipment: Set<String> = emptySet(),
    val addedIds: Set<String> = emptySet(),
    val isSyncing: Boolean = false,
    val syncStatus: String = ""
)
