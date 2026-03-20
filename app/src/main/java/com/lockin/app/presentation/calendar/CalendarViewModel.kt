package com.lockin.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedWorkoutPlansIfEmpty()
            loadTemplates()
            loadSchedulesForMonth(YearMonth.now())
        }
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        loadSchedulesForMonth(yearMonth)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun scheduleWorkout(templateId: String, date: LocalDate) {
        viewModelScope.launch {
            try {
                repository.scheduleWorkout(date, templateId)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to schedule: ${e.message}") }
            }
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSchedule(scheduleId)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun completeSchedule(scheduleId: Long) {
        viewModelScope.launch {
            try {
                repository.completeSchedule(scheduleId)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to mark complete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        viewModelScope.launch {
            repository.getSchedulesForMonth(start, end).collect { map ->
                _uiState.update { it.copy(schedules = map) }
            }
        }
    }

    private suspend fun loadTemplates() {
        try {
            val templates = repository.getWorkoutPlans()
            _uiState.update { it.copy(availableTemplates = templates) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to load workouts: ${e.message}") }
        }
    }
}
