package com.industria.prefecto

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import com.industria.prefecto.CimMessageBuilder

/**
 * Helper to force and verify the IDENTIFY <-> IDENTIFIED handshake over BLE.
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

            val listener: (com.industria.prefecto.CimMessage) -> Unit = { msg ->
                try {
                    if (msg.commandType == com.industria.prefecto.CommandType.IDENTIFIED && msg.sourceMac.equals(mac, ignoreCase = true)) {
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
