// FIX #11: Additional null safety
package com.industria.plc

import android.util.Log
import com.sistema.distribuido.network.GlobalBluetoothManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detector real de pallets usando Bluetooth + sensores ESP32
 * Reemplaza la simulación anterior con datos reales del hardware
 */
@Singleton
class RealPalletDetector @Inject constructor(
    private val bluetoothManager: GlobalBluetoothManager
) {
    private val _palletStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val palletStates: StateFlow<Map<Int, Boolean>> = _palletStates

    private val _lastEvent = MutableStateFlow("--")
    val lastEvent: StateFlow<String> = _lastEvent

    init {
        // Escuchar datos BLE del ESP32 del PLC
        bluetoothManager.receivedData.observeForever { data ->
            if (data != null && data.contains("SENSOR_ACTIVATED")) {
                parseSensorData(data)
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
        // Solo para pruebas sin hardware
        val currentStates = _palletStates.value.toMutableMap()
        currentStates[position] = true
        _palletStates.value = currentStates
        _lastEvent.value = "SIMULADO: Pallet en POS:$position"
    }
}