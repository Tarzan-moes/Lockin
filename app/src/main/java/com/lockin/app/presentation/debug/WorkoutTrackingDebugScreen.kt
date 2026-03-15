@file:OptIn(ExperimentalMaterial3Api::class)
package com.lockin.app.presentation.debug

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lockin.app.presentation.workout.WorkoutTrackingService

@Composable
fun WorkoutTrackingDebugScreen(
    viewModel: WorkoutTrackingDebugViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by viewModel.isWatchConnected.collectAsState()
    val latestBpm by viewModel.latestHeartRate.collectAsState()
    val serviceRunning = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Workout Tracking Debug") }) }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Watch status: ${if (isConnected) "Connected" else "Not connected"}")
            Text("Service status: ${if (serviceRunning.value) "Service running" else "Service stopped"}")
            latestBpm?.let { bpm -> Text("Last heart rate: $bpm BPM") }

            Button(onClick = {
                val intent = Intent(context, WorkoutTrackingService::class.java)
                context.startForegroundService(intent)
                serviceRunning.value = true
            }) {
                Text("Start Workout Tracking Service")
            }

            Button(onClick = {
                val intent = Intent(context, WorkoutTrackingService::class.java)
                context.stopService(intent)
                serviceRunning.value = false
            }) {
                Text("Stop Workout Tracking Service")
            }
        }
    }
}

