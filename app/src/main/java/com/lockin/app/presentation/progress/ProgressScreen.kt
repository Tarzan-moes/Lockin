package com.lockin.app.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.ui.theme.*

/**
 * ProgressScreen – Analytics dashboard with placeholder trend cards.
 */
@Composable
fun ProgressScreen(
    viewModel: MuscleProgressViewModel = hiltViewModel()
) {
    val weeklyRows by viewModel.weeklyVolumeRows.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LockinBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                "Progress",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary
            )
        }

        item {
            Text(
                "Track muscle-specific strength trends and weekly volume.",
                style = MaterialTheme.typography.bodyMedium,
                color = LockinTextSecondary
            )
        }

        item {
            Text(
                "Strength Progress by Muscle",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = LockinTextPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        item {
            MuscleProgressScreen(viewModel = viewModel)
        }

        item {
            Text(
                "Weekly Volume (This Week vs Last Week)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LockinTextPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        if (weeklyRows.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LockinSurface)
                ) {
                    Text(
                        "No weekly volume yet. Complete workouts to populate this section.",
                        modifier = Modifier.padding(16.dp),
                        color = LockinTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(weeklyRows) { row ->
                WeeklyVolumeRowCard(
                    muscle = row.muscleGroup.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                    thisWeek = row.thisWeekVolume,
                    lastWeek = row.lastWeekVolume
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun WeeklyVolumeRowCard(
    muscle: String,
    thisWeek: Float,
    lastWeek: Float
) {
    val changePercent = if (lastWeek <= 0f) null else ((thisWeek - lastWeek) / lastWeek) * 100f
    val trend = when {
        changePercent == null -> "NEW"
        changePercent >= 0f -> "▲ ${"%.0f".format(changePercent)}%"
        else -> "▼ ${"%.0f".format(kotlin.math.abs(changePercent))}%"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LockinSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(muscle, style = MaterialTheme.typography.titleMedium, color = LockinTextPrimary)
                Text(trend, style = MaterialTheme.typography.labelMedium, color = LockinPrimaryAccent)
            }
            Spacer(Modifier.height(6.dp))
            Text("This week: ${"%.0f".format(thisWeek)} kg", color = LockinTextSecondary)
            Text("Last week: ${"%.0f".format(lastWeek)} kg", color = LockinTextSecondary)
        }
    }
}
