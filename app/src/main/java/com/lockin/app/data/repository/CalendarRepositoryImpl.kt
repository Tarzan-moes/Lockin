package com.lockin.app.data.repository

import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.data.local.database.dao.WorkoutScheduleDao
import com.lockin.app.data.local.models.WorkoutPlanEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import com.lockin.app.presentation.calendar.WorkoutScheduleUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val scheduleDao: WorkoutScheduleDao
) : CalendarRepository {

    override suspend fun getWorkoutPlans(): List<WorkoutPlanEntity> =
        workoutDao.getAllWorkoutPlans()

    override fun getSchedules(date: LocalDate): Flow<List<WorkoutScheduleUi>> =
        scheduleDao.getSchedulesForDate(date).map { entities ->
            joinWithPlans(entities)
        }

    override fun getSchedulesForMonth(
        start: LocalDate,
        end: LocalDate
    ): Flow<Map<LocalDate, List<WorkoutScheduleUi>>> =
        scheduleDao.getSchedulesBetweenDates(start, end).map { entities ->
            joinWithPlans(entities).groupBy { it.date }
        }

    override suspend fun scheduleWorkout(date: LocalDate, planId: String, notes: String?): Long =
        scheduleDao.insertSchedule(
            WorkoutScheduleEntity(
                userId = 1L, // Hardcoded for single-user app; replace with injected user session when multi-user support is added
                workoutTemplateId = planId,
                scheduledDate = date,
                notes = notes
            )
        )

    override suspend fun deleteSchedule(id: Long) {
        scheduleDao.deleteSchedule(id)
    }

    override suspend fun completeSchedule(id: Long) {
        scheduleDao.markComplete(id, System.currentTimeMillis())
    }

    override suspend fun seedWorkoutPlansIfEmpty() {
        if (workoutDao.countWorkoutPlans() == 0) {
            workoutDao.insertWorkoutPlans(DEFAULT_WORKOUT_PLANS)
        }
    }

    private suspend fun joinWithPlans(schedules: List<WorkoutScheduleEntity>): List<WorkoutScheduleUi> {
        if (schedules.isEmpty()) return emptyList()
        val plans = workoutDao.getAllWorkoutPlans().associateBy { it.id }
        return schedules.mapNotNull { schedule ->
            plans[schedule.workoutTemplateId]?.let { plan ->
                WorkoutScheduleUi(
                    id = schedule.id,
                    date = schedule.scheduledDate,
                    workoutTemplateId = schedule.workoutTemplateId,
                    workoutName = plan.name,
                    isCompleted = schedule.isCompleted,
                    completedAt = schedule.completedAt,
                    notes = schedule.notes,
                    durationMinutes = plan.estimatedDurationMinutes,
                    difficulty = plan.difficulty
                )
            }
        }
    }

    companion object {
        val DEFAULT_WORKOUT_PLANS = listOf(
            WorkoutPlanEntity("pushups_basic", "Push-ups", "Classic bodyweight chest and tricep exercise", 20, "Beginner", "Strength", 150, "Bodyweight"),
            WorkoutPlanEntity("squats_basic", "Bodyweight Squats", "Fundamental lower-body movement", 25, "Beginner", "Strength", 180, "Bodyweight"),
            WorkoutPlanEntity("run_5km", "5K Run", "Steady-state outdoor or treadmill run", 30, "Intermediate", "Cardio", 300, "None"),
            WorkoutPlanEntity("deadlift_advanced", "Deadlifts", "Compound posterior-chain strength builder", 45, "Advanced", "Strength", 400, "Barbell"),
            WorkoutPlanEntity("yoga_morning", "Morning Yoga", "Full-body flexibility and mindfulness", 30, "Beginner", "Yoga", 120, "Bodyweight"),
            WorkoutPlanEntity("hiit_20min", "20-Min HIIT", "High-intensity interval cardio circuit", 20, "Intermediate", "Cardio", 280, "Bodyweight"),
            WorkoutPlanEntity("bench_press", "Bench Press", "Horizontal pushing for chest strength", 40, "Intermediate", "Strength", 320, "Barbell"),
            WorkoutPlanEntity("pull_ups", "Pull-ups", "Upper back and bicep pulling compound", 20, "Intermediate", "Strength", 160, "Pull-up Bar"),
            WorkoutPlanEntity("cycling_45min", "45-Min Cycling", "Low-impact cardio endurance session", 45, "Beginner", "Cardio", 350, "Bike"),
            WorkoutPlanEntity("overhead_press", "Overhead Press", "Shoulder strength and core stability", 35, "Intermediate", "Strength", 250, "Barbell"),
            WorkoutPlanEntity("plank_core", "Plank Core Workout", "Isometric core-strength routine", 20, "Beginner", "Strength", 130, "Bodyweight"),
            WorkoutPlanEntity("row_dumbbell", "Dumbbell Rows", "Unilateral back thickness builder", 30, "Intermediate", "Strength", 220, "Dumbbells"),
            WorkoutPlanEntity("lunges_lower", "Walking Lunges", "Unilateral lower-body strength and balance", 25, "Beginner", "Strength", 190, "Bodyweight"),
            WorkoutPlanEntity("sprint_intervals", "Sprint Intervals", "Short burst speed and power training", 25, "Advanced", "Cardio", 340, "None"),
            WorkoutPlanEntity("stretch_recovery", "Full-Body Stretch", "Recovery and mobility session", 20, "Beginner", "Yoga", 80, "Bodyweight")
        )
    }
}
