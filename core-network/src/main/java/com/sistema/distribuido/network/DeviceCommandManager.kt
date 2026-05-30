package com.sistema.distribuido.network

import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Gestiona el envío de comandos Hyperterminal y Binarios a los ESP32.
 * Prepara la base para el puenteo con ESP-NOW.
 */
class DeviceCommandManager(private val onLog: (String) -> Unit) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Mapeo: Comando Android -> Comando Físico (Hyperterminal)
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
                    
                    // Actualizar registro
                    val device = DeviceRegistry.dispositivosHardware[deviceIp]
                    device?.ultimoComando = androidCommand
                    device?.estado = "BUSY"
                }
            } catch (e: Exception) {
                onLog("ERR (Hardware) -> $deviceIp: ${e.message}")
                DeviceRegistry.actualizarEstado(deviceIp, "ERROR")
            }
        }
    }

    /**
     * Base para ESP-NOW: Comando que un ESP32 (Gateway) debe retransmitir por radio.
     */
    fun broadcastEspNow(gatewayIp: String, targetMac: String, payload: String) {
        // Formato esperado por el script conect.py/servidor.py del ESP32
        val command = "ESPNOW;ID:$targetMac;DATA:$payload"
        sendToEsp32(gatewayIp, 80, command)
    }
}
