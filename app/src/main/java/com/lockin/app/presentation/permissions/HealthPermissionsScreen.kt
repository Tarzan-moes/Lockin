@file:OptIn(ExperimentalMaterial3Api::class)
package com.lockin.app.presentation.permissions

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.lockin.app.data.external.HealthConnectManager
import com.lockin.app.ui.theme.*

/**
 * HealthPermissionsScreen — Requests Health Connect permissions with a clear
 * explanation of what data is accessed and why.
 *
 * Uses [PermissionController.createRequestPermissionResultContract] to launch
 * the system Health Connect permissions dialog.
 */
@Composable
fun HealthPermissionsScreen(
    healthConnectManager: HealthConnectManager,
    onPermissionsGranted: () -> Unit
) {
    val tag = "HealthPerm"
    var allGranted by remember { mutableStateOf(false) }
    var hasChecked by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Log permissions on composition
    LaunchedEffect(Unit) {
        Log.d(tag, "Composed. permissions size=${healthConnectManager.permissions.size} perms=${healthConnectManager.permissions}")
        allGranted = healthConnectManager.hasAllPermissions()
        hasChecked = true
        if (allGranted) {
            statusMessage = "Permissions already granted"
            Log.d(tag, "Already granted on entry; calling onPermissionsGranted()")
            onPermissionsGranted()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult<Set<String>, Set<String>>(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        Log.d(tag, "Result callback granted=$grantedPermissions")

        val hasAny = grantedPermissions.isNotEmpty()
        val allRequestedGranted = healthConnectManager.permissions.all { it in grantedPermissions }

        when {
            allRequestedGranted -> {
                allGranted = true
                statusMessage = "All permissions granted"
                Log.d(tag, "All permissions granted, calling onPermissionsGranted()")
                onPermissionsGranted()
            }
            hasAny -> {
                allGranted = false
                statusMessage = "Some permissions granted, some denied. Basic features may still work."
                Log.d(tag, "Partial grant: $grantedPermissions")
                onPermissionsGranted()
            }
            else -> {
                allGranted = false
                statusMessage = "No permissions granted. Open Health Connect app → Apps → Lockin and enable permissions manually."
                Log.d(tag, "Empty grant set — dialog may not have shown")
            }
        }
    }

    // Diagnostic: steps-only launcher
    val stepsPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    val stepsLauncher = rememberLauncherForActivityResult<Set<String>, Set<String>>(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d("HealthPermDiag", "Steps-only granted=$granted")
        statusMessage = if (granted.isNotEmpty()) "Steps permission granted!" else "Steps-only: no dialog or denied. Try manual grant."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Health Data Access",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = LockinTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lockin reads your health data to calculate your Energy Score " +
                    "and provide personalized training recommendations. " +
                    "All data stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = LockinTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission items list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(permissionItems) { item ->
                PermissionItem(item = item, isGranted = allGranted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (allGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = LockinPrimaryAccent.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = LockinPrimaryAccent
                    )
                    Text(
                        text = "All permissions granted",
                        color = LockinPrimaryAccent,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (allGranted) {
                    statusMessage = "Permissions already granted"
                    Log.d(tag, "Button: already granted, calling onPermissionsGranted()")
                    onPermissionsGranted()
                } else {
                    val perms = healthConnectManager.permissions
                    if (perms.isEmpty()) {
                        statusMessage = "No permissions to request. Ensure Health Connect is available."
                        Log.e(tag, "No permissions to launch; Health Connect may be unavailable")
                        return@Button
                    }
                    try {
                        Log.d(tag, "Launching permission request, perms=$perms")
                        permissionLauncher.launch(perms)
                    } catch (e: Exception) {
                        Log.e(tag, "Error requesting Health Connect permissions", e)
                        statusMessage = "Unable to launch Health Connect permissions (${e.message ?: "unknown"})"
                    }
                }
            },
            enabled = hasChecked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LockinPrimaryAccent,
                contentColor = LockinOnPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (allGranted) "Continue" else "Grant Permissions",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = LockinTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Diagnostic: minimal steps-only request to isolate whether the dialog shows at all
        Button(
            onClick = {
                try {
                    Log.d("HealthPermDiag", "Launching steps-only request: $stepsPermissions")
                    stepsLauncher.launch(stepsPermissions)
                } catch (e: Exception) {
                    Log.e("HealthPermDiag", "Error in steps-only request", e)
                    statusMessage = "Steps-only error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LockinSurface,
                contentColor = LockinTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Diagnostic: Request Steps Only", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Permission item composable ───────────────────────────────────────────────

@Composable
private fun PermissionItem(item: PermissionItemData, isGranted: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LockinSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (isGranted) LockinPrimaryAccent else LockinTextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LockinTextPrimary
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LockinTextSecondary
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = LockinPrimaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Data ─────────────────────────────────────────────────────────────────────

private data class PermissionItemData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val permissionItems = listOf(
    PermissionItemData(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        title = "Steps & Distance",
        description = "Track daily movement and activity levels"
    ),
    PermissionItemData(
        icon = Icons.Default.FavoriteBorder,
        title = "Heart Rate & HRV",
        description = "Monitor cardiovascular health and recovery"
    ),
    PermissionItemData(
        icon = Icons.Default.Bedtime,
        title = "Sleep Sessions",
        description = "Analyze sleep duration, quality, and stages"
    ),
    PermissionItemData(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        title = "Exercise Sessions",
        description = "Read and log workout sessions"
    ),
    PermissionItemData(
        icon = Icons.Default.LocalFireDepartment,
        title = "Calories Burned",
        description = "Track active and total energy expenditure"
    ),
    PermissionItemData(
        icon = Icons.Default.MonitorWeight,
        title = "Body Weight",
        description = "Record and read weight measurements"
    )
)
