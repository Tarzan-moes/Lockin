package com.lockin.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.lockin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onStartWorkout: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
    )

    var showAddSheet by remember { mutableStateOf(false) }
    
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(uiState.availableTemplates) { template ->
                    ListItem(
                        headlineContent = { Text(template.name) },
                        supportingContent = { Text("${template.difficulty} · ${template.estimatedDurationMinutes}min") },
                        modifier = Modifier.clickable {
                            viewModel.scheduleWorkout(template.id, uiState.selectedDate)
                            showAddSheet = false
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Schedule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, "Schedule Workout")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Calendar Header (Days of week)
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in daysOfWeek) {
                    Text(
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    )
                }
            }

            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(day, isSelected = day.date == uiState.selectedDate) { clicked ->
                        viewModel.onDateSelected(clicked.date)
                    }
                },
                monthHeader = { month ->
                    val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                    // Month header content if needed
                }
            )

            // Scheduled workouts list for selected day
            val schedules = uiState.schedules[uiState.selectedDate] ?: emptyList()
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text(
                        "Scheduled on ${uiState.selectedDate}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                if (schedules.isEmpty()) {
                     item { Text("No workouts scheduled.", color = LockinTextSecondary) }
                }

                items(schedules) { schedule ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(schedule.workoutName)
                                Text(
                                    "${schedule.difficulty} · ${schedule.durationMinutes}min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LockinTextSecondary
                                )
                                if (schedule.isCompleted) {
                                    Text("Completed", color = LockinSuccess)
                                }
                            }
                            Row {
                                if (!schedule.isCompleted) {
                                    Button(onClick = { onStartWorkout(schedule.workoutTemplateId) }) {
                                        Text("Start")
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteSchedule(schedule.id) }) {
                                    Icon(Icons.Default.Delete, "Delete")
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
fun Day(day: CalendarDay, isSelected: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (isSelected) LockinPrimaryAccent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (isSelected) LockinOnPrimary else LockinTextPrimary
        )
    }
}

