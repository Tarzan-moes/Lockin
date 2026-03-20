package com.lockin.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import com.lockin.app.data.repository.CalendarRepository
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
    private val repository: CalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        seedWorkoutPlans()
        observeTemplates()
        loadSchedulesForMonth(YearMonth.now())
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
                userId = 1L,
                workoutTemplateId = templateId,
                scheduledDate = date,
                isCompleted = false
            )
            repository.insertSchedule(schedule)
            loadSchedulesForMonth(YearMonth.from(date))
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
            loadSchedulesForMonth(YearMonth.from(_uiState.value.selectedDate))
        }
    }

    fun completeSchedule(scheduleId: Long) {
        viewModelScope.launch {
            repository.setScheduleCompleted(scheduleId, true)
        }
    }

    private fun observeTemplates() {
        viewModelScope.launch {
            repository.getWorkoutPlans().collect { templates ->
                _uiState.update { it.copy(availableTemplates = templates) }
            }
        }
    }

    private fun loadSchedulesForMonth(yearMonth: YearMonth) {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        viewModelScope.launch {
            repository.getSchedulesBetweenDates(start, end).collect { list ->
                val map = list.groupBy { it.scheduledDate }
                _uiState.update { it.copy(schedules = map) }
            }
        }
    }

    /**
     * Seeds 12 default workout plan templates the first time the app runs.
     * Uses [CalendarRepository.workoutPlanCount] to check if seeding is needed;
     * insertWorkoutPlan uses REPLACE conflict strategy so it is idempotent.
     */
    private fun seedWorkoutPlans() {
        viewModelScope.launch {
            if (repository.workoutPlanCount() == 0) {
                DEFAULT_WORKOUT_PLANS.forEach { repository.insertWorkoutPlan(it) }
            }
        }
    }

    companion object {
        val DEFAULT_WORKOUT_PLANS = listOf(
            WorkoutPlanEntity(
                id = "pushups_beginner",
                name = "Push-Up Blast",
                description = "Bodyweight chest and tricep builder",
                goal = "Build upper-body strength",
                estimatedDurationMinutes = 20,
                difficulty = "BEGINNER",
                type = "STRENGTH",
                caloriesBurned = 150
            ),
            WorkoutPlanEntity(
                id = "squat_beginner",
                name = "Squat Foundation",
                description = "Lower-body strength basics",
                goal = "Build leg strength",
                estimatedDurationMinutes = 25,
                difficulty = "BEGINNER",
                type = "STRENGTH",
                caloriesBurned = 180
            ),
            WorkoutPlanEntity(
                id = "hiit_beginner",
                name = "HIIT Starter",
                description = "Short high-intensity intervals",
                goal = "Burn fat and improve cardio",
                estimatedDurationMinutes = 20,
                difficulty = "BEGINNER",
                type = "CARDIO",
                caloriesBurned = 250
            ),
            WorkoutPlanEntity(
                id = "push_day_intermediate",
                name = "Push Day",
                description = "Chest, shoulders, and triceps",
                goal = "Hypertrophy – push muscles",
                estimatedDurationMinutes = 55,
                difficulty = "INTERMEDIATE",
                type = "STRENGTH",
                caloriesBurned = 320
            ),
            WorkoutPlanEntity(
                id = "pull_day_intermediate",
                name = "Pull Day",
                description = "Back and biceps",
                goal = "Hypertrophy – pull muscles",
                estimatedDurationMinutes = 55,
                difficulty = "INTERMEDIATE",
                type = "STRENGTH",
                caloriesBurned = 310
            ),
            WorkoutPlanEntity(
                id = "leg_day_intermediate",
                name = "Leg Day",
                description = "Quads, hamstrings, and glutes",
                goal = "Lower-body hypertrophy",
                estimatedDurationMinutes = 60,
                difficulty = "INTERMEDIATE",
                type = "STRENGTH",
                caloriesBurned = 380
            ),
            WorkoutPlanEntity(
                id = "full_body_intermediate",
                name = "Full Body",
                description = "All major muscle groups",
                goal = "Balanced full-body workout",
                estimatedDurationMinutes = 60,
                difficulty = "INTERMEDIATE",
                type = "STRENGTH",
                caloriesBurned = 400
            ),
            WorkoutPlanEntity(
                id = "run_5k",
                name = "5K Run",
                description = "Steady-state aerobic run",
                goal = "Improve cardiovascular endurance",
                estimatedDurationMinutes = 30,
                difficulty = "INTERMEDIATE",
                type = "CARDIO",
                caloriesBurned = 300
            ),
            WorkoutPlanEntity(
                id = "yoga_flexibility",
                name = "Yoga & Flexibility",
                description = "Mobility and stretching routine",
                goal = "Improve flexibility and recovery",
                estimatedDurationMinutes = 45,
                difficulty = "BEGINNER",
                type = "FLEXIBILITY",
                caloriesBurned = 120
            ),
            WorkoutPlanEntity(
                id = "hiit_advanced",
                name = "Advanced HIIT",
                description = "High-intensity circuit training",
                goal = "Max calorie burn and conditioning",
                estimatedDurationMinutes = 40,
                difficulty = "ADVANCED",
                type = "CARDIO",
                caloriesBurned = 500
            ),
            WorkoutPlanEntity(
                id = "upper_body_advanced",
                name = "Upper Body Power",
                description = "Heavy compound upper-body lifts",
                goal = "Build maximal upper-body strength",
                estimatedDurationMinutes = 70,
                difficulty = "ADVANCED",
                type = "STRENGTH",
                caloriesBurned = 420
            ),
            WorkoutPlanEntity(
                id = "core_stability",
                name = "Core & Stability",
                description = "Planks, rotations, and anti-rotation work",
                goal = "Strengthen the core and improve posture",
                estimatedDurationMinutes = 30,
                difficulty = "INTERMEDIATE",
                type = "STRENGTH",
                caloriesBurned = 160
            )
        )
    }
}


