/**
 * PlcStationManager
 */
package com.industria.plc

import android.content.Context
import java.util.Date
import java.text.SimpleDateFormat
import kotlinx.coroutines.withTimeout
import android.util.Log
import com.industria.prefecto.CommandBroker
import com.industria.prefecto.CimMessage
import com.industria.prefecto.AppType
import com.industria.prefecto.CommandType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlcStationManager(private val context: Context) {
    private val tag = "PlcStationManager"
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
                Log.d(tag, "[OK] DELIVER: $fromStation -> $toStation")
            } catch (e: Exception) {
                Log.e(tag, "[ERR] Error: ${e.message}")
            }
        }
    }

    fun close() {
        Log.d(tag, "Cerrando PLC...")
    }
}

private val maxLogSize = 500

private fun addLogWithLimit(logs: MutableList<String>, message: String) {
    logs.add(message)
    while (logs.size > maxLogSize) {
        logs.removeAt(0)
    }
}
