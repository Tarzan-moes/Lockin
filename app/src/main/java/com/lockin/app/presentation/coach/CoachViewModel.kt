package com.lockin.app.presentation.coach

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lockin.app.ai.coach.AiCoachManager
import com.lockin.app.ai.inference.ModelManager
import com.lockin.app.domain.model.CoachMessage
import com.lockin.app.domain.repository.UserRepository
import com.lockin.app.domain.repository.WorkoutRepository
import java.util.UUID
import javax.inject.Inject

/**
 * CoachViewModel – Manages the AI Coach chat interface.
 *
 * Handles model initialization (lazy, on first screen visit),
 * sending/receiving messages, and maintaining conversation state.
 * All AI inference runs on background dispatchers via [AiCoachManager].
 */
@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachManager: AiCoachManager,
    private val modelManager: ModelManager,
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        initializeCoach()
    }

    /**
     * Initialize the AI model in the background.
     * Checks if the model file is present; if not, attempts to copy it
     * from the well-known download location before initializing.
     */
    private fun initializeCoach() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true) }
            try {
                // If model is not in app storage, try to copy from Downloads
                if (!modelManager.isModelAvailable()) {
                    val copied = tryAutoImportModel()
                    if (!copied) {
                        addAssistantMessage(
                            "📱 AI model not found. To enable full AI coaching, " +
                                    "push the model via ADB:\n\n" +
                                    "`adb push gemma-2b-it-gpu-int4.bin " +
                                    "/data/data/com.lockin.app/files/models/`\n\n" +
                                    "Using smart fallback responses in the meantime."
                        )
                    }
                }
                coachManager.initialize()
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        isModelReady = coachManager.isReady(),
                        modelSizeMB = modelManager.getModelSizeMB()
                    )
                }

                // Send a welcome message
                if (coachManager.isReady()) {
                    addAssistantMessage(
                        "👋 Hey! I'm your AI fitness coach. Ask me anything about " +
                                "workouts, form, recovery, or nutrition. I work completely " +
                                "offline on your device! 💪"
                    )
                } else {
                    addAssistantMessage(
                        "🤖 AI Coach is ready with basic responses. For full AI-powered " +
                                "coaching, ensure the model file is available in the app's assets."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        error = "Failed to initialize AI: ${e.message}"
                    )
                }
                addAssistantMessage(
                    "⚠️ I had trouble starting up, but I can still help with basic " +
                            "fitness advice. Try asking me a question!"
                )
            }
        }
    }

    /**
     * Update the text input field.
     */
    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onTabChanged(tab: CoachTab) {
        _uiState.update { it.copy(currentTab = tab) }
        // Maybe clear context or show a different welcome?
        // For now, keep history mixed or maybe clear on switch? 
        // Keeping mixed history is confusing if system prompt changes.
        // Prompt says "Keep conversation history (last 10 messages) the same way the workout coach does."
        // This could mean *separate* history or shared. Usually context switching implies shared history or distinct sessions.
        // I'll assume shared history for simplicity unless "Nutrition Coach Tab" implies a separate persona. 
        // AiCoachManager has ONE history list. So it's shared.
    }

    /**
     * Send the current input message to the AI coach.
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        // Add user message immediately
        addUserMessage(text)
        _uiState.update { it.copy(inputText = "", isthinking = true) }

        viewModelScope.launch {
            try {
                // Get user profile for context
                val user = userRepository.profile()
                // Get recent workouts
                val workouts = if (_uiState.value.currentTab == CoachTab.WORKOUT) {
                    workoutRepository.getAllWorkoutPlans() // Or recent sessions
                } else null

                val response = if (_uiState.value.currentTab == CoachTab.WORKOUT) {
                    coachManager.chat(text, user, workouts)
                } else {
                    coachManager.chatNutrition(text, user)
                }
                
                addAssistantMessage(response)
            } catch (e: Exception) {
                addAssistantMessage("⚠️ Something went wrong: ${e.message}. Please try again.")
            } finally {
                _uiState.update { it.copy(isthinking = false) }
            }
        }
    }

    /**
     * Request a daily motivation message.
     */
    fun requestMotivation() {
        _uiState.update { it.copy(isThinking = true) }
        viewModelScope.launch {
            try {
                val motivation = coachManager.getDailyMotivation()
                addAssistantMessage(motivation)
            } catch (e: Exception) {
                addAssistantMessage("🔥 Every rep brings you closer to your goal. Keep pushing!")
            } finally {
                _uiState.update { it.copy(isThinking = false) }
            }
        }
    }

    /**
     * Clear the conversation history.
     */
    fun clearConversation() {
        coachManager.clearConversation()
        _uiState.update { it.copy(messages = emptyList()) }
        addAssistantMessage("🔄 Conversation cleared. What would you like to talk about?")
    }

    override fun onCleared() {
        super.onCleared()
        // Don't release the model here – it's a singleton shared across the app.
        // The model will be released when the application is destroyed.
    }

    // ── Model import ─────────────────────────────────────────────────────

    /**
     * Try to auto-import the model from common locations on the device.
     * Returns true if model was found and copied successfully.
     */
    private suspend fun tryAutoImportModel(): Boolean {
        val searchPaths = listOf(
            "/storage/emulated/0/Download/gemma-2b-it-gpu-int4.bin",
            "/sdcard/Download/gemma-2b-it-gpu-int4.bin",
            "/storage/emulated/0/Documents/gemma-2b-it-gpu-int4.bin"
        )

        for (path in searchPaths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    addAssistantMessage("📦 Found model at ${file.name}. Copying to app storage…")
                    val copied = modelManager.copyModelFromPath(path) { progress ->
                        if (progress % 25 == 0) {
                            Log.d("CoachViewModel", "Model copy: $progress%")
                        }
                    }
                    if (copied) {
                        addAssistantMessage("✅ Model imported (${modelManager.getModelSizeMB()} MB)")
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.d("CoachViewModel", "Cannot access $path: ${e.message}")
            }
        }
        return false
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun addUserMessage(content: String) {
        val message = CoachMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    private fun addAssistantMessage(content: String) {
        val message = CoachMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }
}

data class CoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    val inputText: String = "",
    val isthinking: Boolean = false,
    val error: String? = null,
    val isInitializing: Boolean = false,
    val isModelReady: Boolean = false,
    val modelSizeMB: Float = 0f,
    val currentTab: CoachTab = CoachTab.WORKOUT
)

enum class CoachTab { WORKOUT, NUTRITION }
