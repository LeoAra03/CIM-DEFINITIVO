package com.industria.prefecto

import android.util.Log

import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Manages Hyperterminal and binary command delivery to ESP32 devices.
 * Lays the groundwork for ESP-NOW bridging.
 */
class DeviceCommandManager(private val onLog: (String) -> Unit) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Mapping: Android command -> physical command (Hyperterminal)
     */
    private val commandMap = mapOf(
        "START_CONVEYOR" to "RUN_CINTA\n",
        "STOP_CONVEYOR"  to "STOP_CINTA\n",
        "ROBOT_HOME"     to "MOVE_HOME\n",
        "GET_SENSORS"    to "SCAN_NOW\n",
        "ACTIVATE_LASER" to "LSR_ON\n"
    )

    fun sendToEsp32(deviceIp: String, port: Int, androidCommand: String) {
        val physicalCommand = commandMap[androidCommand] ?: "$androidCommand\n"
        
        scope.launch {
            try {
                onLog("TX (Hardware) -> $deviceIp: $androidCommand")
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(deviceIp, port), 2000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(physicalCommand)
                    
                    // Update the record
                    val device = DeviceRegistry.dispositivosHardware[deviceIp]
                    device?.ultimoComando = androidCommand
                    device?.estado = "BUSY"
                }
            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                onLog("ERR (Hardware) -> $deviceIp: ${e.message}")
                DeviceRegistry.actualizarEstado(deviceIp, "ERROR")
            }
        }
    }

    /**
     * ESP-NOW base: command an ESP32 gateway must retransmit over radio.
     */
    fun broadcastEspNow(gatewayIp: String, targetMac: String, payload: String) {
        // Format expected by the ESP32 conect.py/servidor.py script
        val command = "ESPNOW;ID:$targetMac;DATA:$payload"
        sendToEsp32(gatewayIp, 80, command)
    }
}
