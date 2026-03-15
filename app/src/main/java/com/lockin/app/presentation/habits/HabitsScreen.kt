package com.lockin.app.presentation.habits

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.*

/**
 * HabitsScreen – Daily habit checklist.
 */
@Composable
fun HabitsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✅ Daily Habits",
            style = MaterialTheme.typography.headlineMedium,
            color = LockinTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Build consistency with daily health and wellness habits.",
            style = MaterialTheme.typography.bodyMedium,
            color = LockinTextSecondary
        )
    }
}

