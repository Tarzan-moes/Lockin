package com.lockin.app.presentation.permissions

import androidx.lifecycle.ViewModel
import com.lockin.app.data.external.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * HealthPermissionsViewModel — Exposes [HealthConnectManager] to the
 * Compose permission screen via Hilt injection.
 */
@HiltViewModel
class HealthPermissionsViewModel @Inject constructor(
    val healthConnectManager: HealthConnectManager
) : ViewModel()

