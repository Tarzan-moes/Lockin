package com.lockin.app.presentation.calendar

import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var repository: CalendarRepository

    private lateinit var viewModel: CalendarViewModel

    private val sampleDate = LocalDate.of(2025, 3, 15)

    private val sampleSchedules: Map<LocalDate, List<WorkoutScheduleUi>> = mapOf(
        sampleDate to listOf(
            WorkoutScheduleUi(
                id = 1L,
                date = sampleDate,
                workoutTemplateId = "pushups_basic",
                workoutName = "Push-ups",
                isCompleted = false,
                completedAt = null,
                notes = null,
                durationMinutes = 20,
                difficulty = "Beginner"
            )
        )
    )

    private val samplePlans = listOf(
        WorkoutPlanEntity(
            id = "pushups_basic",
            name = "Push-ups",
            description = "Classic bodyweight exercise",
            estimatedDurationMinutes = 20,
            difficulty = "Beginner",
            type = "Strength",
            caloriesBurned = 150,
            equipment = "Bodyweight"
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        val monthStart = YearMonth.now().atDay(1)
        val monthEnd = YearMonth.now().atEndOfMonth()
        `when`(repository.getSchedulesForMonth(monthStart, monthEnd))
            .thenReturn(MutableStateFlow(sampleSchedules))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init seeds workout plans`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        verify(repository).seedWorkoutPlansIfEmpty()
    }

    @Test
    fun `init loads templates into uiState`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        assertEquals(samplePlans, viewModel.uiState.value.availableTemplates)
    }

    @Test
    fun `initial selectedDate is today`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(emptyList())

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `onDateSelected updates selectedDate in uiState`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(emptyList())

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.onDateSelected(sampleDate)

        assertEquals(sampleDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `scheduleWorkout calls repository and clears error`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)
        `when`(repository.scheduleWorkout(sampleDate, "pushups_basic", null)).thenReturn(1L)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.scheduleWorkout("pushups_basic", sampleDate)
        advanceUntilIdle()

        verify(repository).scheduleWorkout(sampleDate, "pushups_basic", null)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `deleteSchedule calls repository`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteSchedule(1L)
        advanceUntilIdle()

        verify(repository).deleteSchedule(1L)
    }

    @Test
    fun `completeSchedule calls repository`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.completeSchedule(1L)
        advanceUntilIdle()

        verify(repository).completeSchedule(1L)
    }

    @Test
    fun `uiState schedules reflect repository flow`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        assertEquals(sampleSchedules, viewModel.uiState.value.schedules)
    }

    @Test
    fun `scheduleWorkout sets error on failure`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)
        `when`(repository.scheduleWorkout(sampleDate, "pushups_basic", null))
            .thenThrow(RuntimeException("DB error"))

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.scheduleWorkout("pushups_basic", sampleDate)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Failed to schedule"))
    }

    @Test
    fun `clearError removes error from uiState`() = runTest {
        `when`(repository.getWorkoutPlans()).thenReturn(samplePlans)
        `when`(repository.scheduleWorkout(sampleDate, "pushups_basic", null))
            .thenThrow(RuntimeException("DB error"))

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.scheduleWorkout("pushups_basic", sampleDate)
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
