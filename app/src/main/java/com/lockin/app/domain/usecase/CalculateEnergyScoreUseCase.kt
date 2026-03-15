package com.lockin.app.domain.usecase

import android.util.Log
import com.lockin.app.data.external.HealthConnectManager
import com.lockin.app.domain.model.EnergyScore
import com.lockin.app.domain.model.WorkoutSession
import java.time.LocalDate
import javax.inject.Inject

/**
 * CalculateEnergyScoreUseCase — Computes a composite 0-100 energy/readiness
 * score from Health Connect data (sleep, HRV, resting HR) and recent training load.
 *
 * Weights:
 *  - Sleep quality: 35%
 *  - HRV: 30%
 *  - Training load (inverse): 25%
 *  - Stress / soreness (self-reported): 10%
 */
class CalculateEnergyScoreUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    companion object {
        private const val TAG = "CalcEnergyScore"
    }

    suspend fun calculate(
        userId: String,
        date: LocalDate = LocalDate.now(),
        recentWorkouts: List<WorkoutSession> = emptyList(),
        userStressLevel: Int? = null,  // 1 (low) to 10 (extreme)
        userSoreness: Int? = null      // 1 (none) to 10 (severe)
    ): EnergyScore {
        // ── Read health data ─────────────────────────────────────────────
        val sleepData = try {
            healthConnectManager.getSleepForDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Sleep read failed: ${e.message}")
            null
        }

        val hrvValue = try {
            healthConnectManager.getHRVForDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "HRV read failed: ${e.message}")
            null
        }

        val restingHR = try {
            healthConnectManager.getRestingHeartRateForDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Resting HR read failed: ${e.message}")
            null
        }

        // ── Component scores (each 0 – 100) ─────────────────────────────

        val sleepScore = calculateSleepScore(
            totalMinutes = sleepData?.totalMinutes,
            sleepQuality = sleepData?.sleepQuality,
            deepPercent = if (sleepData != null && sleepData.totalMinutes > 0)
                sleepData.deepSleepMinutes.toFloat() / sleepData.totalMinutes else null
        )

        val hrvScore = calculateHRVScore(hrvValue)

        val trainingLoadScore = calculateTrainingLoadScore(recentWorkouts)

        val stressScore = calculateStressScore(userStressLevel, userSoreness)

        // ── Weighted average ─────────────────────────────────────────────
        val overallScore = (
                sleepScore * 0.35f +
                        hrvScore * 0.30f +
                        trainingLoadScore * 0.25f +
                        stressScore * 0.10f
                ).toInt().coerceIn(0, 100)

        val label = when {
            overallScore >= 85 -> "Excellent"
            overallScore >= 70 -> "Good"
            overallScore >= 50 -> "Moderate"
            overallScore >= 30 -> "Low"
            else -> "Very Low"
        }

        val recommendation = generateRecommendation(
            overallScore, sleepScore.toInt(), hrvScore.toInt(),
            trainingLoadScore.toInt(), recentWorkouts.size
        )

        Log.i(TAG, "Energy: $overallScore ($label) | " +
                "sleep=$sleepScore hrv=$hrvScore load=$trainingLoadScore stress=$stressScore")

        return EnergyScore(
            score = overallScore,
            label = label,
            sleepQualityPercent = sleepScore.toInt(),
            hrvScore = hrvScore.toInt(),
            recoveryPercent = trainingLoadScore.toInt()
        )
    }

    // ── Scoring functions ────────────────────────────────────────────────

    private fun calculateSleepScore(
        totalMinutes: Int?,
        sleepQuality: Float?,
        deepPercent: Float?
    ): Float {
        if (totalMinutes == null) return 50f // neutral when no data

        val durationScore = when {
            totalMinutes in 420..540 -> 100f   // 7-9h optimal
            totalMinutes in 360..419 -> 75f    // 6-7h acceptable
            totalMinutes in 300..359 -> 50f    // 5-6h below average
            totalMinutes > 540 -> 80f          // oversleep
            else -> 25f                        // <5h poor
        }

        val qualityScore = (sleepQuality ?: 0.5f) * 100f
        val deepScore = when {
            deepPercent == null -> 50f
            deepPercent >= 0.20f -> 100f  // 20%+ deep sleep is excellent
            deepPercent >= 0.15f -> 75f
            deepPercent >= 0.10f -> 50f
            else -> 25f
        }

        return (durationScore * 0.4f + qualityScore * 0.35f + deepScore * 0.25f)
            .coerceIn(0f, 100f)
    }

    private fun calculateHRVScore(hrv: Float?): Float {
        if (hrv == null) return 50f // neutral when no data
        return when {
            hrv >= 80f -> 100f
            hrv >= 60f -> 85f
            hrv >= 45f -> 70f
            hrv >= 30f -> 50f
            hrv >= 20f -> 35f
            else -> 20f
        }
    }

    private fun calculateTrainingLoadScore(recentWorkouts: List<WorkoutSession>): Float {
        // Count workouts in last 7 days
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentCount = recentWorkouts.count { it.startTime >= sevenDaysAgo }

        // More workouts = higher load = lower recovery score (inverted)
        return when {
            recentCount == 0 -> 95f     // Fully rested
            recentCount <= 2 -> 85f     // Light week
            recentCount <= 4 -> 70f     // Moderate
            recentCount <= 5 -> 55f     // Heavy
            recentCount <= 6 -> 40f     // Very heavy
            else -> 25f                 // Overreaching
        }
    }

    private fun calculateStressScore(stressLevel: Int?, soreness: Int?): Float {
        // Invert: low stress/soreness = high score
        val stressComponent = if (stressLevel != null) {
            ((11 - stressLevel.coerceIn(1, 10)) / 10f * 100f)
        } else 60f // neutral

        val sorenessComponent = if (soreness != null) {
            ((11 - soreness.coerceIn(1, 10)) / 10f * 100f)
        } else 60f // neutral

        return (stressComponent * 0.5f + sorenessComponent * 0.5f).coerceIn(0f, 100f)
    }

    // ── Recommendation ───────────────────────────────────────────────────

    private fun generateRecommendation(
        overall: Int,
        sleepScore: Int,
        hrvScore: Int,
        loadScore: Int,
        workoutCount: Int
    ): String {
        return when {
            overall >= 85 ->
                "You're in great shape! Push hard today — your body is fully recovered."
            overall >= 70 && sleepScore < 60 ->
                "Your recovery is solid but sleep was lacking. Prioritize rest tonight."
            overall >= 70 ->
                "Good readiness. You can train at normal intensity today."
            overall >= 50 && loadScore < 40 ->
                "Training load is high. Consider a lighter session or active recovery."
            overall >= 50 && hrvScore < 40 ->
                "HRV is below normal. Keep today's session moderate and focus on recovery."
            overall >= 50 ->
                "Moderate readiness. Listen to your body and scale intensity as needed."
            overall >= 30 ->
                "Low energy. A deload day or light mobility work would serve you well."
            else ->
                "Very low readiness. Rest day recommended. Focus on sleep, nutrition, and hydration."
        }
    }
}

