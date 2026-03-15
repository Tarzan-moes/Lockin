@file:OptIn(ExperimentalMaterial3Api::class)
package com.lockin.app.presentation.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HealthDebugScreen(
    viewModel: HealthDebugViewModel,
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Debug") },
                navigationIcon = { /* Optionally add back icon if desired */ }
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Quickly verify Health Connect permissions and steps read.")

            Button(onClick = {
                if (state.permissionNeeded) {
                    onRequestPermissions()
                } else {
                    viewModel.testStepsToday()
                }
            }) {
                Text(if (state.permissionNeeded) "Grant Permissions" else "Test Health Connect: Steps Today")
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            if (state.message.isNotBlank()) {
                Text(state.message)
            }

            state.steps?.let { steps ->
                Text("Steps today: $steps")
            }
        }
    }
}
