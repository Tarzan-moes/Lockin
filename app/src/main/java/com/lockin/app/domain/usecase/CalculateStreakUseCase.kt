package com.lockin.app.domain.usecase

import com.lockin.app.data.local.database.dao.WorkoutEntityDao
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class StreakResult(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastWorkoutDate: LocalDate? = null
)

/**
 * Calculates the current and longest workout streaks based on completed workouts.
 *
 * A "worked out" day is any day where at least one WorkoutEntity has status COMPLETED
 * and a non-null startTime whose date matches that day.
 */
class CalculateStreakUseCase @Inject constructor(
    private val workoutEntityDao: WorkoutEntityDao
) {
    suspend fun calculate(): StreakResult {
        // 1. Collect all completed workout LocalDateTimes
        val dateTimes = workoutEntityDao.getCompletedWorkoutDateTimes().first()

        if (dateTimes.isEmpty()) {
            return StreakResult()
        }

        // 2. Convert to distinct LocalDates, sorted descending
        val distinctDatesDesc = dateTimes
            .map { it.toLocalDate() }
            .distinct()
            .sortedDescending()

        val lastWorkoutDate = distinctDatesDesc.first()

        // 3. Current streak: walk backwards from today
        val today = LocalDate.now()
        val dateSet = distinctDatesDesc.toSet()
        var currentStreak = 0
        var checkDate = today

        while (checkDate in dateSet) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // If today has no workout yet, check if yesterday continues the streak
        // (user might not have worked out yet today but has a streak going)
        if (currentStreak == 0 && today.minusDays(1) in dateSet) {
            checkDate = today.minusDays(1)
            while (checkDate in dateSet) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        // 4. Longest streak: walk all dates sorted ascending
        val distinctDatesAsc = distinctDatesDesc.sortedAscending()
        var longestStreak = 1
        var runLength = 1

        for (i in 1 until distinctDatesAsc.size) {
            val daysBetween = ChronoUnit.DAYS.between(distinctDatesAsc[i - 1], distinctDatesAsc[i])
            if (daysBetween == 1L) {
                runLength++
                if (runLength > longestStreak) longestStreak = runLength
            } else {
                runLength = 1
            }
        }

        // Ensure longestStreak is at least as big as currentStreak
        if (currentStreak > longestStreak) longestStreak = currentStreak

        return StreakResult(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            lastWorkoutDate = lastWorkoutDate
        )
    }

    private fun List<LocalDate>.sortedAscending(): List<LocalDate> = this.sorted()
}

