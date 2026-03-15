package com.lockin.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.repository.ExerciseLibraryRepository
import com.lockin.app.data.repository.WorkoutBuilderRepository
import com.lockin.app.domain.usecase.CalculateStreakUseCase
import com.lockin.app.domain.usecase.StreakResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkoutHomeViewModel @Inject constructor(
    private val repo: WorkoutBuilderRepository,
    private val exerciseLibrary: ExerciseLibraryRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutHomeUiState())
    val uiState: StateFlow<WorkoutHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ensure exercise library has at least defaults
            exerciseLibrary.seedIfEmpty()
        }
        viewModelScope.launch {
            repo.observeAllWorkouts().collect { workouts ->
                _uiState.update { it.copy(workouts = workouts, isLoading = false) }
            }
        }
        viewModelScope.launch {
            val streak = runCatching { calculateStreakUseCase.calculate() }.getOrDefault(StreakResult())
            _uiState.update { it.copy(streak = streak) }
        }
    }

    fun createWorkout(name: String, goal: String): String {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            repo.saveWorkout(
                WorkoutEntity(
                    id = id,
                    name = name.ifBlank { "Workout ${LocalDate.now()}" },
                    goal = goal,
                    type = "STRENGTH",
                    plannedDate = LocalDate.now()
                )
            )
        }
        return id
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch { repo.deleteWorkout(id) }
    }
}

data class WorkoutHomeUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val isLoading: Boolean = true,
    val streak: StreakResult = StreakResult()
)

