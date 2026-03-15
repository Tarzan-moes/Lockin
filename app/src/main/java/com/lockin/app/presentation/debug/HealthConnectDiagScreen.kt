package com.lockin.app.presentation.debug

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectDiagScreen(
    viewModel: HealthConnectDiagViewModel,
    onNavigateBack: () -> Unit
) {
    val tag = "HC Diag"
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val stepsPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val permissionLauncher = rememberLauncherForActivityResult<Set<String>, Set<String>>(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d(tag, "Result: granted=$granted")
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("HC Diagnostic") })
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Health Connect available: ${state.isAvailable}")
            Text("Granted permissions count: ${state.grantedPermissions.size}")
            if (state.grantedPermissions.isNotEmpty()) {
                state.grantedPermissions.forEach { perm ->
                    Text(perm.toString())
                }
            }

            Button(onClick = {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("package:com.google.android.apps.healthdata"))
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(tag, "Failed to open Health Connect app: ${e.message}")
                }
            }) {
                Text("Open Health Connect app settings")
            }

            Button(onClick = {
                try {
                    Log.d(tag, "Launching steps permission request")
                    permissionLauncher.launch(stepsPermissions)
                } catch (e: Exception) {
                    Log.e(tag, "Error launching steps permission request", e)
                }
            }) {
                Text("Request Steps Permission (DIAGNOSTIC)")
            }
        }
    }
}
