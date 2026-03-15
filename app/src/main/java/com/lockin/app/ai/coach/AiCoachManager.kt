package com.lockin.app.ai.coach

import android.util.Log
import com.lockin.app.ai.AIModel
import com.lockin.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AiCoachManager – High-level orchestrator for AI coaching features.
 *
 * The presentation layer talks to this manager instead of directly
 * touching the inference engine. This keeps AI logic out of ViewModels
 * and allows us to add prompt engineering, context management, and
 * conversation history without touching the UI layer.
 */
interface AiCoachManager {

    /** Initialize the underlying AI model (lazy-loaded on first use). */
    suspend fun initialize()

    /** Ask the AI coach a free-form question and get a response. */
    suspend fun askCoach(userMessage: String): String

    /**
     * Context-aware chat that considers user profile and recent workouts.
     */
    suspend fun chat(
        userMessage: String,
        user: UserProfile? = null,
        recentWorkouts: List<WorkoutPlan>? = null
    ): String

    /**
     * Nutrition-focused chat.
     */
    suspend fun chatNutrition(
        userMessage: String,
        user: UserProfile? = null
    ): String

    /**
     * Suggest the next workout based on user state and energy score.
     */
    suspend fun suggestNextWorkout(
        user: UserProfile,
        recentWorkouts: List<WorkoutPlan>,
        energyScore: Int
    ): String

    /** Get a workout suggestion based on the user's current state. */
    suspend fun suggestWorkoutAdjustment(
        currentFatigue: Float,
        recentVolume: Float
    ): String

    /** Get a motivational message or tip of the day. */
    suspend fun getDailyMotivation(): String

    /** Analyze a completed workout and suggest specific improvements. */
    suspend fun reviewWorkoutAndSuggestImprovements(
        workoutSummary: String,
        userGoal: String
    ): String

    /** Clear all conversation history. */
    fun clearConversation()

    /** Check if the AI models are loaded and ready. */
    fun isReady(): Boolean

    /** Release the model and free resources. */
    fun release()
}

/**
 * Stub implementation that returns placeholder responses.
 * Used for testing or when the real model is not available.
 */
class AiCoachManagerStub : AiCoachManager {
    override suspend fun initialize() { /* no-op */ }

    override suspend fun askCoach(userMessage: String): String =
        "🤖 AI Coach is not yet connected. Your message: \"$userMessage\""

    override suspend fun chat(
        userMessage: String,
        user: UserProfile?,
        recentWorkouts: List<WorkoutPlan>?
    ): String = askCoach(userMessage)

    override suspend fun chatNutrition(
        userMessage: String,
        user: UserProfile?
    ): String = "Nutrition AI not available in stub."

    override suspend fun suggestNextWorkout(
        user: UserProfile,
        recentWorkouts: List<WorkoutPlan>,
        energyScore: Int
    ): String = "Based on your energy score of $energyScore, I suggest a moderate push session today."

    override suspend fun suggestWorkoutAdjustment(
        currentFatigue: Float,
        recentVolume: Float
    ): String = "Based on your fatigue level, consider reducing volume by 10% today."

    override suspend fun getDailyMotivation(): String =
        "Consistency beats intensity. Show up today. 💪"

    override suspend fun reviewWorkoutAndSuggestImprovements(
        workoutSummary: String,
        userGoal: String
    ): String =
        "Unable to analyze workout right now. AI model is in stub mode.\n\n$workoutSummary"

    override fun clearConversation() { /* no-op */ }
    override fun isReady(): Boolean = false
    override fun release() { /* no-op */ }
}

// ── Sealed message hierarchy for conversation history ────────────────────────

sealed class Message(val content: String) {
    class User(content: String) : Message(content)
    class Assistant(content: String) : Message(content)
}

/**
 * Real AI Coach implementation backed by a [AIModel] (e.g., GemmaModel).
 *
 * Builds context-aware prompts using the user's profile, recent workouts,
 * and energy score. Maintains an in-memory conversation history for
 * multi-turn interactions. All inference runs on background dispatchers.
 */
class AiCoachManagerImpl(
    private val model: AIModel
) : AiCoachManager {

    companion object {
        private const val TAG = "AiCoachManagerImpl"
        private const val MAX_HISTORY_SIZE = 10

        /** System prompt that defines the coach's behaviour. */
        private val SYSTEM_PROMPT = """
            You are an expert AI fitness coach inside the Lockin training app.
            Your role:
            - Provide evidence-based workout advice, form tips, and recovery guidance.
            - Be motivational, concise, and supportive.
            - Personalise recommendations based on the user's goal, experience, equipment, and energy.
            - Never recommend dangerous exercises for beginners.
            - Keep responses under 150 words unless the user asks for detail.
            - Format workout suggestions clearly with sets, reps, rest periods.
            - If you don't know something, say so honestly.
        """.trimIndent()

        val FALLBACK_MOTIVATIONS = listOf(
            "🔥 Consistency beats perfection. Show up today and give your best!",
            "💪 Your future self will thank you for the work you put in today.",
            "🏋️ Every rep counts. Every set matters. Let's make today great!",
            "⚡ Energy follows action. Start moving and the motivation will follow.",
            "🎯 Progress, not perfection. You're stronger than yesterday."
        )
    }

    private val conversationHistory = mutableListOf<Message>()

    // ── Lifecycle ────────────────────────────────────────────────────────

    override suspend fun initialize() {
        if (!model.isLoaded()) {
            Log.i(TAG, "Initializing AI model…")
            val success = model.initialize()
            Log.i(TAG, "Model initialization ${if (success) "succeeded" else "failed"}")
        }
    }

    override fun isReady(): Boolean = model.isLoaded()

    override fun clearConversation() {
        conversationHistory.clear()
    }

    override fun release() {
        conversationHistory.clear()
        model.release()
    }

    // ── Chat APIs ────────────────────────────────────────────────────────

    override suspend fun askCoach(userMessage: String): String =
        chat(userMessage, user = null, recentWorkouts = null)

    override suspend fun chat(
        userMessage: String,
        user: UserProfile?,
        recentWorkouts: List<WorkoutPlan>?
    ): String = withContext(Dispatchers.Default) {
        // Ensure model is loaded (lazy init)
        if (!model.isLoaded()) {
            val ok = model.initialize()
            if (!ok) return@withContext "⚠️ AI coach could not load. Please try again later."
        }

        // Record user message
        conversationHistory.add(Message.User(userMessage))
        trimHistory()

        // Build the full prompt
        val prompt = buildPrompt(user, recentWorkouts)

        // Generate
        val response = try {
            model.generate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e)
            "I'm having trouble thinking right now. Please try again in a moment."
        }

        // Record assistant response
        conversationHistory.add(Message.Assistant(response))
        trimHistory()

        response
    }

    override suspend fun chatNutrition(
        userMessage: String,
        user: UserProfile?
    ): String = withContext(Dispatchers.Default) {
        if (!model.isLoaded()) model.initialize()

        conversationHistory.add(Message.User(userMessage))
        trimHistory()

        val systemPrompt = """
            You are an expert sports nutritionist.
            User Profile:
            - Goal: ${user?.primaryGoal?.name ?: "General"}
            - Weight: ${user?.weightKg}kg
            
            Answer the following question about nutrition, macros, or diet.
            Keep it evidence-based and concise.
        """.trimIndent()

        val history = conversationHistory.joinToString("\n") { 
            when(it) {
                is Message.User -> "User: ${it.content}"
                is Message.Assistant -> "Coach: ${it.content}"
            }
        }
        
        val prompt = "$systemPrompt\n\nHistory:\n$history\n\nUser: $userMessage\nCoach:"

        val response = try {
            model.generate(prompt)
        } catch (e: Exception) {
            "I'm having trouble thinking about nutrition right now."
        }

        conversationHistory.add(Message.Assistant(response))
        trimHistory()
        response
    }

    override suspend fun suggestNextWorkout(
        user: UserProfile,
        recentWorkouts: List<WorkoutPlan>,
        energyScore: Int
    ): String = withContext(Dispatchers.Default) {
        if (!model.isLoaded()) {
            model.initialize()
        }

        val prompt = buildWorkoutSuggestionPrompt(user, recentWorkouts, energyScore)
        try {
            model.generate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Workout suggestion failed: ${e.message}", e)
            generateFallbackWorkoutSuggestion(user, energyScore)
        }
    }

    override suspend fun suggestWorkoutAdjustment(
        currentFatigue: Float,
        recentVolume: Float
    ): String = withContext(Dispatchers.Default) {
        if (!model.isLoaded()) {
            model.initialize()
        }

        val prompt = """
            $SYSTEM_PROMPT
            
            The user's current fatigue level is ${(currentFatigue * 100).toInt()}% and 
            recent training volume is ${recentVolume}kg. 
            Suggest how they should adjust today's workout.
        """.trimIndent()

        try {
            model.generate(prompt)
        } catch (e: Exception) {
            if (currentFatigue > 0.7f) {
                "You're quite fatigued (${(currentFatigue * 100).toInt()}%). " +
                        "Consider a deload session: reduce volume by 40% and focus on mobility work."
            } else {
                "Your fatigue is manageable. Proceed with your planned workout " +
                        "but listen to your body and reduce sets if needed."
            }
        }
    }

    override suspend fun getDailyMotivation(): String = withContext(Dispatchers.Default) {
        if (!model.isLoaded()) {
            model.initialize()
        }

        val prompt = """
            $SYSTEM_PROMPT
            
            Give a short, motivational fitness quote or tip for today. 
            Keep it to 1-2 sentences. Be energetic and positive.
        """.trimIndent()

        try {
            model.generate(prompt)
        } catch (e: Exception) {
            FALLBACK_MOTIVATIONS.random()
        }
    }

    override suspend fun reviewWorkoutAndSuggestImprovements(
        workoutSummary: String,
        userGoal: String
    ): String = withContext(Dispatchers.Default) {
        val systemPrompt = """
            You are an expert personal trainer and strength coach.
            Analyze the following completed workout and provide specific, actionable suggestions.

            User's goal: $userGoal

            Structure your response with these sections:

            💪 PROGRESSIVE OVERLOAD
            - For each exercise, suggest specific next-session targets (weight/reps).

            🔄 EXERCISE SELECTION
            - Are there redundant exercises? Missing muscle groups?
            - Suggest swaps or additions if relevant to the goal.

            📊 VOLUME & INTENSITY
            - Comment on total sets and intensity patterns.
            - Hypertrophy guideline: 10-20 hard sets per week per muscle.
            - Strength guideline: lower reps and higher intensity.

            😴 RECOVERY RECOMMENDATION
            - Use average RPE and total volume to suggest recovery timing.
            - Flag deload if workload looks too aggressive.

            Keep suggestions specific, numbered, and actionable.
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nWorkout data:\n$workoutSummary"

        return@withContext try {
            if (!model.isLoaded() && !model.initialize()) {
                "AI coach is not loaded yet. Add your model file to assets/models and try again."
            } else {
                model.generate(fullPrompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "reviewWorkout failed: ${e.message}", e)
            "Unable to analyze workout right now. Make sure the AI model is loaded."
        }
    }

    // ── Prompt building ──────────────────────────────────────────────────

    private fun buildPrompt(
        user: UserProfile?,
        recentWorkouts: List<WorkoutPlan>?
    ): String {
        val sb = StringBuilder()

        // System prompt
        sb.appendLine(SYSTEM_PROMPT)
        sb.appendLine()

        // User context
        if (user != null) {
            sb.appendLine("=== User Profile ===")
            sb.appendLine("Goal: ${user.primaryGoal.name.lowercase().replace('_', ' ')}")
            sb.appendLine("Experience: ${user.experienceLevel.name.lowercase()}")
            if (user.availableEquipment.isNotEmpty()) {
                sb.appendLine("Equipment: ${user.availableEquipment.joinToString { it.name.lowercase().replace('_', ' ') }}")
            }
            user.weightKg?.let { sb.appendLine("Weight: ${it}kg") }
            sb.appendLine()
        }

        // Recent workout context
        if (!recentWorkouts.isNullOrEmpty()) {
            sb.appendLine("=== Recent Workouts ===")
            recentWorkouts.take(3).forEach { workout ->
                sb.appendLine("- ${workout.name} (${workout.exercises.size} exercises, ${workout.estimatedDurationMinutes}min)")
            }
            sb.appendLine()
        }

        // Conversation history
        if (conversationHistory.isNotEmpty()) {
            sb.appendLine("=== Conversation ===")
            conversationHistory.forEach { msg ->
                when (msg) {
                    is Message.User -> sb.appendLine("User: ${msg.content}")
                    is Message.Assistant -> sb.appendLine("Coach: ${msg.content}")
                }
            }
        }

        // The latest user message is already in history, but we re-state it
        // as the explicit question for the model
        val lastUserMsg = conversationHistory.lastOrNull { it is Message.User }?.content ?: ""
        sb.appendLine()
        sb.appendLine("Respond to the user's latest message: \"$lastUserMsg\"")
        sb.appendLine("Coach:")

        return sb.toString()
    }

    private fun buildWorkoutSuggestionPrompt(
        user: UserProfile,
        recentWorkouts: List<WorkoutPlan>,
        energyScore: Int
    ): String = buildString {
        appendLine(SYSTEM_PROMPT)
        appendLine()
        appendLine("=== Context ===")
        appendLine("Goal: ${user.primaryGoal.name.lowercase().replace('_', ' ')}")
        appendLine("Experience: ${user.experienceLevel.name.lowercase()}")
        appendLine("Equipment: ${user.availableEquipment.joinToString { it.name.lowercase().replace('_', ' ') }}")
        appendLine("Energy score today: $energyScore/100")
        appendLine()
        if (recentWorkouts.isNotEmpty()) {
            appendLine("Recent workouts:")
            recentWorkouts.take(3).forEach { w ->
                appendLine("- ${w.name}")
            }
            appendLine()
        }
        appendLine("Based on the user's energy score, goal, and recent training, suggest")
        appendLine("what they should train today. Include exercise names, sets, reps, rest,")
        appendLine("and any notes about intensity adjustment based on their energy level.")
        appendLine("Coach:")
    }

    private fun generateFallbackWorkoutSuggestion(
        user: UserProfile,
        energyScore: Int
    ): String {
        val intensity = when {
            energyScore >= 80 -> "high"
            energyScore >= 50 -> "moderate"
            else -> "low"
        }
        return "Based on your energy score of $energyScore, I suggest a $intensity-intensity " +
                "session today. Focus on compound lifts at ${if (energyScore >= 50) "your normal" else "reduced"} " +
                "weight. Listen to your body and adjust as needed. 💪"
    }

    private fun trimHistory() {
        while (conversationHistory.size > MAX_HISTORY_SIZE) {
            conversationHistory.removeAt(0)
        }
    }

}

