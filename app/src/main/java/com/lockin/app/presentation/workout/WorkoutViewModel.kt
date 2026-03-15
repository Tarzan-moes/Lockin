package com.lockin.app.presentation.workout

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * WorkoutViewModel – Manages active workout execution state.
 *
 * Tracks the current exercise, set number, timer, and logged entries.
 * Full implementation in Part 2 when we build the workout execution screen.
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
}

data class WorkoutUiState(
    val isActive: Boolean = false,
    val currentExerciseName: String = "",
    val currentSetNumber: Int = 0,
    val elapsedSeconds: Long = 0L,
    val isResting: Boolean = false
)

