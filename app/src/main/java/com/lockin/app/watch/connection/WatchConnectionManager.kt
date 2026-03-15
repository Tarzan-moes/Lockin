package com.lockin.app.watch.connection

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * WatchConnectionManager — Manages communication with a paired Wear OS watch
 * via the Google Play Services Wearable Data Layer API.
 *
 * Supports:
 * - Detecting watch connectivity
 * - Sending commands (start/stop workout)
 * - Receiving heart rate and workout data streams
 * - Syncing workout plans to the watch
 */
class WatchConnectionManager(context: Context) {

    companion object {
        private const val TAG = "WatchConnectionMgr"

        // Message paths
        const val START_WORKOUT_PATH = "/start_workout"
        const val STOP_WORKOUT_PATH = "/stop_workout"

        // Data paths
        const val HEART_RATE_PATH = "/heart_rate"
        const val WORKOUT_DATA_PATH = "/workout_data"
        const val WORKOUT_PLAN_PATH = "/workout_plan"

        // Data keys
        const val KEY_HEART_RATE = "heart_rate"
        const val KEY_CALORIES = "calories"
        const val KEY_DISTANCE = "distance"
        const val KEY_DURATION = "duration"
        const val KEY_EXERCISES = "exercises"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_WORKOUT_NAME = "workout_name"
    }

    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }

    // ── Connection ───────────────────────────────────────────────────────

    /**
     * Check if any Wear OS watch is currently connected.
     */
    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            val connected = nodes.isNotEmpty()
            Log.d(TAG, "isWatchConnected -> $connected")
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking watch connection: ${e.message}", e)
            false
        }
    }

    /**
     * Get the first connected watch node, or null if none.
     */
    suspend fun getConnectedWatch(): Node? {
        return try {
            nodeClient.connectedNodes.await().firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected watch: ${e.message}", e)
            null
        }
    }

    // ── Commands ─────────────────────────────────────────────────────────

    /**
     * Send a "start workout" command to the watch.
     */
    suspend fun startWorkoutOnWatch(workoutName: String) {
        try {
            val node = getConnectedWatch() ?: run {
                Log.w(TAG, "No watch connected to start workout")
                return
            }
            val payload = workoutName.toByteArray(Charsets.UTF_8)
            messageClient.sendMessage(node.id, START_WORKOUT_PATH, payload).await()
            Log.i(TAG, "Sent start workout command: $workoutName")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending start workout: ${e.message}", e)
        }
    }

    /**
     * Send a "stop workout" command to the watch.
     */
    suspend fun stopWorkoutOnWatch() {
        try {
            val node = getConnectedWatch() ?: run {
                Log.w(TAG, "No watch connected to stop workout")
                return
            }
            messageClient.sendMessage(node.id, STOP_WORKOUT_PATH, byteArrayOf()).await()
            Log.i(TAG, "Sent stop workout command")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending stop workout: ${e.message}", e)
        }
    }

    // ── Heart Rate Stream ────────────────────────────────────────────────

    /**
     * Observe real-time heart rate updates from the watch.
     * Emits BPM values as they arrive via the Wearable DataClient.
     */
    fun observeHeartRate(): Flow<Int> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == HEART_RATE_PATH
                ) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val bpm = dataMap.getInt(KEY_HEART_RATE, 0)
                        Log.d(TAG, "Received HR from watch: $bpm")
                        if (bpm > 0) {
                            trySend(bpm)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing heart rate: ${e.message}", e)
                    }
                }
            }
        }

        dataClient.addListener(listener)
        Log.i(TAG, "Heart rate listener registered")

        awaitClose {
            dataClient.removeListener(listener)
            Log.i(TAG, "Heart rate listener removed")
        }
    }

    // ── Workout Data Stream ──────────────────────────────────────────────

    /**
     * Observe workout summary data (HR, calories, distance, duration) from watch.
     */
    fun observeWorkoutData(): Flow<WorkoutDataFromWatch> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == WORKOUT_DATA_PATH
                ) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val data = WorkoutDataFromWatch(
                            heartRate = dataMap.getInt(KEY_HEART_RATE, 0),
                            calories = dataMap.getInt(KEY_CALORIES, 0),
                            distance = dataMap.getFloat(KEY_DISTANCE, 0f),
                            duration = dataMap.getLong(KEY_DURATION, 0L)
                        )
                        trySend(data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing workout data: ${e.message}", e)
                    }
                }
            }
        }

        dataClient.addListener(listener)
        Log.i(TAG, "Workout data listener registered")

        awaitClose {
            dataClient.removeListener(listener)
            Log.i(TAG, "Workout data listener removed")
        }
    }

    // ── Sync Workout Plan to Watch ───────────────────────────────────────

    /**
     * Push a list of exercise names to the watch so it can display the workout plan.
     */
    suspend fun syncWorkoutToWatch(exercises: List<String>) {
        try {
            val request = PutDataMapRequest.create(WORKOUT_PLAN_PATH).apply {
                dataMap.putStringArrayList(KEY_EXERCISES, ArrayList(exercises))
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            val putDataReq = request.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()
            Log.i(TAG, "Synced ${exercises.size} exercises to watch")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing workout to watch: ${e.message}", e)
        }
    }
}

// ── Data class ───────────────────────────────────────────────────────────────

data class WorkoutDataFromWatch(
    val heartRate: Int,
    val calories: Int,
    val distance: Float,
    val duration: Long
)
