package com.lockin.app.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lockin.app.data.local.models.ExerciseSetEntity
import com.lockin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    viewModel: WorkoutSessionViewModel = hiltViewModel(),
    onWorkoutCompleted: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate away when completed
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onWorkoutCompleted()
    }

    Scaffold(
        containerColor = LockinBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.workout?.name ?: "Workout",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = LockinTextPrimary
                        )
                        Text(
                            "Log your sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = LockinTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LockinSurface,
                    navigationIconContentColor = LockinTextPrimary
                )
            )
        },
        bottomBar = {
            Surface(color = LockinSurface, shadowElevation = 4.dp) {
                Button(
                    onClick = { viewModel.completeWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LockinSuccess,
                        contentColor = LockinOnPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Complete Workout", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LockinPrimaryAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress summary
                item {
                    val totalSets = uiState.exercises.sumOf { it.sets.size }
                    val completedSets = uiState.exercises.sumOf { ex -> ex.sets.count { it.status == "COMPLETED" } }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LockinSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Progress", style = MaterialTheme.typography.labelLarge, color = LockinTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { if (totalSets > 0) completedSets.toFloat() / totalSets else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = LockinPrimaryAccent,
                                trackColor = LockinSurfaceElevated
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("$completedSets / $totalSets sets", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                        }
                    }
                }

                items(uiState.exercises, key = { it.workoutExercise.id }) { sessionExercise ->
                    SessionExerciseCard(
                        sessionExercise = sessionExercise,
                        onUpdateSet = viewModel::updateSet,
                        onCompleteSet = viewModel::completeSet
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SessionExerciseCard(
    sessionExercise: SessionExercise,
    onUpdateSet: (setId: String, weight: Double?, reps: Int?, rpe: Double?) -> Unit,
    onCompleteSet: (setId: String) -> Unit
) {
    val detail = sessionExercise.detail
    val we = sessionExercise.workoutExercise

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise header with thumbnail
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!detail?.gifUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(detail?.gifUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = detail?.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        detail?.name ?: "Exercise",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LockinTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail?.let {
                            Text(it.primaryMuscleGroup, style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                        }
                        we.targetRpe?.let {
                            Text("Target RPE: $it", style = MaterialTheme.typography.bodySmall, color = LockinPrimaryAccent)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, color = LockinTextDisabled, modifier = Modifier.width(32.dp))
                Text("Weight", style = MaterialTheme.typography.labelSmall, color = LockinTextDisabled, modifier = Modifier.weight(1f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, color = LockinTextDisabled, modifier = Modifier.weight(1f))
                Text("RPE", style = MaterialTheme.typography.labelSmall, color = LockinTextDisabled, modifier = Modifier.weight(0.7f))
                Spacer(Modifier.width(40.dp)) // button space
            }

            Spacer(Modifier.height(4.dp))

            sessionExercise.sets.forEach { set ->
                SetInputRow(
                    set = set,
                    onUpdate = { w, r, rpe -> onUpdateSet(set.id, w, r, rpe) },
                    onComplete = { onCompleteSet(set.id) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SetInputRow(
    set: ExerciseSetEntity,
    onUpdate: (weight: Double?, reps: Int?, rpe: Double?) -> Unit,
    onComplete: () -> Unit
) {
    val isCompleted = set.status == "COMPLETED"
    var weightText by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg?.toString() ?: "") }
    var repsText by remember(set.id, set.reps) { mutableStateOf(set.reps?.toString() ?: "") }
    var rpeText by remember(set.id, set.rpe) { mutableStateOf(set.rpe?.toString() ?: "") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "${set.setNumber}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isCompleted) LockinSuccess else LockinTextPrimary,
            modifier = Modifier.width(32.dp)
        )
        MiniInput(weightText, Modifier.weight(1f), !isCompleted) { v ->
            weightText = v
            onUpdate(v.toDoubleOrNull(), repsText.toIntOrNull(), rpeText.toDoubleOrNull())
        }
        MiniInput(repsText, Modifier.weight(1f), !isCompleted) { v ->
            repsText = v
            onUpdate(weightText.toDoubleOrNull(), v.toIntOrNull(), rpeText.toDoubleOrNull())
        }
        MiniInput(rpeText, Modifier.weight(0.7f), !isCompleted) { v ->
            rpeText = v
            onUpdate(weightText.toDoubleOrNull(), repsText.toIntOrNull(), v.toDoubleOrNull())
        }
        if (isCompleted) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = LockinSuccess, modifier = Modifier.size(28.dp))
        } else {
            IconButton(
                onClick = {
                    // Save current values then complete
                    onUpdate(weightText.toDoubleOrNull(), repsText.toIntOrNull(), rpeText.toDoubleOrNull())
                    onComplete()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Complete set", tint = LockinPrimaryAccent, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun MiniInput(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.height(44.dp),
        textStyle = MaterialTheme.typography.bodySmall,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LockinPrimaryAccent,
            unfocusedBorderColor = LockinSubtleBorder,
            disabledBorderColor = LockinSubtleBorder.copy(alpha = 0.4f),
            focusedTextColor = LockinTextPrimary,
            unfocusedTextColor = LockinTextPrimary,
            disabledTextColor = LockinTextDisabled,
            cursorColor = LockinPrimaryAccent
        )
    )
}

