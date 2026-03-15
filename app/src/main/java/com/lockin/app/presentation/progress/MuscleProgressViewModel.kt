package com.lockin.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.domain.model.MuscleGroup
import com.lockin.app.domain.usecase.MuscleProgressPoint
import com.lockin.app.domain.usecase.MuscleProgressUseCase
import com.lockin.app.domain.usecase.WeeklyVolumeRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MuscleProgressViewModel @Inject constructor(
    private val muscleProgressUseCase: MuscleProgressUseCase
) : ViewModel() {

    private val _selectedMuscle = MutableStateFlow(MuscleGroup.CHEST)
    val selectedMuscle: StateFlow<MuscleGroup> = _selectedMuscle.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val progressPoints: StateFlow<List<MuscleProgressPoint>> = _selectedMuscle
        .flatMapLatest { muscle -> muscleProgressUseCase.getProgressForMuscle(muscle) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyVolumeRows: StateFlow<List<WeeklyVolumeRow>> =
        muscleProgressUseCase.getWeeklyVolumeSummary()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMuscle(muscle: MuscleGroup) {
        _selectedMuscle.value = muscle
    }
}

