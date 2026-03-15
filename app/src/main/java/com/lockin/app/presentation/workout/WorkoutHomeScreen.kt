package com.lockin.app.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.domain.usecase.StreakResult
import com.lockin.app.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutHomeScreen(
    viewModel: WorkoutHomeViewModel = hiltViewModel(),
    onCreateWorkout: (workoutId: String) -> Unit = {},
    onOpenWorkout: (workoutId: String) -> Unit = {},
    onStartWorkout: (workoutId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf("HYPERTROPHY") }
    var workoutName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        CreateWorkoutDialog(
            name = workoutName,
            onNameChange = { workoutName = it },
            selectedGoal = selectedGoal,
            onGoalChange = { selectedGoal = it },
            onConfirm = {
                val id = viewModel.createWorkout(workoutName, selectedGoal)
                workoutName = ""
                selectedGoal = "HYPERTROPHY"
                showCreateDialog = false
                onCreateWorkout(id)
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                "Workouts",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary
            )
        }

        // Streak chip
        if (uiState.streak.currentStreakDays > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LockinPrimaryAccent.copy(alpha = 0.12f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔥", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${uiState.streak.currentStreakDays} day streak",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LockinPrimaryAccent
                        )
                    }
                }
            }
        }

        // Create CTA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LockinPrimaryAccent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create Workout", style = MaterialTheme.typography.titleLarge, color = LockinOnPrimary)
                        Spacer(Modifier.height(2.dp))
                        Text("Build a custom workout plan", style = MaterialTheme.typography.bodyMedium, color = LockinOnPrimary.copy(alpha = 0.8f))
                    }
                    FilledIconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LockinOnPrimary.copy(alpha = 0.2f),
                            contentColor = LockinOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create")
                    }
                }
            }
        }

        // Workout list
        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LockinPrimaryAccent)
                }
            }
        } else if (uiState.workouts.isEmpty()) {
            item { EmptyWorkoutsPlaceholder() }
        } else {
            // Group: In Progress first, then Planned, then Completed
            val grouped = uiState.workouts.sortedBy {
                when (it.status) {
                    "IN_PROGRESS" -> 0
                    "PLANNED" -> 1
                    else -> 2
                }
            }
            items(grouped, key = { it.id }) { workout ->
                WorkoutListItem(
                    workout = workout,
                    onClick = {
                        if (workout.status == "IN_PROGRESS") onStartWorkout(workout.id)
                        else onOpenWorkout(workout.id)
                    },
                    onStart = { onStartWorkout(workout.id) },
                    onDelete = { viewModel.deleteWorkout(workout.id) }
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun WorkoutListItem(
    workout: WorkoutEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d") }
    val statusColor = when (workout.status) {
        "IN_PROGRESS" -> LockinWarning
        "COMPLETED" -> LockinSuccess
        else -> LockinPrimaryAccent
    }
    val statusLabel = when (workout.status) {
        "IN_PROGRESS" -> "In Progress"
        "COMPLETED" -> "Completed"
        else -> "Planned"
    }
    val goalLabel = when (workout.goal) {
        "STRENGTH" -> "💪 Strength"
        "ENDURANCE" -> "🏃 Endurance"
        else -> "🎯 Hypertrophy"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        workout.name.ifBlank { "Untitled Workout" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LockinTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goalLabel, style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                        workout.plannedDate?.let {
                            Text("· ${it.format(dateFmt)}", style = MaterialTheme.typography.bodySmall, color = LockinTextDisabled)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (workout.status == "COMPLETED" && workout.totalVolumeKg != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Vol: ${String.format("%.0f", workout.totalVolumeKg)} kg", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                    workout.totalSets?.let { Text("Sets: $it", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary) }
                }
            }

            if (workout.status != "COMPLETED") {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (workout.status == "PLANNED") {
                        FilledTonalButton(
                            onClick = onStart,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LockinPrimaryAccent.copy(alpha = 0.12f),
                                contentColor = LockinPrimaryAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onStart,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LockinWarning.copy(alpha = 0.12f),
                                contentColor = LockinWarning
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Continue")
                        }
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = LockinError, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkoutsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = LockinTextDisabled, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("No workouts yet", style = MaterialTheme.typography.titleMedium, color = LockinTextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Create your first workout to get started.", style = MaterialTheme.typography.bodySmall, color = LockinTextDisabled, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CreateWorkoutDialog(
    name: String,
    onNameChange: (String) -> Unit,
    selectedGoal: String,
    onGoalChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val goals = listOf("STRENGTH" to "💪 Strength", "HYPERTROPHY" to "🎯 Hypertrophy", "ENDURANCE" to "🏃 Endurance")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockinSurface,
        title = { Text("New Workout", color = LockinTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Workout name") },
                    placeholder = { Text("e.g. Push Day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LockinPrimaryAccent,
                        unfocusedBorderColor = LockinSubtleBorder,
                        focusedLabelColor = LockinPrimaryAccent,
                        focusedTextColor = LockinTextPrimary,
                        unfocusedTextColor = LockinTextPrimary,
                        cursorColor = LockinPrimaryAccent
                    )
                )
                Text("Goal", style = MaterialTheme.typography.labelLarge, color = LockinTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedGoal == key,
                            onClick = { onGoalChange(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LockinPrimaryAccent.copy(alpha = 0.15f),
                                selectedLabelColor = LockinPrimaryAccent
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = LockinPrimaryAccent, contentColor = LockinOnPrimary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = LockinTextSecondary) }
        }
    )
}

