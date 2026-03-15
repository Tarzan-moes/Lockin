package com.lockin.app.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.watch.connection.WatchConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutTrackingDebugViewModel @Inject constructor(
    private val watchConnectionManager: WatchConnectionManager
) : ViewModel() {

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _latestHeartRate = MutableStateFlow<Int?>(null)
    val latestHeartRate: StateFlow<Int?> = _latestHeartRate.asStateFlow()

    init {
        viewModelScope.launch {
            _isWatchConnected.value = runCatching { watchConnectionManager.isWatchConnected() }.getOrDefault(false)
        }
        viewModelScope.launch {
            watchConnectionManager.observeHeartRate()
                .catch { /* ignore stream errors in debug view */ }
                .collect { bpm -> _latestHeartRate.value = bpm }
        }
    }
}

