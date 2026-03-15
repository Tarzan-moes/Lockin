package com.lockin.app.presentation.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.lockin.app.watch.connection.WatchConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WorkoutTrackingService — Foreground service that runs during an active workout
 * and listens to the watch heart rate stream, updating a persistent notification.
 *
 * Start with: context.startForegroundService(intent)
 * Stop with:  context.stopService(intent) or sendBroadcast(STOP action)
 */
@AndroidEntryPoint
class WorkoutTrackingService : Service() {

    companion object {
        private const val TAG = "WorkoutTrackingSvc"
        private const val CHANNEL_ID = "workout_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.lockin.app.STOP_WORKOUT_TRACKING"
    }

    @Inject
    lateinit var watchConnectionManager: WatchConnectionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var latestBpm = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "WorkoutTrackingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stopping via ACTION_STOP")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Starting workout tracking..."),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )
        startHeartRateCollection()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        serviceScope.cancel()
        Log.i(TAG, "WorkoutTrackingService destroyed")
        super.onDestroy()
    }

    // ── Heart rate collection ────────────────────────────────────────────

    private fun startHeartRateCollection() {
        serviceScope.launch {
            val connected = runCatching { watchConnectionManager.isWatchConnected() }.getOrDefault(false)
            Log.d(TAG, "Watch connected at start: $connected")
            if (!connected) {
                updateNotification("No watch connected")
            }
            watchConnectionManager.observeHeartRate()
                .onEach { Log.d(TAG, "HR event: $it") }
                .catch { e ->
                    Log.e(TAG, "Heart rate stream error: ${e.message}", e)
                    updateNotification("Heart rate unavailable")
                }
                .collect { bpm ->
                    latestBpm = bpm
                    updateNotification("Heart Rate: $bpm BPM")
                }
        }
    }

    // ── Notification ─────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Workout Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows workout progress and heart rate"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Lockin – Workout Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
