package com.sistema.distribuido.vision.viewmodels

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
class VisionViewModel @Inject constructor(
    private val commCoordinator: CommunicationCoordinator
) : ViewModel() {

    private val deviceMac = "AA:BB:CC:03:04:FF"
    private val deviceName = "VISION_ESP32"

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _connectionStatus = MutableStateFlow("CONNECTING")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    init {
        checkAuthorization()
    }

    private fun checkAuthorization() {
        val authorized = AuthorizationManager.isAuthorized(deviceMac)
        _isAuthorized.value = authorized
        _connectionStatus.value = if (authorized) "READY" else "PENDING_AUTH"
    }

    fun captureImage() {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|CAPTURE_IMAGE")
        }
    }

    fun detectAruco() {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|DETECT_ARUCO")
        }
    }

    fun checkQuality() {
        if (!_isAuthorized.value) return
        viewModelScope.launch {
            commCoordinator.routeCommand(deviceMac, "COMMAND|CHECK_QUALITY")
        }
    }
}
