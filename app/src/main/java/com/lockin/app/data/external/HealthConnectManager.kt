package com.lockin.app.data.external

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * HealthConnectManager — Reads and writes health data via the Health Connect API.
 *
 * This is the single source of truth for Health Connect interactions. All reads
 * are done on [Dispatchers.IO] and return null/defaults when data is unavailable.
 */
class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectManager"
    }

    // Expose client for diagnostic/debug only (internal visibility would be better but public for simplicity here)
    val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    // ── Permissions ──────────────────────────────────────────────────────

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted: Set<String> = client.permissionController.getGrantedPermissions()
            permissions.all { it in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}", e)
            false
        }
    }

    // ── Steps ────────────────────────────────────────────────────────────

    suspend fun getStepsForDate(date: LocalDate): Long {
        Log.d("HC_STEPS", "━━━ getStepsForDate called for: $date ━━━")
        return try {
            val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            Log.d("HC_STEPS", "Query range: $start → $end")

            val response = client.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end))
            )

            Log.d("HC_STEPS", "Records returned: ${response.records.size}")
            response.records.forEachIndexed { i, record ->
                Log.d("HC_STEPS", "  Record $i: count=${record.count}, startTime=${record.startTime}, endTime=${record.endTime}")
            }

            val total = response.records.sumOf { it.count }
            Log.d("HC_STEPS", "Total steps: $total")
            total
        } catch (e: Exception) {
            Log.e("HC_STEPS", "ERROR: ${e::class.simpleName} — ${e.message}", e)
            0L
        }
    }

    // ── Heart Rate ───────────────────────────────────────────────────────

    suspend fun getHeartRateForToday(): List<HeartRateData> = withContext(Dispatchers.IO) {
        try {
            val (start, end) = dayRange(LocalDate.now())
            Log.d(TAG, "Reading heart rate for today | range: $start → $end")
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            Log.d(TAG, "HeartRate records count: ${response.records.size}")
            val samples = response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateData(bpm = sample.beatsPerMinute, timestamp = sample.time)
                }
            }
            Log.d(TAG, "HeartRate total samples: ${samples.size}, latest: ${samples.lastOrNull()?.bpm}")
            samples
        } catch (e: Exception) {
            Log.e(TAG, "HeartRate read FAILED: ${e::class.simpleName} — ${e.message}", e)
            emptyList()
        }
    }

    // ── Sleep ────────────────────────────────────────────────────────────

    suspend fun getSleepForDate(date: LocalDate): SleepData? = withContext(Dispatchers.IO) {
        try {
            // Sleep sessions typically end on the target date morning,
            // so query from previous day evening to target date noon
            val zone = ZoneId.systemDefault()
            val start = date.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val end = date.atTime(14, 0).atZone(zone).toInstant()
            Log.d(TAG, "Reading sleep for $date | range: $start → $end")

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            Log.d(TAG, "Sleep records count: ${response.records.size}")

            if (response.records.isEmpty()) {
                Log.d(TAG, "Sleep: no sessions found for $date")
                return@withContext null
            }

            val session = response.records.last() // Use the most recent session
            val totalMinutes = java.time.Duration.between(
                session.startTime, session.endTime
            ).toMinutes().toInt()
            Log.d(TAG, "Sleep session: ${session.startTime} → ${session.endTime} = ${totalMinutes}min, stages=${session.stages.size}")

            var deepMinutes = 0
            var remMinutes = 0
            for (stage in session.stages) {
                val stageMinutes = java.time.Duration.between(
                    stage.startTime, stage.endTime
                ).toMinutes().toInt()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += stageMinutes
                    SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += stageMinutes
                }
            }
            Log.d(TAG, "Sleep details: total=${totalMinutes}min deep=${deepMinutes}min rem=${remMinutes}min")

            val sleepQuality = calculateSleepQuality(totalMinutes, deepMinutes, remMinutes)

            SleepData(
                totalMinutes = totalMinutes,
                deepSleepMinutes = deepMinutes,
                remSleepMinutes = remMinutes,
                sleepQuality = sleepQuality
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sleep read FAILED: ${e::class.simpleName} — ${e.message}", e)
            null
        }
    }

    // ── HRV (Heart Rate Variability RMSSD) ───────────────────────────────

    suspend fun getHRVForDate(date: LocalDate): Float? = withContext(Dispatchers.IO) {
        try {
            val (start, end) = dayRange(date)
            Log.d(TAG, "Reading HRV for $date | range: $start → $end")
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            Log.d(TAG, "HRV records count: ${response.records.size}")
            if (response.records.isEmpty()) {
                Log.d(TAG, "HRV: no records for $date")
                return@withContext null
            }
            val avg = response.records.map { it.heartRateVariabilityMillis.toFloat() }.average().toFloat()
            Log.d(TAG, "HRV average: $avg ms")
            avg
        } catch (e: Exception) {
            Log.e(TAG, "HRV read FAILED: ${e::class.simpleName} — ${e.message}", e)
            null
        }
    }

    // ── Resting Heart Rate ───────────────────────────────────────────────

    suspend fun getRestingHeartRateForDate(date: LocalDate): Int? = withContext(Dispatchers.IO) {
        try {
            val (start, end) = dayRange(date)
            Log.d(TAG, "Reading resting HR for $date | range: $start → $end")
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            Log.d(TAG, "Resting HR records count: ${response.records.size}")
            if (response.records.isEmpty()) {
                Log.d(TAG, "Resting HR: no records for $date")
                return@withContext null
            }
            val rhr = response.records.last().beatsPerMinute.toInt()
            Log.d(TAG, "Resting HR: $rhr BPM")
            rhr
        } catch (e: Exception) {
            Log.e(TAG, "Resting HR read FAILED: ${e::class.simpleName} — ${e.message}", e)
            null
        }
    }

    // ── Write Exercise Session ────────────────────────────────────────────

    suspend fun writeExerciseSession(
        exerciseType: Int,
        startTime: Instant,
        endTime: Instant,
        title: String
    ): Unit = withContext(Dispatchers.IO) {
        try {
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startTime)
            @Suppress("RestrictedApi")
            val record = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                exerciseType = exerciseType,
                title = title
            )
            client.insertRecords(listOf(record))
            Log.i(TAG, "Exercise session written: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing exercise session: ${e.message}", e)
        }
    }

    // ── Write Weight ─────────────────────────────────────────────────────

    suspend fun writeWeight(weightKg: Double, time: Instant): Unit = withContext(Dispatchers.IO) {
        try {
            val record = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = time,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(time)
            )
            client.insertRecords(listOf(record))
            Log.i(TAG, "Weight written: ${weightKg}kg")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing weight: ${e.message}", e)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun dayRange(date: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }

    /**
     * Sleep quality heuristic (0.0 – 1.0):
     *  - Duration score: 7–9 hours is ideal
     *  - Deep sleep score: >60 min is great
     *  - REM score: >90 min is great
     */
    private fun calculateSleepQuality(
        totalMinutes: Int,
        deepMinutes: Int,
        remMinutes: Int
    ): Float {
        val durationScore = when {
            totalMinutes >= 420 && totalMinutes <= 540 -> 1.0f  // 7-9h ideal
            totalMinutes >= 360 -> 0.7f                          // 6h acceptable
            totalMinutes >= 300 -> 0.4f                          // 5h poor
            else -> 0.2f                                         // <5h very poor
        }
        val deepScore = (deepMinutes.coerceAtMost(90) / 90f).coerceIn(0f, 1f)
        val remScore = (remMinutes.coerceAtMost(120) / 120f).coerceIn(0f, 1f)

        return (durationScore * 0.5f + deepScore * 0.3f + remScore * 0.2f).coerceIn(0f, 1f)
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────

data class HeartRateData(
    val bpm: Long,
    val timestamp: Instant
)

data class SleepData(
    val totalMinutes: Int,
    val deepSleepMinutes: Int,
    val remSleepMinutes: Int,
    val sleepQuality: Float
)

