package com.sistema.distribuido.coordinador.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.AuthorizationManager
import com.sistema.distribuido.network.CommunicationCoordinator
import com.sistema.distribuido.network.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
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
class HubViewModel(
    private val permissionManager: PermissionManager,
    private val authorizationManager: AuthorizationManager,
    private val commCoordinator: CommunicationCoordinator
) : ViewModel() {

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
                updateDeviceStates(status)
            }
        }
    }

    private fun updateDeviceStates(status: Map<String, Any>) {
        // Actualizar lista de pendientes
        val pending = commCoordinator.getPendingDevices()
        val pendingList = pending.map { sessionState ->
            PendingDeviceState(
                mac = sessionState.key,
                name = "Device_${sessionState.key.takeLast(2)}",
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
        val authorizedList = authorized.map { (mac, _) ->
            AuthorizedDeviceState(
                mac = mac,
                name = "Device_${mac.takeLast(2)}",
                authorizedAt = System.currentTimeMillis()
            )
        }
        _authorizedDevices.value = authorizedList

        // Actualizar rechazados
        val rejected = commCoordinator.getRejectedDevices()
        _rejectedDevices.value = rejected.keys.toList()
    }

    // ====== ACCIONES DE AUTORIZACIÓN ======

    fun approveDevice(deviceMac: String, rememberDecision: Boolean) {
        Timber.tag("HubViewModel").i("Aprobando dispositivo: $deviceMac (recordar: $rememberDecision)")

        viewModelScope.launch {
            // 1. Registrar decisión en PermissionManager
            permissionManager.grantPermission(deviceMac, "ALL", "MANUAL_APPROVAL")

            // 2. Enviar AUTHORIZED al coordinador
            commCoordinator.sendAuthorizationResponse(deviceMac, authorized = true)

            // 3. Cerrar diálogo
            _currentAuthorizationDialog.value = null

            // 4. Recargar estado
            updateDeviceStates(commCoordinator.coordinationStatus.value)
        }
    }

    fun rejectDevice(deviceMac: String, rememberDecision: Boolean) {
        Timber.tag("HubViewModel").w("Rechazando dispositivo: $deviceMac (recordar: $rememberDecision)")

        viewModelScope.launch {
            // 1. Registrar decisión
            if (rememberDecision) {
                permissionManager.denyPermission(deviceMac)
            }

            // 2. Enviar REJECTED
            commCoordinator.sendAuthorizationResponse(deviceMac, authorized = false)

            // 3. Cerrar diálogo
            _currentAuthorizationDialog.value = null

            // 4. Recargar
            updateDeviceStates(commCoordinator.coordinationStatus.value)
        }
    }

    fun revokeDevice(deviceMac: String) {
        Timber.tag("HubViewModel").w("Revocando autorización de: $deviceMac")

        viewModelScope.launch {
            commCoordinator.revokeAuthorization(deviceMac)
            updateDeviceStates(commCoordinator.coordinationStatus.value)
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
