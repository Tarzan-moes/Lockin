package com.lockin.app.presentation.calendar

import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import com.lockin.app.data.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // ── Fake repository ───────────────────────────────────────────────────

    private class FakeCalendarRepository : CalendarRepository {
        val plans = mutableListOf<WorkoutPlanEntity>()
        val schedules = mutableListOf<WorkoutScheduleEntity>()
        private var nextId = 1L

        private val plansFlow = MutableStateFlow<List<WorkoutPlanEntity>>(emptyList())
        private val schedulesFlow = MutableStateFlow<List<WorkoutScheduleEntity>>(emptyList())

        override fun getWorkoutPlans(): Flow<List<WorkoutPlanEntity>> = plansFlow

        override fun getSchedulesForDate(date: LocalDate): Flow<List<WorkoutScheduleEntity>> =
            MutableStateFlow(schedules.filter { it.scheduledDate == date })

        override fun getSchedulesBetweenDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<WorkoutScheduleEntity>> = schedulesFlow

        override suspend fun insertSchedule(schedule: WorkoutScheduleEntity): Long {
            val withId = schedule.copy(id = nextId++)
            schedules.add(withId)
            schedulesFlow.value = schedules.toList()
            return withId.id
        }

        override suspend fun deleteSchedule(id: Long) {
            schedules.removeAll { it.id == id }
            schedulesFlow.value = schedules.toList()
        }

        override suspend fun setScheduleCompleted(id: Long, isCompleted: Boolean) {
            val idx = schedules.indexOfFirst { it.id == id }
            if (idx >= 0) {
                schedules[idx] = schedules[idx].copy(isCompleted = isCompleted)
                schedulesFlow.value = schedules.toList()
            }
        }

        override suspend fun insertWorkoutPlan(plan: WorkoutPlanEntity) {
            plans.removeAll { it.id == plan.id }
            plans.add(plan)
            plansFlow.value = plans.toList()
        }

        override suspend fun workoutPlanCount(): Int = plans.size
    }

    // ── Test setup / teardown ─────────────────────────────────────────────

    private lateinit var fakeRepo: FakeCalendarRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCalendarRepository()
        viewModel = CalendarViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `init seeds 12 default workout plans when repository is empty`() = runTest {
        advanceUntilIdle()
        assertEquals(12, fakeRepo.plans.size)
    }

    @Test
    fun `init does not seed plans when repository already has plans`() = runTest {
        advanceUntilIdle()
        val countAfterFirstInit = fakeRepo.plans.size

        // Create a second ViewModel against the same (now non-empty) repo
        val vm2 = CalendarViewModel(fakeRepo)
        advanceUntilIdle()

        assertEquals(countAfterFirstInit, fakeRepo.plans.size)
    }

    @Test
    fun `availableTemplates reflects seeded plans`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.first()
        assertEquals(12, state.availableTemplates.size)
    }

    @Test
    fun `onDateSelected updates selectedDate in state`() = runTest {
        advanceUntilIdle()
        val targetDate = LocalDate.of(2025, 6, 15)
        viewModel.onDateSelected(targetDate)
        advanceUntilIdle()
        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `scheduleWorkout inserts a schedule into repository`() = runTest {
        advanceUntilIdle()
        val date = LocalDate.of(2025, 6, 10)
        viewModel.scheduleWorkout("pushups_beginner", date)
        advanceUntilIdle()
        assertTrue(fakeRepo.schedules.any { it.workoutTemplateId == "pushups_beginner" && it.scheduledDate == date })
    }

    @Test
    fun `deleteSchedule removes schedule from repository`() = runTest {
        advanceUntilIdle()
        val date = LocalDate.of(2025, 6, 10)
        val id = fakeRepo.insertSchedule(
            WorkoutScheduleEntity(userId = 1L, workoutTemplateId = "squat_beginner", scheduledDate = date)
        )
        advanceUntilIdle()
        viewModel.deleteSchedule(id)
        advanceUntilIdle()
        assertTrue(fakeRepo.schedules.none { it.id == id })
    }

    @Test
    fun `completeSchedule marks schedule as completed`() = runTest {
        advanceUntilIdle()
        val date = LocalDate.of(2025, 6, 10)
        val id = fakeRepo.insertSchedule(
            WorkoutScheduleEntity(userId = 1L, workoutTemplateId = "run_5k", scheduledDate = date)
        )
        advanceUntilIdle()
        viewModel.completeSchedule(id)
        advanceUntilIdle()
        assertTrue(fakeRepo.schedules.first { it.id == id }.isCompleted)
    }
}
