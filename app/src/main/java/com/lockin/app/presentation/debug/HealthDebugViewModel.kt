package com.lockin.app.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.external.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HealthDebugViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthDebugUiState())
    val uiState: StateFlow<HealthDebugUiState> = _uiState

    fun testStepsToday() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "")
            val hasPerm = runCatching { healthConnectManager.hasAllPermissions() }.getOrDefault(false)
            if (!hasPerm) {
                _uiState.value = HealthDebugUiState(
                    isLoading = false,
                    permissionNeeded = true,
                    message = "Permissions required. Please grant Health Connect access."
                )
                return@launch
            }

            val steps = runCatching {
                healthConnectManager.getStepsForDate(LocalDate.now())
            }.onFailure {
                _uiState.value = HealthDebugUiState(
                    isLoading = false,
                    permissionNeeded = false,
                    message = "Read failed: ${it.message ?: "unknown error"}",
                    steps = null
                )
            }.getOrNull()

            if (steps != null) {
                _uiState.value = HealthDebugUiState(
                    isLoading = false,
                    permissionNeeded = false,
                    message = "Steps today: $steps",
                    steps = steps
                )
            }
        }
    }
}

data class HealthDebugUiState(
    val isLoading: Boolean = false,
    val permissionNeeded: Boolean = false,
    val message: String = "",
    val steps: Long? = null
)

