package com.lockin.app.presentation.habits

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lockin.app.domain.model.Habit
import javax.inject.Inject

/**
 * HabitsViewModel – Manages the daily habit checklist.
 */
@HiltViewModel
class HabitsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()
}

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false
)

