package com.sistema.distribuido.laser.viewmodels

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
class LaserViewModel @Inject constructor(
    private val commCoordinator: CommunicationCoordinator
) : ViewModel() {

    private val deviceMac = "AA:BB:CC:05:06:FF"
    private val deviceName = "LASER_ESP32"

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _laserPower = MutableStateFlow(0)
    val laserPower: StateFlow<Int> = _laserPower.asStateFlow()

    init {
        checkAuthorization()
    }

    private fun checkAuthorization() {
        val authorized = AuthorizationManager.isAuthorized(deviceMac)
        _isAuthorized.value = authorized
        _connectionStatus.value = if (authorized) "READY" else "PENDING_AUTH"
    }

    fun startEngraving() {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|ENGRAVE")
        }
    }

    fun setPower(power: Int) {
        if (!_isAuthorized.value) return
        _laserPower.value = power
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|POWER|$power")
        }
    }

    fun stop() {
        if (!_isAuthorized.value) return
        _laserPower.value = 0
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|STOP")
        }
    }
}
