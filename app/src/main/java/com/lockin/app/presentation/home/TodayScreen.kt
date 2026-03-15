@file:OptIn(ExperimentalMaterial3Api::class)
package com.lockin.app.presentation.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.data.local.models.EnergyScoreEntity
import com.lockin.app.data.local.models.WorkoutEntity
import com.lockin.app.domain.usecase.StreakResult
import com.lockin.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onRequestHealthPermissions: () -> Unit = {},
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToCoach: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // ── Diagnostic logging ──
    LaunchedEffect(state.stepsToday, state.healthPermissionsGranted) {
        Log.d("TODAY_SCREEN", "━━━ TodayScreen recomposed ━━━")
        Log.d("TODAY_SCREEN", "healthPermissionsGranted=${state.healthPermissionsGranted}")
        Log.d("TODAY_SCREEN", "stepsToday=${state.stepsToday}")
        Log.d("TODAY_SCREEN", "sleepData=${state.sleepData}")
        Log.d("TODAY_SCREEN", "latestHeartRateBpm=${state.latestHeartRateBpm}")
        Log.d("TODAY_SCREEN", "hrvValue=${state.hrvValue}")
        Log.d("TODAY_SCREEN", "restingHeartRate=${state.restingHeartRate}")
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LockinBackground),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ───────────────────────────────────────────────
            item { HeaderSection(onRefresh = { viewModel.refresh() }) }

            // ── Energy Score ─────────────────────────────────────────
            item { EnergyScoreCard(state.energyScore, state.recommendation) }

            // ── Streak Card ──────────────────────────────────────────
            item { StreakCard(state.streak) }

            // ── Streak milestone / warning banners ───────────────────
            item { StreakBanners(state.streak) }

            // ── Quick Actions ────────────────────────────────────────
            item {
                QuickActionsRow(
                    onWorkout = onNavigateToWorkout,
                    onCoach = onNavigateToCoach,
                    onProgress = onNavigateToProgress
                )
            }

            // ── Health Summary ────────────────────────────────────────
            item {
                HealthSummaryCard(
                    state = state,
                    onRequestHealth = onRequestHealthPermissions
                )
            }

            // ── Watch Status ─────────────────────────────────────────
            item { WatchStatusChip(state.watchConnected) }

            // ── Recent Workouts ──────────────────────────────────────
            if (state.recentWorkouts.isNotEmpty()) {
                item { SectionHeader(title = "Recent Workouts") }
                items(state.recentWorkouts) { workout ->
                    WorkoutCard(workout)
                }
            } else {
                item { EmptyWorkoutsCard(onNavigateToWorkout) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HeaderSection(onRefresh: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LOCKIN",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = LockinPrimaryAccent
            )
            Text(
                text = "Today's Dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = LockinTextSecondary
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh health data",
                tint = LockinTextSecondary
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Energy Score Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EnergyScoreCard(energy: EnergyScoreEntity?, recommendation: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(LockinSurface, LockinSurface.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(LockinPrimaryAccent.copy(alpha = 0.3f), LockinSubtleBorder)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ENERGY SCORE", style = MaterialTheme.typography.labelMedium, color = LockinTextSecondary, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    val score = energy?.overallScore ?: 0
                    val scoreColor = when {
                        score >= 75 -> LockinSecondaryAccent
                        score >= 50 -> LockinPrimaryAccent
                        score >= 25 -> LockinWarning
                        else -> LockinError
                    }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(listOf(scoreColor.copy(alpha = 0.2f), Color.Transparent)),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(3.dp, scoreColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (energy != null) "$score" else "—",
                            style = MaterialTheme.typography.displayLarge,
                            color = scoreColor
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                val label = energy?.details?.takeIf { it.isNotBlank() } ?: "No data yet"
                Text(label, style = MaterialTheme.typography.titleMedium, color = if (energy != null) LockinTextPrimary else LockinTextSecondary)

                if (recommendation != null && recommendation != energy?.details) {
                    Spacer(Modifier.height(4.dp))
                    Text(recommendation, style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                }

                Spacer(Modifier.height(20.dp))
                if (energy != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SubMetric("Sleep", "${energy.sleepScore}%", Icons.Outlined.Bedtime)
                        SubMetric("HRV", "${energy.hrvScore}", Icons.Outlined.MonitorHeart)
                        SubMetric("Recovery", "${energy.trainingLoadScore}%", Icons.Outlined.FitnessCenter)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubMetric(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = LockinTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LockinTextSecondary)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Quick Actions
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(onWorkout: () -> Unit, onCoach: () -> Unit, onProgress: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionChip("Workout", Icons.Default.FitnessCenter, LockinPrimaryAccent, Modifier.weight(1f), onWorkout)
        QuickActionChip("AI Coach", Icons.Default.Psychology, LockinSecondaryAccent, Modifier.weight(1f), onCoach)
        QuickActionChip("Progress", Icons.AutoMirrored.Filled.TrendingUp, LockinWarning, Modifier.weight(1f), onProgress)
    }
}

@Composable
private fun QuickActionChip(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = LockinTextPrimary)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Health Summary Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HealthSummaryCard(state: TodayUiState, onRequestHealth: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Health Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
            Spacer(Modifier.height(12.dp))

            if (!state.healthPermissionsGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, tint = LockinWarning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enable Health Connect to see your data.", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRequestHealth) {
                        Text("Enable", color = LockinPrimaryAccent)
                    }
                }
            } else {
                // ── RAW DEBUG (remove later) ──
                Text(
                    text = "DEBUG: steps=${state.stepsToday} sleep=${state.sleepData?.totalMinutes}min HR=${state.latestHeartRateBpm} HRV=${state.hrvValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LockinWarning
                )
                Spacer(Modifier.height(8.dp))
                // 2x2 grid of health metrics
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HealthMetricMini(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = "Steps",
                        value = if (state.stepsToday != null) "%,d".format(state.stepsToday) else "—",
                        color = LockinPrimaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                    HealthMetricMini(
                        icon = Icons.Outlined.Bedtime,
                        label = "Sleep",
                        value = state.sleepData?.let {
                            val h = it.totalMinutes / 60
                            val m = it.totalMinutes % 60
                            "${h}h ${m}m"
                        } ?: "—",
                        color = Color(0xFF818CF8),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HealthMetricMini(
                        icon = Icons.Outlined.MonitorHeart,
                        label = "Heart Rate",
                        value = state.latestHeartRateBpm?.let { "$it BPM" } ?: "—",
                        color = LockinError,
                        modifier = Modifier.weight(1f)
                    )
                    HealthMetricMini(
                        icon = Icons.Outlined.Insights,
                        label = "HRV",
                        value = state.hrvValue?.let { "%.0f ms".format(it) } ?: "—",
                        color = LockinSecondaryAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.restingHeartRate != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = LockinError.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Resting HR: ${state.restingHeartRate} BPM", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMetricMini(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LockinSurfaceElevated
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = LockinTextPrimary)
                Text(label, style = MaterialTheme.typography.labelSmall, color = LockinTextSecondary)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Watch Status
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WatchStatusChip(connected: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LockinSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Watch,
                contentDescription = "Watch",
                tint = if (connected) LockinSecondaryAccent else LockinTextDisabled,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (connected) LockinSecondaryAccent else LockinTextDisabled)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (connected) "Watch Connected" else "Watch Not Connected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) LockinTextPrimary else LockinTextSecondary
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = LockinTextPrimary, modifier = Modifier.padding(top = 4.dp))
}

// ══════════════════════════════════════════════════════════════════════════════
// Workout Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WorkoutCard(workout: WorkoutEntity) {
    val typeIcon = when (workout.type.uppercase()) {
        "STRENGTH" -> Icons.Default.FitnessCenter
        "CARDIO" -> Icons.AutoMirrored.Filled.DirectionsWalk
        else -> Icons.Default.SportsMma
    }
    val statusColor = when (workout.status.uppercase()) {
        "COMPLETED" -> LockinSecondaryAccent
        "IN_PROGRESS" -> LockinPrimaryAccent
        else -> LockinTextDisabled
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(LockinSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon, contentDescription = workout.type, tint = LockinPrimaryAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name.ifBlank { "Workout" }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = LockinTextPrimary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(workout.type.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                    workout.plannedDate?.let {
                        Text(" · ${it.format(DateTimeFormatter.ofPattern("MMM d"))}", style = MaterialTheme.typography.bodySmall, color = LockinTextSecondary)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                Text(workout.status.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = statusColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Empty State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyWorkoutsCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = LockinTextDisabled, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("No workouts yet", style = MaterialTheme.typography.titleMedium, color = LockinTextSecondary)
            Spacer(Modifier.height(4.dp))
            Text("Start your first workout to see it here", style = MaterialTheme.typography.bodySmall, color = LockinTextDisabled)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onStart) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start Workout")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Streak Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StreakCard(streak: StreakResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LockinSurface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🔥", style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    text = if (streak.currentStreakDays > 0) "${streak.currentStreakDays} day streak" else "No active streak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LockinTextPrimary
                )
                Text(
                    text = if (streak.currentStreakDays > 0) "Longest: ${streak.longestStreakDays} days" else "Start training today!",
                    style = MaterialTheme.typography.bodySmall,
                    color = LockinTextSecondary
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Streak Banners
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StreakBanners(streak: StreakResult) {
    val today = LocalDate.now()
    val milestones = setOf(3, 7, 14, 30, 50, 100)
    val isMilestone = streak.currentStreakDays in milestones

    AnimatedVisibility(visible = isMilestone, enter = fadeIn() + slideInVertically()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LockinPRGold.copy(alpha = 0.12f))) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎉", style = MaterialTheme.typography.titleLarge)
                Text("${streak.currentStreakDays}-day streak! Keep it up!", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LockinPRGold)
            }
        }
    }

    val daysSince = streak.lastWorkoutDate?.let { ChronoUnit.DAYS.between(it, today).toInt() }
    val showWarning = !isMilestone && daysSince != null && daysSince >= 2 && streak.currentStreakDays >= 3

    AnimatedVisibility(visible = showWarning, enter = fadeIn() + slideInVertically()) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LockinWarning.copy(alpha = 0.12f))) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚠️", style = MaterialTheme.typography.titleLarge)
                Text("Don't break your streak — last workout was $daysSince days ago.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LockinWarning)
            }
        }
    }
}
