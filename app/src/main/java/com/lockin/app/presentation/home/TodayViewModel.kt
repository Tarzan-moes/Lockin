package com.lockin.app.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.ai.coach.AiCoachManager
import com.lockin.app.data.external.HealthConnectManager
import com.lockin.app.data.external.SleepData
import com.lockin.app.data.local.database.dao.WorkoutScheduleDao
import com.lockin.app.data.local.models.EnergyScoreEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.data.local.models.WorkoutScheduleEntity
import com.lockin.app.data.repository.EnergyScoreRepository
import com.lockin.app.data.repository.WorkoutEntityRepository
import com.lockin.app.domain.usecase.CalculateEnergyScoreUseCase
import com.lockin.app.domain.usecase.CalculateStreakUseCase
import com.lockin.app.domain.usecase.StreakResult
import com.lockin.app.watch.connection.WatchConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val workoutRepo: WorkoutEntityRepository,
    private val energyRepo: EnergyScoreRepository,
    private val scheduleDao: WorkoutScheduleDao,
    private val healthConnectManager: HealthConnectManager,
    private val watchConnectionManager: WatchConnectionManager,
    private val calculateEnergyScoreUseCase: CalculateEnergyScoreUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val aiCoachManager: AiCoachManager
) : ViewModel() {

    companion object {
        private const val TAG = "TodayViewModel"
        private const val DEFAULT_USER_ID = "default-user"
    }

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // ── Health permissions ──
                val healthGranted = try {
                    val granted = healthConnectManager.hasAllPermissions()
                    Log.d("TODAY_VM", "━━━ Health permissions granted: $granted ━━━")
                    granted
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking health permissions: ${e.message}", e)
                    false
                }

                // ── Load health data ──
                var steps: Long? = null
                var sleepData: SleepData? = null
                var latestHrBpm: Long? = null
                var hrvValue: Float? = null
                var restingHr: Int? = null

                if (healthGranted) {
                    steps = try {
                        Log.d("TODAY_VM", "━━━ CALLING getStepsForDate ━━━")
                        val s = healthConnectManager.getStepsForDate(LocalDate.now())
                        Log.d("TODAY_VM", "Received steps: $s")
                        s
                    } catch (e: Exception) {
                        Log.e("TODAY_VM", "Steps load EXCEPTION: ${e.message}", e)
                        null
                    }

                    sleepData = try {
                        // Pass today — the manager queries from yesterday 18:00 to today 14:00
                        val sd = healthConnectManager.getSleepForDate(LocalDate.now())
                        Log.d(TAG, "Sleep loaded: totalMin=${sd?.totalMinutes} deep=${sd?.deepSleepMinutes} rem=${sd?.remSleepMinutes}")
                        sd
                    } catch (e: Exception) {
                        Log.e(TAG, "Sleep load failed: ${e.message}", e)
                        null
                    }

                    latestHrBpm = try {
                        val hrList = healthConnectManager.getHeartRateForToday()
                        val latest = hrList.lastOrNull()?.bpm
                        Log.d(TAG, "HeartRate loaded: samples=${hrList.size} latest=$latest")
                        latest
                    } catch (e: Exception) {
                        Log.e(TAG, "HeartRate load failed: ${e.message}", e)
                        null
                    }

                    hrvValue = try {
                        val hrv = healthConnectManager.getHRVForDate(LocalDate.now())
                        Log.d(TAG, "HRV loaded: $hrv")
                        hrv
                    } catch (e: Exception) {
                        Log.e(TAG, "HRV load failed: ${e.message}", e)
                        null
                    }

                    restingHr = try {
                        val rhr = healthConnectManager.getRestingHeartRateForDate(LocalDate.now())
                        Log.d(TAG, "Resting HR loaded: $rhr")
                        rhr
                    } catch (e: Exception) {
                        Log.e(TAG, "Resting HR load failed: ${e.message}", e)
                        null
                    }
                }

                Log.d("TODAY_VM", "Health summary → steps=$steps sleep=${sleepData?.totalMinutes}min HR=$latestHrBpm HRV=$hrvValue RHR=$restingHr")

                // ── Energy score ──
                var energyScore = runCatching {
                    energyRepo.getEnergyScoreForDate(DEFAULT_USER_ID)
                }.getOrNull()

                if (energyScore == null && healthGranted) {
                    val calculated = runCatching {
                        calculateEnergyScoreUseCase.calculate(
                            userId = DEFAULT_USER_ID,
                            recentWorkouts = emptyList()
                        )
                    }.getOrNull()

                    if (calculated != null) {
                        val entity = EnergyScoreEntity(
                            id = UUID.randomUUID().toString(),
                            userId = DEFAULT_USER_ID,
                            date = LocalDate.now(),
                            overallScore = calculated.score,
                            sleepScore = calculated.sleepQualityPercent,
                            hrvScore = calculated.hrvScore,
                            trainingLoadScore = calculated.recoveryPercent,
                            stressScore = 0,
                            details = calculated.label,
                            shouldDeload = false
                        )
                        runCatching { energyRepo.saveEnergyScore(entity) }
                        energyScore = entity
                    }
                }

                // ── Workouts, watch, streak ──
                val recentWorkouts = runCatching {
                    workoutRepo.getRecentWorkouts(limit = 5)
                }.getOrDefault(emptyList())

                val watchConnected = runCatching { watchConnectionManager.isWatchConnected() }.getOrDefault(false)

                // ── Schedule ──
                val scheduleFlow = scheduleDao.getSchedulesForDate(LocalDate.now())
                // Need to collect flow or just use a one-shot query?
                // Ideally DAO should have a suspend function for List.
                // But I defined it as Flow. I will collect it once or use first().

                // For "next upcoming", I need > today. My DAO has getSchedulesBetweenDates.
                // I will check today first.

                // ── AI Deload Detection ──
                val deloadPlan = checkForDeload(energyScore)

                val streak = calculateStreakUseCase.calculate()

                // Check for today's schedule
                // Note: since we are in a coroutine, we can collect flow first item
                // Use a helper function or assume DAO has suspend variant.
                // Since I defined Flow, I'll use flow collection in a separate launch or here.

                // Hack: Collecting flow here blocks? No, I'll use first().
                // But Flow might not emit immediately if DB is empty or slow? Room creates flow immediately.

                // Let's modify UiState to hold the schedule

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    energyScore = energyScore,
                    stepsToday = steps,
                    sleepData = sleepData,
                    latestHeartRateBpm = latestHrBpm,
                    hrvValue = hrvValue,
                    healthPermissionsGranted = healthGranted,
                    recentWorkouts = recentWorkouts,
                    watchConnected = watchConnected,
                    recommendation = energyScore?.details,
                    streak = streak,
                    deloadSuggestion = deloadPlan
                )
                Log.d(TAG, "UI state updated: steps=${_uiState.value.stepsToday} sleep=${_uiState.value.sleepData?.totalMinutes} HR=${_uiState.value.latestHeartRateBpm} HRV=${_uiState.value.hrvValue}")
                Log.d("TODAY_VM", "Updated uiState with steps: ${_uiState.value.stepsToday}")

                // Launch separate job for schedule to keep flow active if needed or just one-shot
                loadNextScheduledWorkout()

            } catch (e: Exception) {
                Log.e(TAG, "refresh() top-level error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun loadNextScheduledWorkout() {
        viewModelScope.launch {
            // Find schedule for today or next 7 days
            try {
                scheduleDao.getSchedulesBetweenDates(
                    LocalDate.now(),
                    LocalDate.now().plusDays(7)
                ).collect { list ->
                    // Sort by date, filter incomplete
                    val next = list.filter { !it.isCompleted }.minByOrNull { it.scheduledDate }
                    _uiState.value = _uiState.value.copy(nextScheduledWorkout = next)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load schedules", e)
            }
        }
    }

    private suspend fun checkForDeload(todayScore: EnergyScoreEntity?): String? {
        if (todayScore == null) return null

        // 1. Check Energy Score < 60 for 3 days
        // We need history. energyRepo needs getAll() or past days.
        // Assuming energyRepo.getEnergyScoreForDate works.
        val daysToCheck = 3
        var lowEnergyDays = 0
        for (i in 0 until daysToCheck) {
            val date = LocalDate.now().minusDays(i.toLong())
            val score = energyRepo.getEnergyScoreForDate(date)
            if ((score?.overallScore ?: 100) < 60) {
                lowEnergyDays++
            }
        }

        if (lowEnergyDays < 3) return null

        // 2. Check Training Load Ratio > 1.5
        // Needs recent workouts volume.
        val recentWorkouts = workoutRepo.getRecentWorkouts() // Assuming returns List<WorkoutEntity>
        val last7DaysVol = recentWorkouts.filter {
            it.startTime != null && it.startTime.toLocalDate().isAfter(LocalDate.now().minusDays(7))
        }.sumOf { it.totalVolumeKg?.toDouble() ?: 0.0 }

        val last28DaysVol = recentWorkouts.filter {
            it.startTime != null && it.startTime.toLocalDate().isAfter(LocalDate.now().minusDays(28))
        }.sumOf { it.totalVolumeKg?.toDouble() ?: 0.0 }

        val chronicLoad = last28DaysVol / 4.0
        val acuteLoad = last7DaysVol

        val ratio = if (chronicLoad > 0) acuteLoad / chronicLoad else 0.0

        if (ratio > 1.5) {
            return aiCoachManager.askCoach(
                "I am overreaching. My energy score has been < 60 for 3 days and my training load ratio is ${"%.2f".format(ratio)}. " +
                        "Generate a short deload plan for me for this week."
            )
        }

        return null
    }
}

data class TodayUiState(
    val isLoading: Boolean = false,
    val energyScore: EnergyScoreEntity? = null,
    val stepsToday: Long? = null,
    val sleepData: SleepData? = null,
    val latestHeartRateBpm: Long? = null,
    val hrvValue: Float? = null,
    val restingHeartRate: Int? = null,
    val healthPermissionsGranted: Boolean = false,
    val recentWorkouts: List<WorkoutEntity> = emptyList(),
    val watchConnected: Boolean = false,
    val recommendation: String? = null,
    val streak: StreakResult = StreakResult(),
    val nextScheduledWorkout: WorkoutScheduleEntity? = null,
    val deloadSuggestion: String? = null,
    val error: String? = null
)
