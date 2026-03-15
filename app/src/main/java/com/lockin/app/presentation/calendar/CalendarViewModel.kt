package com.lockin.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.data.local.database.dao.WorkoutScheduleDao
import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val schedules: Map<LocalDate, List<WorkoutScheduleEntity>> = emptyMap(),
    val availableTemplates: List<WorkoutPlanEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val scheduleDao: WorkoutScheduleDao,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadSchedulesForMonth(YearMonth.now())
        loadTemplates()
    }

    fun onMonthChanged(yearMonth: YearMonth) {
        loadSchedulesForMonth(yearMonth)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun scheduleWorkout(templateId: String, date: LocalDate) {
        viewModelScope.launch {
            val schedule = WorkoutScheduleEntity(
                userId = 1L, // Hardcoded for single user
                workoutTemplateId = templateId,
                scheduledDate = date,
                isCompleted = false
            )
            scheduleDao.insertSchedule(schedule)
            // Reload simple refresh - optimizing flow observation would be better but this is fine
            loadSchedulesForMonth(YearMonth.from(date)) 
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            scheduleDao.deleteSchedule(scheduleId)
            loadSchedulesForMonth(YearMonth.from(_uiState.value.selectedDate))
        }
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        viewModelScope.launch {
            scheduleDao.getSchedulesBetweenDates(start, end).collect { list ->
                val map = list.groupBy { it.scheduledDate }
                _uiState.update { it.copy(schedules = map) }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            // Assuming getAllWorkoutPlans exists or similar
            // It seems getAllWorkoutPlans returns Flow<List<WorkoutPlanEntity>> in WorkoutDao
            workoutDao.getAllWorkoutPlans().collect { templates ->
                _uiState.update { it.copy(availableTemplates = templates) }
            }
        }
    }
}

