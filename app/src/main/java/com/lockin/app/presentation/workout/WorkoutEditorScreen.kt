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
import com.lockin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    viewModel: WorkoutEditorViewModel = hiltViewModel(),
    exercisePickerViewModel: ExercisePickerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onAddExercise: () -> Unit = {},
    onStartWorkout: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pickerUiState by exercisePickerViewModel.uiState.collectAsState()
    val goals = listOf("STRENGTH" to "💪 Strength", "HYPERTROPHY" to "🎯 Hypertrophy", "ENDURANCE" to "🏃 Endurance")

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = LockinBackground
        ) {
            ExercisePickerContent(
                uiState = pickerUiState,
                onSearchQueryChanged = exercisePickerViewModel::updateSearch,
                onToggleMuscleFilter = exercisePickerViewModel::toggleMuscleFilter,
                onToggleEquipmentFilter = exercisePickerViewModel::toggleEquipmentFilter,
                onAddExercise = {
                    exercisePickerViewModel.addExerciseToWorkout(it)
                    // We don't close immediately so user can add multiple
                },
                onExerciseClick = { /* Show details? Or just add? For now just add seems faster flow, or ignore details */ }
            )
        }
    }

    Scaffold(
        containerColor = LockinBackground,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Edit Workout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveWorkout()
                        onNavigateBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LockinSurface,
                    titleContentColor = LockinTextPrimary,
                    navigationIconContentColor = LockinTextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Workout name") },
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
            }

            // Goal chips
            item {
                Text("Goal", style = MaterialTheme.typography.labelLarge, color = LockinTextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { (key, label) ->
                        FilterChip(
                            selected = uiState.goal == key,
                            onClick = { viewModel.updateGoal(key) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LockinPrimaryAccent.copy(alpha = 0.15f),
                                selectedLabelColor = LockinPrimaryAccent
                            )
                        )
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.updateNotes(it) },
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    maxLines = 3,
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
            }

            // Exercises header + add button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Exercises (${uiState.exercises.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = LockinTextPrimary
                    )
                    FilledTonalButton(
                        onClick = {
                            viewModel.saveWorkout()
                            showBottomSheet = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = LockinPrimaryAccent.copy(alpha = 0.12f),
                            contentColor = LockinPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            if (uiState.exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LockinSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No exercises added yet", style = MaterialTheme.typography.bodyMedium, color = LockinTextDisabled)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap + Add to pick from the library", style = MaterialTheme.typography.bodySmall, color = LockinTextDisabled)
                        }
                    }
                }
            }

            items(uiState.exercises, key = { it.workoutExercise.id }) { exw ->
                ExerciseEditorCard(
                    exerciseWithDetail = exw,
                    onUpdateTargets = { sets, reps, rpe ->
                        viewModel.updateExerciseTargets(exw.workoutExercise.id, sets, reps, rpe)
                    }
                )
            }

            // Start workout button
            if (uiState.exercises.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveWorkout()
                            uiState.workout?.let { onStartWorkout(it.id) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LockinPrimaryAccent,
                            contentColor = LockinOnPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Workout", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    exerciseWithDetail: ExerciseWithDetail,
    onUpdateTargets: (sets: Int?, reps: Int?, rpe: Double?) -> Unit
) {
    val we = exerciseWithDetail.workoutExercise
    val detail = exerciseWithDetail.detail
    var setsText by remember(we.id) { mutableStateOf(we.plannedSets?.toString() ?: "3") }
    var repsText by remember(we.id) { mutableStateOf(we.plannedReps?.toString() ?: "10") }
    var rpeText by remember(we.id) { mutableStateOf(we.targetRpe?.toString() ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail
                if (!detail?.gifUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(detail?.gifUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = detail?.name,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        detail?.name ?: "Exercise",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LockinTextPrimary
                    )
                    detail?.let {
                        Text(
                            "${it.primaryMuscleGroup} · ${it.equipment.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LockinTextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactNumberField("Sets", setsText, Modifier.weight(1f)) { v ->
                    setsText = v
                    onUpdateTargets(v.toIntOrNull(), repsText.toIntOrNull(), rpeText.toDoubleOrNull())
                }
                CompactNumberField("Reps", repsText, Modifier.weight(1f)) { v ->
                    repsText = v
                    onUpdateTargets(setsText.toIntOrNull(), v.toIntOrNull(), rpeText.toDoubleOrNull())
                }
                CompactNumberField("RPE", rpeText, Modifier.weight(1f)) { v ->
                    rpeText = v
                    onUpdateTargets(setsText.toIntOrNull(), repsText.toIntOrNull(), v.toDoubleOrNull())
                }
            }
        }
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LockinPrimaryAccent,
            unfocusedBorderColor = LockinSubtleBorder,
            focusedTextColor = LockinTextPrimary,
            unfocusedTextColor = LockinTextPrimary,
            focusedLabelColor = LockinPrimaryAccent,
            cursorColor = LockinPrimaryAccent
        )
    )
}
