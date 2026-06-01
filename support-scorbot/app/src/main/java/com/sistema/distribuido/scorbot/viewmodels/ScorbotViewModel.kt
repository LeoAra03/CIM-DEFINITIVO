package com.sistema.distribuido.scorbot.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.AuthorizationManager
import com.sistema.distribuido.network.CommunicationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * SCORBOT VIEW MODEL
 *
 * Gestiona estado local de la estación Scorbot:
 * - Autorización (comunicación con coordinador)
 * - Control de brazo
 * - Monitoreo de estado
 */
@HiltViewModel
class ScorbotViewModel @Inject constructor(
    private val commCoordinator: CommunicationCoordinator
) : ViewModel() {

    private val deviceMac = "AA:BB:CC:01:02:FF" // Simulado, se obtendría del device
    private val deviceName = "SCORBOT_ESP32"

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _lastCommand = MutableStateFlow("")
    val lastCommand: StateFlow<String> = _lastCommand.asStateFlow()

    init {
        Timber.tag("ScorbotVM").d("Inicializando ViewModel para $deviceName [$deviceMac]")
        checkAuthorization()
    }

    private fun checkAuthorization() {
        val authorized = AuthorizationManager.isAuthorized(deviceMac)
        Timber.tag("ScorbotVM").d("Estado de autorización: $authorized")
        _isAuthorized.value = authorized
        _connectionStatus.value = if (authorized) "READY" else "PENDING_AUTH"
    }

    fun executeCommand(command: String) {
        if (!_isAuthorized.value) {
            Timber.tag("ScorbotVM").w("Intento de comando sin autorización: $command")
            return
        }

        Timber.tag("ScorbotVM").i("Ejecutando: $command")
        _lastCommand.value = command

        viewModelScope.launch {
            val routed = commCoordinator.routeCommand(deviceMac, "COMMAND|$command")
            if (!routed) {
                Timber.tag("ScorbotVM").w("Fallo al enrutar comando: $command")
                _connectionStatus.value = "FAILED"
            }
        }
    }

    fun refreshAuthorization() {
        checkAuthorization()
    }

    override fun onCleared() {
        super.onCleared()
        Timber.tag("ScorbotVM").d("ViewModel limpiado")
    }
}
