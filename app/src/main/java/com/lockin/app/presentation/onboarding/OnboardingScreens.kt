package com.lockin.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.*

/**
 * Onboarding Screens – Placeholder composables for the first-time user flow.
 *
 * Each screen follows the same skeleton: title, description, and a "Continue" CTA.
 * The actual onboarding logic (form inputs, permission requests, animations)
 * will be built in a later part. For now they demonstrate the navigation flow
 * and use the Lockin color system.
 */

// ── Shared layout for all onboarding steps ──────────────────────────────────

@Composable
private fun OnboardingStepLayout(
    stepTitle: String,
    stepDescription: String,
    buttonText: String = "Continue",
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .primaryGradientBackground()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stepTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = LockinTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stepDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = LockinTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = LockinPrimaryAccent,
                contentColor = LockinOnPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = buttonText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Individual onboarding screens ────────────────────────────────────────────

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Welcome to Lockin",
        stepDescription = "Your AI-powered hybrid training engine.\nBuild strength. Track progress. Stay locked in.",
        buttonText = "Get Started",
        onAction = onContinue
    )
}

@Composable
fun ProfileSetupScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Set Up Your Profile",
        stepDescription = "Tell us about your goals, experience level, and available equipment so we can personalize your training.",
        onAction = onContinue
    )
}

@Composable
fun HealthPermissionsPlaceholderScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Health Permissions",
        stepDescription = "Connect to Health Connect to sync sleep, heart rate, and activity data for smarter coaching.",
        buttonText = "Grant Access",
        onAction = onContinue
    )
}

@Composable
fun WatchPairingScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Pair Your Watch",
        stepDescription = "Connect a Galaxy Watch or Wear OS device for real-time heart rate and rep tracking during workouts.",
        buttonText = "Skip for Now",
        onAction = onContinue
    )
}

@Composable
fun AiCoachIntroScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Meet Your AI Coach",
        stepDescription = "Get personalized workout adjustments, form feedback, and recovery insights — all running privately on your device.",
        onAction = onContinue
    )
}

@Composable
fun FirstWorkoutScreen(onContinue: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Your First Workout",
        stepDescription = "Based on your profile, we've generated a starter workout plan. Let's take a quick look at how it works.",
        buttonText = "See My Plan",
        onAction = onContinue
    )
}

@Composable
fun LoggingTutorialScreen(onFinish: () -> Unit) {
    OnboardingStepLayout(
        stepTitle = "Log Your Sets",
        stepDescription = "Tap to log weight, reps, and RPE for each set. The AI learns from every session to optimize your future workouts.",
        buttonText = "Let's Go!",
        onAction = onFinish
    )
}

