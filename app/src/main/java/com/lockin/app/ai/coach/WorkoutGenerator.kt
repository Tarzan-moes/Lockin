package com.lockin.app.ai.coach

import android.util.Log
import com.lockin.app.ai.AIModel
import com.lockin.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * WorkoutGenerator – AI-powered workout creation.
 *
 * Uses the on-device LLM to generate full workout plans in JSON format,
 * then parses them into the app's domain model (WorkoutPlan + PlannedExercise).
 * Includes a robust fallback system so the user always gets a workout
 * even if the model or JSON parsing fails.
 */
class WorkoutGenerator(
    private val model: AIModel
) {
    companion object {
        private const val TAG = "WorkoutGenerator"
    }

    /**
     * Generate a personalized workout using AI.
     *
     * @param user            The user's profile (goal, experience, equipment).
     * @param energyScore     Today's energy score (0–100).
     * @param recentWorkouts  Last few completed workouts for variety.
     * @param targetDuration  Desired workout length in minutes.
     * @return A [GeneratedWorkout] containing the plan and exercises.
     */
    suspend fun generateWorkout(
        user: UserProfile,
        energyScore: Int,
        recentWorkouts: List<WorkoutPlan> = emptyList(),
        targetDuration: Int = 60
    ): GeneratedWorkout = withContext(Dispatchers.Default) {
        // Ensure model is ready
        if (!model.isLoaded()) {
            val ok = model.initialize()
            if (!ok) {
                Log.w(TAG, "Model not available, returning fallback workout")
                return@withContext createFallbackWorkout(user)
            }
        }

        val prompt = buildWorkoutPrompt(user, energyScore, recentWorkouts, targetDuration)

        try {
            val response = model.generate(prompt)
            parseWorkoutFromJson(response)
        } catch (e: Exception) {
            Log.e(TAG, "Workout generation/parsing failed: ${e.message}", e)
            createFallbackWorkout(user)
        }
    }

    // ── Prompt building ──────────────────────────────────────────────────

    private fun buildWorkoutPrompt(
        user: UserProfile,
        energyScore: Int,
        recentWorkouts: List<WorkoutPlan>,
        targetDuration: Int
    ): String = buildString {
        appendLine("You are an expert fitness coach. Generate a workout plan as strict JSON.")
        appendLine()
        appendLine("=== User Profile ===")
        appendLine("Goal: ${user.primaryGoal.name.lowercase().replace('_', ' ')}")
        appendLine("Experience: ${user.experienceLevel.name.lowercase()}")
        appendLine("Equipment: ${user.availableEquipment.joinToString { it.name.lowercase().replace('_', ' ') }.ifEmpty { "full gym" }}")
        user.weightKg?.let { appendLine("Body weight: ${it}kg") }
        appendLine()
        appendLine("=== Current State ===")
        appendLine("Energy score: $energyScore/100")
        appendLine("Target duration: ${targetDuration} minutes")
        appendLine()

        if (recentWorkouts.isNotEmpty()) {
            appendLine("=== Recent Workouts (avoid repeating) ===")
            recentWorkouts.take(3).forEach { w ->
                appendLine("- ${w.name}: ${w.exercises.joinToString { it.exerciseName }}")
            }
            appendLine()
        }

        appendLine("=== Requirements ===")
        appendLine("- Include a warm-up section (5 min)")
        appendLine("- Main session with progressive overload principles")
        appendLine("- Cool-down section (5 min)")
        appendLine("- Adjust intensity based on energy score (lower energy → less volume)")
        appendLine("- Use exercises appropriate for the user's experience level")
        appendLine()
        appendLine("=== Output Format (strict JSON) ===")
        appendLine("""
            {
              "name": "Workout Name",
              "description": "Brief description",
              "estimatedDurationMinutes": $targetDuration,
              "difficulty": "MODERATE",
              "tags": ["push", "chest"],
              "exercises": [
                {
                  "exerciseName": "Exercise Name",
                  "targetSets": 3,
                  "targetRepsMin": 8,
                  "targetRepsMax": 12,
                  "restSeconds": 90,
                  "notes": "Form cue or note"
                }
              ]
            }
        """.trimIndent())
        appendLine()
        appendLine("Respond with ONLY the JSON object, no other text.")
    }

    // ── JSON parsing ─────────────────────────────────────────────────────

    /**
     * Extract the first JSON object from the model response and parse it
     * into domain models.
     */
    private fun parseWorkoutFromJson(response: String): GeneratedWorkout {
        // Find the first JSON block in the response
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            Log.w(TAG, "No JSON found in response, using fallback")
            throw IllegalArgumentException("No JSON block found in model response")
        }

        val jsonString = response.substring(jsonStart, jsonEnd + 1)
        val json = JSONObject(jsonString)

        val workoutId = UUID.randomUUID().toString()

        // Parse exercises
        val exercisesJson = json.optJSONArray("exercises") ?: JSONArray()
        val exercises = mutableListOf<PlannedExercise>()

        for (i in 0 until exercisesJson.length()) {
            val ex = exercisesJson.getJSONObject(i)
            exercises.add(
                PlannedExercise(
                    exerciseId = UUID.randomUUID().toString(),
                    exerciseName = ex.optString("exerciseName", "Unknown Exercise"),
                    targetSets = ex.optInt("targetSets", 3),
                    targetReps = ex.optInt("targetRepsMin", 8)..ex.optInt("targetRepsMax", 12),
                    restSeconds = ex.optInt("restSeconds", 90),
                    notes = ex.optString("notes", "")
                )
            )
        }

        // Parse difficulty
        val difficultyStr = json.optString("difficulty", "MODERATE").uppercase()
        val difficulty = try {
            Difficulty.valueOf(difficultyStr)
        } catch (_: Exception) {
            Difficulty.MODERATE
        }

        // Parse tags
        val tagsJson = json.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsJson != null) {
            for (i in 0 until tagsJson.length()) {
                tags.add(tagsJson.optString(i, ""))
            }
        }

        val workout = WorkoutPlan(
            id = workoutId,
            name = json.optString("name", "AI Generated Workout"),
            description = json.optString("description", "AI-generated personalized workout"),
            exercises = exercises,
            estimatedDurationMinutes = json.optInt("estimatedDurationMinutes", 60),
            difficulty = difficulty,
            tags = tags + "ai-generated"
        )

        return GeneratedWorkout(workout = workout, exercises = exercises)
    }

    // ── Fallback ─────────────────────────────────────────────────────────

    /**
     * Create a sensible default workout when AI generation fails.
     * Adjusts based on the user's goal and equipment.
     */
    fun createFallbackWorkout(user: UserProfile): GeneratedWorkout {
        val workoutId = UUID.randomUUID().toString()

        val exercises = when (user.primaryGoal) {
            FitnessGoal.STRENGTH -> listOf(
                PlannedExercise(exerciseName = "Barbell Back Squat", targetSets = 5, targetReps = 3..5, restSeconds = 180, notes = "Focus on bracing, controlled descent"),
                PlannedExercise(exerciseName = "Bench Press", targetSets = 5, targetReps = 3..5, restSeconds = 180, notes = "Full arch, tight shoulder blades"),
                PlannedExercise(exerciseName = "Barbell Row", targetSets = 4, targetReps = 5..8, restSeconds = 120, notes = "Pull to lower chest"),
                PlannedExercise(exerciseName = "Overhead Press", targetSets = 3, targetReps = 5..8, restSeconds = 120, notes = "Brace core, squeeze glutes"),
                PlannedExercise(exerciseName = "Romanian Deadlift", targetSets = 3, targetReps = 8..10, restSeconds = 90, notes = "Hinge at hips, soft knees")
            )
            FitnessGoal.HYPERTROPHY -> listOf(
                PlannedExercise(exerciseName = "Incline Dumbbell Press", targetSets = 4, targetReps = 8..12, restSeconds = 90, notes = "Control the eccentric"),
                PlannedExercise(exerciseName = "Cable Row", targetSets = 4, targetReps = 10..12, restSeconds = 75, notes = "Squeeze at the peak"),
                PlannedExercise(exerciseName = "Leg Press", targetSets = 4, targetReps = 10..15, restSeconds = 90, notes = "Full range of motion"),
                PlannedExercise(exerciseName = "Lateral Raises", targetSets = 4, targetReps = 12..15, restSeconds = 60, notes = "Slight lean forward"),
                PlannedExercise(exerciseName = "Hammer Curls", targetSets = 3, targetReps = 10..12, restSeconds = 60, notes = "No swinging"),
                PlannedExercise(exerciseName = "Tricep Pushdowns", targetSets = 3, targetReps = 10..15, restSeconds = 60, notes = "Lock out at bottom")
            )
            FitnessGoal.WEIGHT_LOSS -> listOf(
                PlannedExercise(exerciseName = "Goblet Squat", targetSets = 3, targetReps = 12..15, restSeconds = 45, notes = "Keep rest short"),
                PlannedExercise(exerciseName = "Push-Ups", targetSets = 3, targetReps = 10..15, restSeconds = 45, notes = "Full range of motion"),
                PlannedExercise(exerciseName = "Dumbbell Lunges", targetSets = 3, targetReps = 12..15, restSeconds = 45, notes = "Alternate legs"),
                PlannedExercise(exerciseName = "Plank", targetSets = 3, targetReps = 30..60, restSeconds = 30, notes = "Hold for seconds, not reps"),
                PlannedExercise(exerciseName = "Mountain Climbers", targetSets = 3, targetReps = 20..30, restSeconds = 30, notes = "Keep hips level"),
                PlannedExercise(exerciseName = "Kettlebell Swings", targetSets = 3, targetReps = 15..20, restSeconds = 45, notes = "Hip hinge, explosive")
            )
            else -> listOf(
                PlannedExercise(exerciseName = "Barbell Squat", targetSets = 3, targetReps = 8..12, restSeconds = 90, notes = "Warm up thoroughly"),
                PlannedExercise(exerciseName = "Bench Press", targetSets = 3, targetReps = 8..12, restSeconds = 90, notes = "Control the bar"),
                PlannedExercise(exerciseName = "Pull-Ups", targetSets = 3, targetReps = 6..10, restSeconds = 90, notes = "Use band assist if needed"),
                PlannedExercise(exerciseName = "Dumbbell Shoulder Press", targetSets = 3, targetReps = 8..12, restSeconds = 75, notes = "Don't flare elbows"),
                PlannedExercise(exerciseName = "Plank", targetSets = 3, targetReps = 30..60, restSeconds = 30, notes = "Hold for seconds")
            )
        }

        val name = when (user.primaryGoal) {
            FitnessGoal.STRENGTH -> "Strength – Full Body"
            FitnessGoal.HYPERTROPHY -> "Hypertrophy – Upper/Lower Split"
            FitnessGoal.WEIGHT_LOSS -> "Fat Loss – Circuit Training"
            else -> "General Fitness – Full Body"
        }

        val workout = WorkoutPlan(
            id = workoutId,
            name = name,
            description = "Auto-generated fallback workout for ${user.primaryGoal.name.lowercase().replace('_', ' ')}",
            exercises = exercises,
            estimatedDurationMinutes = 55,
            difficulty = when (user.experienceLevel) {
                ExperienceLevel.BEGINNER -> Difficulty.EASY
                ExperienceLevel.INTERMEDIATE -> Difficulty.MODERATE
                ExperienceLevel.ADVANCED -> Difficulty.HARD
                ExperienceLevel.ELITE -> Difficulty.BRUTAL
            },
            tags = listOf("ai-generated", "fallback", user.primaryGoal.name.lowercase())
        )

        return GeneratedWorkout(workout = workout, exercises = exercises)
    }
}

/**
 * Container for an AI-generated workout and its exercises.
 */
data class GeneratedWorkout(
    val workout: WorkoutPlan,
    val exercises: List<PlannedExercise>
)

