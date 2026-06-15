package com.sistema.distribuido.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import com.sistema.distribuido.network.protocol.CimMessageBuilder

/**
 * Helper para forzar/verificar el handshake IDENTIFY <-> IDENTIFIED sobre BLE.
 */
object BleIdentificationHelper {

    suspend fun ensureIdentified(
        mac: String,
        bluetoothManager: BluetoothHardwareManager,
        broker: CommandBroker,
        timeoutMs: Long = 5000
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val result = CompletableDeferred<Boolean>()

            val listener: (com.sistema.distribuido.network.protocol.CimMessage) -> Unit = { msg ->
                try {
                    if (msg.commandType == com.sistema.distribuido.network.protocol.CommandType.IDENTIFIED && msg.sourceMac.equals(mac, ignoreCase = true)) {
                        if (!result.isCompleted) result.complete(true)
                    }
                } catch (_: Exception) { }
            }

            broker.addCommandReceivedListener(listener)

            try {
                val appId = AppIdentifier.getInstance()
                val identify = CimMessageBuilder.createIdentifyMessage(appId.deviceMac, appId.appType, appId.appVersion).toTransportString()
                
                bluetoothManager.sendToDevice(mac, identify)

                withTimeout(timeoutMs) {
                    result.await()
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                broker.removeCommandReceivedListener(listener)
            }
        }
    }
}
