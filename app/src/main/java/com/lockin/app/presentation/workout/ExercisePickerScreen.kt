package com.lockin.app.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.ui.theme.*

private val muscleGroups = listOf(
    "CHEST", "BACK", "SHOULDERS", "ABS", "BICEPS", "TRICEPS",
    "QUADS", "HAMSTRINGS", "GLUTES", "CALVES", "CORE",
    "UPPER_BACK", "LATS", "FOREARMS", "ARMS", "LEGS", "CARDIO"
)
private val equipmentList = listOf(
    "BARBELL", "DUMBBELL", "CABLE", "MACHINE", "BODY_WEIGHT",
    "LEVERAGE_MACHINE", "BAND", "SMITH_MACHINE", "KETTLEBELL",
    "SZ-BAR", "SWISS_BALL", "INCLINE_BENCH", "NONE_(BODYWEIGHT_EXERCISE)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    viewModel: ExercisePickerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = LockinBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Exercises", fontWeight = FontWeight.Bold)
                        if (uiState.syncStatus.isNotBlank()) {
                            Text(
                                uiState.syncStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isSyncing) LockinPrimaryAccent
                                        else if (uiState.syncStatus.contains("✓")) LockinSuccess
                                        else LockinTextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LockinSurface,
                    titleContentColor = LockinTextPrimary,
                    navigationIconContentColor = LockinTextPrimary
                )
            )
        }
    ) { padding ->
        ExercisePickerContent(
            uiState = uiState,
            onSearchQueryChanged = viewModel::updateSearch,
            onToggleMuscleFilter = viewModel::toggleMuscleFilter,
            onToggleEquipmentFilter = viewModel::toggleEquipmentFilter,
            onAddExercise = { viewModel.addExerciseToWorkout(it) },
            onExerciseClick = onExerciseClick,
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerContent(
    uiState: ExercisePickerUiState,
    onSearchQueryChanged: (String) -> Unit,
    onToggleMuscleFilter: (String) -> Unit,
    onToggleEquipmentFilter: (String) -> Unit,
    onAddExercise: (ExerciseDetailEntity) -> Unit,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sync progress
        if (uiState.isSyncing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = LockinPrimaryAccent,
                trackColor = LockinBackground
            )
        }

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search exercises…", color = LockinTextDisabled) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LockinTextSecondary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LockinPrimaryAccent,
                unfocusedBorderColor = LockinSubtleBorder,
                focusedContainerColor = LockinSurface,
                unfocusedContainerColor = LockinSurface,
                focusedTextColor = LockinTextPrimary,
                unfocusedTextColor = LockinTextPrimary,
                cursorColor = LockinPrimaryAccent
            )
        )

        // Filters with ExposedDropdownMenuBox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Muscle Filter Dropdown
            var muscleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = muscleExpanded,
                onExpandedChange = { muscleExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = if (uiState.selectedMuscles.isEmpty()) "All Muscles" else "${uiState.selectedMuscles.size} selected",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleExpanded) },
                    modifier = Modifier.menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = LockinSurface,
                        unfocusedContainerColor = LockinSurface,
                        focusedBorderColor = LockinPrimaryAccent,
                        unfocusedBorderColor = LockinSubtleBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = muscleExpanded,
                    onDismissRequest = { muscleExpanded = false },
                    modifier = Modifier.background(LockinSurface)
                ) {
                    muscleGroups.forEach { muscle ->
                        val isSelected = muscle in uiState.selectedMuscles
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(muscle.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                                }
                            },
                            onClick = {
                                onToggleMuscleFilter(muscle)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Equipment Filter Dropdown
            var equipmentExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = equipmentExpanded,
                onExpandedChange = { equipmentExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = if (uiState.selectedEquipment.isEmpty()) "All Equipment" else "${uiState.selectedEquipment.size} selected",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                    modifier = Modifier.menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                         focusedContainerColor = LockinSurface,
                         unfocusedContainerColor = LockinSurface,
                         focusedBorderColor = LockinPrimaryAccent,
                         unfocusedBorderColor = LockinSubtleBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = equipmentExpanded,
                    onDismissRequest = { equipmentExpanded = false },
                    modifier = Modifier.background(LockinSurface)
                ) {
                    equipmentList.forEach { eq ->
                        val isSelected = eq in uiState.selectedEquipment
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(eq.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                                }
                            },
                            onClick = {
                                onToggleEquipmentFilter(eq)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        // Active filter chips
        if (uiState.selectedMuscles.isNotEmpty() || uiState.selectedEquipment.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.selectedMuscles.toList()) { muscle ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleMuscleFilter(muscle) },
                        label = { Text(muscle.lowercase().replace("_", " ")) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = LockinPrimaryAccent.copy(alpha = 0.15f),
                            labelColor = LockinPrimaryAccent
                        )
                    )
                }
                items(uiState.selectedEquipment.toList()) { eq ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleEquipmentFilter(eq) },
                        label = { Text(eq.lowercase().replace("_", " ")) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = LockinSecondaryAccent.copy(alpha = 0.15f),
                            labelColor = LockinSecondaryAccent
                        )
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // Exercise list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (uiState.exercises.isEmpty() && !uiState.isSyncing) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = LockinTextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No exercises available",
                            style = MaterialTheme.typography.titleMedium,
                            color = LockinTextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Connect to the internet to sync the exercise library,\nthen exercises will be available offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LockinTextDisabled,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            items(uiState.exercises, key = { it.id }) { exercise ->
                // ...existing code...
                    ExercisePickerItem(
                        exercise = exercise,
                        isAdded = exercise.id in uiState.addedIds,
                        onAdd = { viewModel.addExerciseToWorkout(exercise) },
                        onTap = { onExerciseClick(exercise.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ExercisePickerItem(
    exercise: ExerciseDetailEntity,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onTap: () -> Unit
) {
    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GIF thumbnail (always show area; placeholder if no URL)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LockinSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!exercise.gifUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(exercise.gifUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = LockinTextDisabled,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LockinTextPrimary,
                    maxLines = 2
                )
                Text(
                    buildString {
                        append(exercise.primaryMuscleGroup.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                        if (exercise.equipment.isNotEmpty()) {
                            append(" · ")
                            append(exercise.equipment.first().lowercase().replace("_", " ").replaceFirstChar { it.uppercase() })
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LockinTextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isAdded) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LockinSuccess.copy(alpha = 0.12f)
                ) {
                    Text(
                        "Added",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LockinSuccess,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onAdd,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LockinPrimaryAccent.copy(alpha = 0.12f),
                        contentColor = LockinPrimaryAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
