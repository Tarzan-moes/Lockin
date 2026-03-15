package com.lockin.app.presentation.progress

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lockin.app.domain.model.ProgressEntry
import javax.inject.Inject

/**
 * ProgressViewModel – Manages the progress review / charts screen.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
}

data class ProgressUiState(
    val entries: List<ProgressEntry> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalPRs: Int = 0,
    val isLoading: Boolean = false
)

