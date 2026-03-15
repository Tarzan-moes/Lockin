package com.lockin.app.presentation.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.*

/**
 * ExerciseLibraryScreen – Browse and search the exercise database.
 */
@Composable
fun ExerciseLibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📖 Exercise Library",
            style = MaterialTheme.typography.headlineMedium,
            color = LockinTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Browse exercises by muscle group, search by name, view form guides.",
            style = MaterialTheme.typography.bodyMedium,
            color = LockinTextSecondary
        )
    }
}

