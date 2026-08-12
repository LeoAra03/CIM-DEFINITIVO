/**
 * PlcStationManager
 * @author CIM Team
 */
// FIX #11: Additional null safety
package com.industria.plc

import android.content.Context
import java.util.Date
import java.text.SimpleDateFormat
import kotlinx.coroutines.withTimeout
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
                Log.d(TAG, "[OK] DELIVER: $fromStation -> $toStation")
            } catch (e: Exception) {
                Log.e(TAG, "[ERR] Error: ${e.message}")
            }
        }
    }

    fun close() {
        Log.d(TAG, "Cerrando PLC...")
    }
}

// FIX #82: Límite de logs para prevenir memory leak
private val MAX_LOG_SIZE = 500

private fun addLogWithLimit(logs: MutableList<String>, message: String) {
    logs.add(message)
    while (logs.size > MAX_LOG_SIZE) {
        logs.removeAt(0)
    }
}
