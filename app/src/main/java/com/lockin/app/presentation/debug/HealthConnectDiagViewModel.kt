package com.lockin.app.presentation.debug

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.external.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HealthConnectDiagViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthConnectDiagState())
    val uiState: StateFlow<HealthConnectDiagState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val available = isHealthConnectAvailable()
            val granted = getGrantedPermissionsSnapshot()
            _uiState.value = HealthConnectDiagState(
                isAvailable = available,
                grantedPermissions = granted
            )
        }
    }

    suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getGrantedPermissionsSnapshot(): Set<HealthPermission> {
        return try {
            val granted = healthConnectManager.client.permissionController.getGrantedPermissions()
            granted.filterIsInstance<HealthPermission>().toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}

data class HealthConnectDiagState(
    val isAvailable: Boolean = false,
    val grantedPermissions: Set<HealthPermission> = emptySet()
)
