package com.industria.plc

import android.content.Context
import android.util.Log
import com.sistema.distribuido.network.CommandBroker
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CommandType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlcStationManager(private val context: Context) {
    private val TAG = "PlcStationManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var commandBroker: CommandBroker? = null

    fun setCommandBroker(broker: CommandBroker) {
        this.commandBroker = broker
    }

    fun sendDeliverCommand(fromStation: Int, toStation: Int) {
        scope.launch {
            try {
                val msg = CimMessage(
                    sourceApp = AppType.PLC,
                    destApp = AppType.COORDINADOR,
                    commandType = CommandType.EXECUTE,
                    payload = "DELIVER|fromStation=$fromStation,toStation=$toStation"
                )
                commandBroker?.sendCommand(msg)
                Log.d(TAG, "✓ DELIVER: $fromStation → $toStation")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error: ${e.message}")
            }
        }
    }

    fun close() {
        Log.d(TAG, "Cerrando PLC...")
    }
}
