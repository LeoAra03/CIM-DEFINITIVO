package com.industria.plc

import android.util.Log
import com.industria.prefecto.GlobalBluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detector real de pallets usando Bluetooth + sensores ESP32
 * Replaces the previous simulation with real hardware data
 */
@Singleton
class RealPalletDetector @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _palletStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val palletStates: StateFlow<Map<Int, Boolean>> = _palletStates

    private val _lastEvent = MutableStateFlow("--")
    val lastEvent: StateFlow<String> = _lastEvent

    init {
        // BluetoothHardwareManager exposes messages as a Flow, not LiveData.
        // The subscription is optional so the app can run without BLE hardware.
        GlobalBluetoothManager.getInstanceOrNull()?.let { manager ->
            scope.launch {
                manager.incomingMessages.collect { data ->
                    if (data.contains("SENSOR_ACTIVATED")) {
                        parseSensorData(data)
                    }
                }
            }
        }
    }

    private fun parseSensorData(data: String) {
        // Formato esperado: SENSOR_ACTIVATED|POS:5
        val posMatch = Regex("POS:(\\d+)").find(data)
        val position = posMatch?.groupValues?.get(1)?.toIntOrNull() ?: return

        val currentStates = _palletStates.value.toMutableMap()
        currentStates[position] = true

        _palletStates.value = currentStates
        _lastEvent.value = "Pallet detectado en estación $position"

        Log.d("PalletDetector", "Pallet real detectado en POS:$position")
    }

    fun clearPallet(position: Int) {
        val currentStates = _palletStates.value.toMutableMap()
        currentStates[position] = false
        _palletStates.value = currentStates
        _lastEvent.value = "Estación $position liberada"
    }

    fun simulatePalletForTesting(position: Int) {
        // Test-only path without hardware
        val currentStates = _palletStates.value.toMutableMap()
        currentStates[position] = true
        _palletStates.value = currentStates
        _lastEvent.value = "SIMULADO: Pallet en POS:$position"
    }
}