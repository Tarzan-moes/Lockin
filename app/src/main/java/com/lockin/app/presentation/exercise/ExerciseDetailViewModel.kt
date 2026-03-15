package com.lockin.app.presentation.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.local.models.ExerciseDetailEntity
import com.lockin.app.data.repository.WorkoutBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: WorkoutBuilderRepository
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.get<String>("exerciseId") ?: ""

    private val _exercise = MutableStateFlow<ExerciseDetailEntity?>(null)
    val exercise: StateFlow<ExerciseDetailEntity?> = _exercise.asStateFlow()

    init {
        viewModelScope.launch {
            _exercise.value = repo.getExerciseDetail(exerciseId)
        }
    }
}

