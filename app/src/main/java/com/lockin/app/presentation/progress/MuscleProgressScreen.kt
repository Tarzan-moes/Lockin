package com.lockin.app.presentation.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.MuscleGroup
import com.lockin.app.domain.usecase.MuscleProgressPoint
import com.lockin.app.ui.theme.LockinBackground
import com.lockin.app.ui.theme.LockinPrimaryAccent
import com.lockin.app.ui.theme.LockinSurface
import com.lockin.app.ui.theme.LockinTextPrimary
import com.lockin.app.ui.theme.LockinTextSecondary
import kotlin.math.abs

@Composable
fun MuscleProgressScreen(
    viewModel: MuscleProgressViewModel = hiltViewModel()
) {
    val selectedMuscle: MuscleGroup =
        viewModel.selectedMuscle.collectAsState(initial = MuscleGroup.CHEST).value
    val points: List<MuscleProgressPoint> =
        viewModel.progressPoints.collectAsState(initial = emptyList<MuscleProgressPoint>()).value
    var selectedPoint by remember(points) { mutableStateOf<MuscleProgressPoint?>(points.lastOrNull()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LockinBackground)
    ) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MuscleGroup.entries.forEach { muscle ->
                FilterChip(
                    selected = selectedMuscle == muscle,
                    onClick = {
                        viewModel.selectMuscle(muscle)
                        selectedPoint = null
                    },
                    label = { Text(muscle.name.replace("_", " ")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LockinPrimaryAccent.copy(alpha = 0.18f),
                        selectedLabelColor = LockinPrimaryAccent
                    )
                )
            }
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No data yet for ${selectedMuscle.name.lowercase().replaceFirstChar { it.uppercase() }}. Start training to see progress!",
                    color = LockinTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val xSpacing = if (points.size <= 1) 1f else 1f / (points.size - 1)
            val maxVolume = points.maxOf { it.totalVolume }.coerceAtLeast(1f)
            val dotPositions = remember(points) { mutableListOf<Pair<Offset, MuscleProgressPoint>>() }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LockinSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(12.dp)
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val hit = dotPositions.minByOrNull { (pos, _) ->
                                    abs(pos.x - offset.x) + abs(pos.y - offset.y)
                                }
                                if (hit != null && (abs(hit.first.x - offset.x) <= 24f && abs(hit.first.y - offset.y) <= 24f)) {
                                    selectedPoint = hit.second
                                }
                            }
                        }
                ) {
                    dotPositions.clear()
                    val path = Path()

                    points.forEachIndexed { index, point ->
                        val x = size.width * (index * xSpacing)
                        val y = size.height - ((point.totalVolume / maxVolume) * size.height)
                        val p = Offset(x, y)
                        dotPositions.add(p to point)

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(path = path, color = LockinPrimaryAccent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                    dotPositions.forEach { (offset, point) ->
                        val selected = selectedPoint?.date == point.date
                        drawCircle(
                            color = if (selected) Color.White else LockinPrimaryAccent,
                            radius = if (selected) 10f else 7f,
                            center = offset
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedPoint != null) {
            selectedPoint?.let { point ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LockinSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = point.date.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LockinTextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        point.exercises.forEach { detail ->
                            Text(
                                text = "${detail.exerciseName}: ${detail.weightKg}kg × ${detail.reps} reps @ RPE ${detail.rpe ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LockinTextSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Total volume: ${"%.0f".format(point.totalVolume)} kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = LockinPrimaryAccent
                        )
                    }
                }
            }
        }
    }
}

