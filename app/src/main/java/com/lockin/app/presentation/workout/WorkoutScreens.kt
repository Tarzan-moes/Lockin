package com.lockin.app.presentation.workout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.ui.theme.*

/**
 * WorkoutScreen – Workout hub with start button and placeholder workout list.
 */
@Composable
fun WorkoutScreen(
    onWorkoutComplete: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        // Header
        item {
            Text(
                text = "Workouts",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary
            )
        }

        // Start workout CTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            "Ready to train?",
                            style = MaterialTheme.typography.titleLarge,
                            color = LockinOnPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Start a new workout session",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LockinOnPrimary.copy(alpha = 0.8f)
                        )
                    }
                    FilledIconButton(
                        onClick = { /* TODO: start workout flow */ },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LockinOnPrimary.copy(alpha = 0.2f),
                            contentColor = LockinOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                "Planned",
                style = MaterialTheme.typography.titleLarge,
                color = LockinTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Empty state
        item {
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
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = LockinTextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No planned workouts",
                        style = MaterialTheme.typography.titleMedium,
                        color = LockinTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ask the AI Coach to generate one,\nor create your own.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LockinTextDisabled,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Quick history
        item {
            Text(
                "Recent Sessions",
                style = MaterialTheme.typography.titleLarge,
                color = LockinTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Text(
                "No completed workouts yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = LockinTextDisabled,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

/**
 * WorkoutSummaryScreen – Post-workout summary with real stats.
 */
@Composable
fun WorkoutSummaryScreen(
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
    reviewViewModel: WorkoutReviewViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviewState by reviewViewModel.reviewState.collectAsState()
    val workout = uiState.workout

    LaunchedEffect(workout?.id) {
        reviewViewModel.reset()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(24.dp)) }

        // Trophy icon + title
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = LockinPRGold,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Workout Complete!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = LockinTextPrimary
                )
                if (workout != null) {
                    Text(
                        workout.name.ifBlank { "Workout" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LockinTextSecondary
                    )
                }
            }
        }

        // Stats grid
        if (workout != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        label = "Volume",
                        value = "%.0f kg".format(workout.totalVolumeKg ?: 0.0),
                        icon = Icons.Default.FitnessCenter,
                        color = LockinPrimaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Duration",
                        value = uiState.durationMinutes?.let { "${it} min" } ?: "—",
                        icon = Icons.Default.Timer,
                        color = LockinSecondaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        label = "Sets",
                        value = "${workout.totalSets ?: 0}",
                        icon = Icons.Default.Repeat,
                        color = LockinWarning,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = "Avg RPE",
                        value = uiState.avgRpe?.let { "%.1f".format(it) } ?: "—",
                        icon = Icons.Default.Speed,
                        color = LockinError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Exercise breakdown
        if (uiState.exerciseSummaries.isNotEmpty()) {
            item {
                Text(
                    "Exercise Breakdown",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LockinTextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(uiState.exerciseSummaries) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LockinSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LockinTextPrimary
                            )
                            Text(
                                "${exercise.primaryMuscle} · ${exercise.totalSets} sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = LockinTextSecondary
                            )
                        }
                        if (exercise.bestWeight != null && exercise.bestReps != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LockinPrimaryAccent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "%.0f kg × %d".format(exercise.bestWeight, exercise.bestReps),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LockinPrimaryAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Buttons
        if (workout != null) {
            item {
                when (val state = reviewState) {
                    WorkoutReviewViewModel.ReviewState.Idle -> {
                        Button(
                            onClick = { reviewViewModel.reviewWorkout(workout.id, workout.goal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LockinPrimaryAccent),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ask AI to Improve This Workout")
                        }
                    }

                    WorkoutReviewViewModel.ReviewState.Loading -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LockinSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = LockinPrimaryAccent
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("AI is analyzing your workout...", color = LockinTextPrimary)
                            }
                        }
                    }

                    is WorkoutReviewViewModel.ReviewState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LockinSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = LockinPrimaryAccent
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "AI Coach Suggestions",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = LockinPrimaryAccent
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = state.suggestions,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LockinTextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { reviewViewModel.reviewWorkout(workout.id, workout.goal) }) {
                                    Text("Regenerate suggestions")
                                }
                            }
                        }
                    }

                    is WorkoutReviewViewModel.ReviewState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LockinPrimaryAccent,
                    contentColor = LockinOnPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Workouts", style = MaterialTheme.typography.labelLarge)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = LockinTextSecondary)
        }
    }
}
