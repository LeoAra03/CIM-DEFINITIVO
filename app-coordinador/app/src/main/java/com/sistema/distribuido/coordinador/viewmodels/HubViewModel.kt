package com.sistema.distribuido.coordinador.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.CommunicationCoordinator
import com.sistema.distribuido.network.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * HUB VIEW MODEL - COORDINADOR
 *
 * Responsabilidades:
 * 1. Orquestar el flujo central de autorización de dispositivos
 * 2. Mantener lista de dispositivos conectados (estados)
 * 3. Exponer estados de coordinación a la UI (AuthorizationDialog, DeviceList)
 * 4. Integración con CommunicationCoordinator y AuthorizationManager
 */
@HiltViewModel
class HubViewModel @Inject constructor(
    application: Application,
    private val commCoordinator: CommunicationCoordinator
) : AndroidViewModel(application) {
    private val permissionManager = PermissionManager(application.applicationContext)

    // ====== ESTADO PÚBLICO ======

    private val _pendingDevices = MutableStateFlow<List<PendingDeviceState>>(emptyList())
    val pendingDevices: StateFlow<List<PendingDeviceState>> = _pendingDevices.asStateFlow()

    private val _authorizedDevices = MutableStateFlow<List<AuthorizedDeviceState>>(emptyList())
    val authorizedDevices: StateFlow<List<AuthorizedDeviceState>> = _authorizedDevices.asStateFlow()

    private val _rejectedDevices = MutableStateFlow<List<String>>(emptyList())
    val rejectedDevices: StateFlow<List<String>> = _rejectedDevices.asStateFlow()

    private val _currentAuthorizationDialog = MutableStateFlow<AuthorizationDialogState?>(null)
    val currentAuthorizationDialog: StateFlow<AuthorizationDialogState?> = _currentAuthorizationDialog.asStateFlow()

    // ====== INICIALIZACIÓN ======

    init {
        Timber.tag("HubViewModel").d("Inicializando con coordinador")
        startMonitoringDevices()
    }

    // ====== MONITORING ======

    private fun startMonitoringDevices() {
        viewModelScope.launch {
            commCoordinator.coordinationStatus.collect { status ->
                Timber.tag("HubViewModel").d("Estado de coordinación actualizado: ${status.size} dispositivos")
                updateDeviceStates()
            }
        }
    }

    private fun updateDeviceStates() {
        // Actualizar lista de pendientes
        val pending = commCoordinator.getPendingDevices()
        val pendingList = pending.map { mac ->
            PendingDeviceState(
                mac = mac,
                name = "Device_${mac.takeLast(2)}",
                type = "STATION",
                timestamp = System.currentTimeMillis()
            )
        }
        _pendingDevices.value = pendingList

        // Mostrar el primer dispositivo en diálogo si hay pendientes
        if (pendingList.isNotEmpty() && _currentAuthorizationDialog.value == null) {
            showAuthorizationDialog(pendingList.first())
        }

        // Actualizar autorizados
        val authorized = commCoordinator.getAuthorizedDevices()
        val authorizedList = authorized.map { mac ->
            AuthorizedDeviceState(
                mac = mac,
                name = "Device_${mac.takeLast(2)}",
                authorizedAt = System.currentTimeMillis()
            )
        }
        _authorizedDevices.value = authorizedList

        // Actualizar rechazados
        val rejected = commCoordinator.getRejectedDevices()
        _rejectedDevices.value = rejected
    }

    // ====== ACCIONES DE AUTORIZACIÓN ======

    fun approveDevice(deviceMac: String, rememberDecision: Boolean) {
        Timber.tag("HubViewModel").i("Aprobando dispositivo: $deviceMac (recordar: $rememberDecision)")

        viewModelScope.launch {
            permissionManager.approve(deviceMac, rememberDecision)
            _currentAuthorizationDialog.value = null
            updateDeviceStates()
        }
    }

    fun rejectDevice(deviceMac: String, rememberDecision: Boolean) {
        Timber.tag("HubViewModel").w("Rechazando dispositivo: $deviceMac (recordar: $rememberDecision)")

        viewModelScope.launch {
            permissionManager.reject(deviceMac, rememberDecision)
            _currentAuthorizationDialog.value = null
            updateDeviceStates()
        }
    }

    fun revokeDevice(deviceMac: String) {
        Timber.tag("HubViewModel").w("Revocando autorización de: $deviceMac")

        viewModelScope.launch {
            commCoordinator.revokeAuthorization(deviceMac)
            updateDeviceStates()
        }
    }

    // ====== DIALOG MANAGEMENT ======

    private fun showAuthorizationDialog(device: PendingDeviceState) {
        _currentAuthorizationDialog.value = AuthorizationDialogState(
            mac = device.mac,
            name = device.name,
            type = device.type,
            receivedAt = device.timestamp,
            timeoutMs = 30000 // 30 segundos default
        )
    }

    fun dismissAuthorizationDialog() {
        _currentAuthorizationDialog.value = null
    }

    // ====== DATA CLASSES ======

    data class PendingDeviceState(
        val mac: String,
        val name: String,
        val type: String, // STATION, CONTROLLER, etc.
        val timestamp: Long
    )

    data class AuthorizedDeviceState(
        val mac: String,
        val name: String,
        val authorizedAt: Long
    )

    data class AuthorizationDialogState(
        val mac: String,
        val name: String,
        val type: String,
        val receivedAt: Long,
        val timeoutMs: Long = 30000
    )

    override fun onCleared() {
        super.onCleared()
        Timber.tag("HubViewModel").d("ViewModel limpiado")
    }
}
