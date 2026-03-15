package com.lockin.app.presentation.exercise

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lockin.app.domain.model.Exercise
import javax.inject.Inject

/**
 * ExerciseLibraryViewModel – Manages the exercise browsing / search screen.
 */
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()
}

data class ExerciseLibraryUiState(
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

