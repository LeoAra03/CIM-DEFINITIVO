package com.sistema.distribuido.conveyor.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sistema.distribuido.network.AuthorizationManager
import com.sistema.distribuido.network.CommunicationCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConveyorViewModel @Inject constructor(
    private val commCoordinator: CommunicationCoordinator
) : ViewModel() {

    private val deviceMac = "AA:BB:CC:07:08:FF"
    private val deviceName = "CONVEYOR_ESP32"

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _conveyorSpeed = MutableStateFlow(0)
    val conveyorSpeed: StateFlow<Int> = _conveyorSpeed.asStateFlow()

    init {
        checkAuthorization()
    }

    private fun checkAuthorization() {
        val authorized = AuthorizationManager.isAuthorized(deviceMac)
        _isAuthorized.value = authorized
        _connectionStatus.value = if (authorized) "READY" else "PENDING_AUTH"
    }

    fun forward() {
        if (!_isAuthorized.value) return
        _conveyorSpeed.value = 50
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|FORWARD|50")
        }
    }

    fun reverse() {
        if (!_isAuthorized.value) return
        _conveyorSpeed.value = -50
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|REVERSE|50")
        }
    }

    fun stop() {
        _conveyorSpeed.value = 0
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|STOP")
        }
    }
}
