package com.lockin.app.presentation.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.ui.theme.*

/**
 * WelcomeScreen – A simple welcome / landing screen for the app.
 *
 * Displays the app name, tagline, and a "Get Started" CTA button.
 * This is a minimal but working Compose UI per the Part 2 spec.
 * Navigation to onboarding or main flow is handled by the caller.
 */
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App name
        Text(
            text = "LOCKIN",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            ),
            color = LockinPrimaryAccent
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Your AI-Powered Fitness Companion",
            style = MaterialTheme.typography.bodyLarge,
            color = LockinTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Get Started button
        Button(
            onClick = onGetStartedClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LockinPrimaryAccent,
                contentColor = LockinOnPrimary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version info
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = LockinTextDisabled
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun WelcomeScreenPreview() {
    LockinTheme {
        WelcomeScreen()
    }
}

