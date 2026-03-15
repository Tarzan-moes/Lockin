package com.lockin.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.ai.coach.AiCoachManager
import com.lockin.app.domain.usecase.BuildWorkoutSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutReviewViewModel @Inject constructor(
    private val buildWorkoutSummaryUseCase: BuildWorkoutSummaryUseCase,
    private val aiCoachManager: AiCoachManager
) : ViewModel() {

    sealed class ReviewState {
        data object Idle : ReviewState()
        data object Loading : ReviewState()
        data class Success(val suggestions: String) : ReviewState()
        data class Error(val message: String) : ReviewState()
    }

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()

    fun reviewWorkout(workoutId: String, userGoal: String) {
        if (workoutId.isBlank()) {
            _reviewState.value = ReviewState.Error("Workout not found.")
            return
        }

        viewModelScope.launch(Dispatchers.Default.limitedParallelism(1)) {
            _reviewState.value = ReviewState.Loading
            try {
                val summary = buildWorkoutSummaryUseCase.build(workoutId)

                if (summary == null) {
                    _reviewState.value = ReviewState.Error("Workout not found.")
                    return@launch
                }

                if (summary.exercises.isEmpty() || summary.totalSets == 0) {
                    _reviewState.value = ReviewState.Error("Cannot analyze an empty workout. Log some sets first!")
                    return@launch
                }

                val promptString = buildWorkoutSummaryUseCase.buildPromptString(summary)
                val suggestions = aiCoachManager.reviewWorkoutAndSuggestImprovements(promptString, userGoal)
                _reviewState.value = ReviewState.Success(suggestions)
            } catch (e: Exception) {
                _reviewState.value = ReviewState.Error("AI Analysis failed: ${e.message}")
            }
        }
    }

    fun reset() {
        _reviewState.value = ReviewState.Idle
    }
}
