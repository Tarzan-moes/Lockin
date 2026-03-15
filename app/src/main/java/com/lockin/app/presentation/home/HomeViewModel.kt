package com.lockin.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.*
import com.lockin.app.domain.usecase.GetDailyDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeViewModel – Drives the Home / Dashboard screen.
 *
 * Exposes a single [HomeUiState] flow that the composable observes.
 * Uses [GetDailyDashboardDataUseCase] to load the energy score,
 * today's workout, habits, and recent PRs in one coordinated call.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDailyDashboardData: GetDailyDashboardDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val data = getDailyDashboardData()
                _uiState.value = HomeUiState(
                    isLoading = false,
                    energyScore = data.energyScore,
                    todayWorkout = data.todayWorkout,
                    habits = data.habits
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}

/** Immutable UI state for the Home screen. */
data class HomeUiState(
    val isLoading: Boolean = false,
    val energyScore: EnergyScore = EnergyScore(),
    val todayWorkout: WorkoutPlan? = null,
    val habits: List<Habit> = emptyList(),
    val error: String? = null
)

