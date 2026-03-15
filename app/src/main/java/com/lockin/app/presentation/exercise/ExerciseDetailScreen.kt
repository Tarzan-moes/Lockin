package com.lockin.app.presentation.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val exercise by viewModel.exercise.collectAsState()

    Scaffold(
        containerColor = LockinBackground,
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise", fontWeight = FontWeight.Bold) },
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
        if (exercise == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LockinPrimaryAccent)
            }
        } else {
            val ex = exercise!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // GIF / Image hero
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(LockinSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!ex.gifUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(ex.gifUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = ex.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                tint = LockinTextDisabled,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // Name + chips
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            ex.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = LockinTextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (ex.primaryMuscleGroup.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ex.primaryMuscleGroup.formatLabel()) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = LockinPrimaryAccent.copy(alpha = 0.12f),
                                        labelColor = LockinPrimaryAccent
                                    )
                                )
                            }
                            if (ex.equipment.isNotEmpty()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ex.equipment.first().formatLabel()) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = LockinSecondaryAccent.copy(alpha = 0.12f),
                                        labelColor = LockinSecondaryAccent
                                    )
                                )
                            }
                            if (ex.difficulty.isNotBlank()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(ex.difficulty.formatLabel()) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = LockinWarning.copy(alpha = 0.12f),
                                        labelColor = LockinWarning
                                    )
                                )
                            }
                        }
                    }
                }

                // Muscle info card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LockinSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Muscles", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Primary", ex.primaryMuscleGroup.formatLabel())
                            if (ex.secondaryMuscles.isNotEmpty()) {
                                InfoRow("Secondary", ex.secondaryMuscles.joinToString(", ") { it.formatLabel() })
                            }
                            if (ex.equipment.isNotEmpty()) {
                                InfoRow("Equipment", ex.equipment.joinToString(", ") { it.formatLabel() })
                            }
                            if (ex.movementPattern.isNotBlank()) {
                                InfoRow("Body Part", ex.movementPattern.formatLabel())
                            }
                        }
                    }
                }

                // Instructions
                if (ex.instructions.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LockinSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("How to Perform", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
                                Spacer(Modifier.height(8.dp))
                                val steps = ex.instructions.split("\n").filter { it.isNotBlank() }
                                steps.forEachIndexed { index, step ->
                                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                        Text(
                                            "${index + 1}.",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = LockinPrimaryAccent,
                                            modifier = Modifier.width(28.dp)
                                        )
                                        Text(
                                            step.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = LockinTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tips
                if (ex.tips.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LockinSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Tips", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
                                Spacer(Modifier.height(8.dp))
                                ex.tips.forEach { tip ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text("•  ", color = LockinPrimaryAccent, style = MaterialTheme.typography.bodyMedium)
                                        Text(tip, style = MaterialTheme.typography.bodyMedium, color = LockinTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LockinTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LockinTextPrimary)
    }
}

private fun String.formatLabel(): String =
    this.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

