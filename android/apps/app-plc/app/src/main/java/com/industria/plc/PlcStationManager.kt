/**
 * PlcStationManager
 * @author CIM Team
 */
// FIX #11: Additional null safety
package com.industria.plc

import android.content.Context
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import android.util.Log
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import com.sistema.distribuido.network.CommandBroker
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import com.sistema.distribuido.network.protocol.CimMessage
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import com.sistema.distribuido.network.protocol.AppType
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import com.sistema.distribuido.network.protocol.CommandType
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Date

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

// FIX #82: Límite de logs para prevenir memory leak
private val MAX_LOG_SIZE = 500

private fun addLogWithLimit(logs: MutableList<String>, message: String) {
    logs.add(message)
    while (logs.size > MAX_LOG_SIZE) {
        logs.removeAt(0)
    }
}

// FIX #82: Límite de logs
private val MAX_LOG_SIZE = 500
