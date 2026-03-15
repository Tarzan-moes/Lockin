package com.lockin.app.watch.sync

/**
 * WatchSyncManager – Handles bidirectional data sync between phone and watch.
 *
 * Syncs workout data, heart rate readings, and notifications.
 * Uses Wear OS DataClient or Samsung Accessory SDK under the hood.
 */
interface WatchSyncManager {
    suspend fun syncWorkoutToWatch(workoutId: String): Boolean
    suspend fun syncHealthDataFromWatch(): Boolean
    suspend fun getLastSyncTimestamp(): Long?
}

