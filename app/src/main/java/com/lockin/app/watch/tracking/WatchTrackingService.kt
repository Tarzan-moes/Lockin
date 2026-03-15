package com.lockin.app.watch.tracking

/**
 * WatchTrackingService – Runs on the watch to track workouts in real-time.
 *
 * Collects heart rate, motion data, and rep counts from watch sensors,
 * then sends them to the phone app via WatchSyncManager.
 *
 * For Part 1 this is just the interface definition. The actual Service
 * will live in a separate :watch module when we build the Wear OS app.
 */
interface WatchTrackingService {
    fun startTracking(workoutId: String)
    fun stopTracking()
    fun isTracking(): Boolean
    suspend fun getCurrentHeartRate(): Int?
}

