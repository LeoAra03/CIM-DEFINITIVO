package com.sistema.distribuido.network

import android.util.Log
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * COMMAND BROKER CIM v6.0
 */

data class CommandTransaction(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: CimMessage,
    var response: CimMessage? = null,
    var status: TransactionStatus = TransactionStatus.PENDING,
    var latencyMs: Long = 0
)

enum class TransactionStatus {
    PENDING, SENT, RECEIVED, ACK, NACK, TIMEOUT, ERROR
}

class CommandBroker(
    private val bluetoothManager: BluetoothHardwareManager? = null,
    private val sppManager: BluetoothSppManager? = null,
    private val tcpServer: TcpServer? = null,
    private val tcpClient: TcpClient? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val maxLogSize: Int = 1000,
    private val allowOfflineSend: Boolean = false
) {

    companion object {
        const val TAG = "CommandBroker"
        const val COMMAND_TIMEOUT_MS: Long = 5000
    }

    private val transactions = ConcurrentHashMap<String, CommandTransaction>()
    private val transactionLog = java.util.concurrent.ConcurrentLinkedDeque<CommandTransaction>()

    private val onCommandSent: MutableList<(CimMessage) -> Unit> = mutableListOf()
    private val onCommandReceived: MutableList<(CimMessage) -> Unit> = mutableListOf()
    private val onCommandError: MutableList<(String) -> Unit> = mutableListOf()

    fun addCommandSentListener(callback: (CimMessage) -> Unit) { onCommandSent.add(callback) }
    fun addCommandReceivedListener(callback: (CimMessage) -> Unit) { onCommandReceived.add(callback) }
    fun removeCommandReceivedListener(callback: (CimMessage) -> Unit) { onCommandReceived.remove(callback) }
    fun addErrorListener(callback: (String) -> Unit) { onCommandError.add(callback) }

    fun disconnectBleDevice(mac: String) {
        bluetoothManager?.disconnect(mac)
    }

    fun reconnectBleDevice(mac: String, onConnectionChange: (Boolean) -> Unit = {}) {
        bluetoothManager?.reconnect(mac, onConnectionChange)
    }

    suspend fun sendCommand(message: CimMessage) {
        if (message.destMac.isBlank() && message.destApp == AppType.UNKNOWN) {
            notifyError("✗ BROKER: Destino inválido")
            return
        }

        val transaction = CommandTransaction(message = message)
        transactions[message.id] = transaction
        logTransaction(transaction)

        try {
            var sent = false
            var lastError: Exception? = null

            // Intentar BLE/Híbrido
            if (bluetoothManager != null) {
                try {
                    sendViaBle(message, transaction)
                    sent = true
                } catch (e: Exception) { lastError = e }
            }

            // Intentar TCP
            if (!sent && tcpClient != null && tcpClient.isSocketConnected()) {
                try {
                    val ok = tcpClient.sendSafe(message.toTransportString())
                    if (ok) {
                        transaction.status = TransactionStatus.SENT
                        sent = true
                        if (message.requiresAck()) waitForAckOrTimeout(transaction)
                    }
                } catch (e: Exception) { lastError = e }
            }

            // Intentar TCP Server
            if (!sent && tcpServer != null) {
                try {
                    val ok = tcpServer.sendToClientByMac(message.destMac, message.toTransportString())
                    if (ok) {
                        transaction.status = TransactionStatus.SENT
                        sent = true
                        if (message.requiresAck()) waitForAckOrTimeout(transaction)
                    }
                } catch (e: Exception) { lastError = e }
            }

            if (!sent && !allowOfflineSend) {
                transaction.status = TransactionStatus.ERROR
                notifyError("✗ NO ROUTE: ${message.destApp} - ${lastError?.message}")
            } else {
                onCommandSent.forEach { it(message) }
            }

        } catch (e: Exception) {
            transaction.status = TransactionStatus.ERROR
            notifyError("✗ BROKER ERROR: ${e.message}")
        }
    }

    suspend fun handleResponse(response: CimMessage) {
        var transaction = transactions[response.id]
        if (transaction == null && (response.commandType == CommandType.ACK || response.commandType == CommandType.NACK || response.commandType == CommandType.ERROR)) {
            val originalId = response.id.removeSuffix("_ACK").removeSuffix("_NACK").removeSuffix("_ERR")
            transaction = transactions[originalId]
        }

        if (transaction != null) {
            transaction.response = response
            transaction.status = when (response.commandType) {
                CommandType.ACK -> TransactionStatus.ACK
                CommandType.NACK -> TransactionStatus.NACK
                CommandType.ERROR -> TransactionStatus.ERROR
                else -> TransactionStatus.RECEIVED
            }
            transaction.latencyMs = System.currentTimeMillis() - transaction.timestamp
            logTransaction(transaction)
        }
        onCommandReceived.forEach { it(response) }
    }

    fun getStats(): BrokerStats {
        val logList = transactionLog.toList()
        return BrokerStats(
            totalTransactions = logList.size,
            avgLatencyMs = if (logList.isNotEmpty()) logList.map { it.latencyMs }.average() else 0.0,
            successRate = if (logList.isNotEmpty()) logList.count { it.status == TransactionStatus.ACK }.toDouble() / logList.size else 0.0,
            errorCount = logList.count { it.status == TransactionStatus.ERROR },
            logSize = logList.size
        )
    }

    private suspend fun sendViaBle(message: CimMessage, transaction: CommandTransaction) {
        bluetoothManager?.let {
            if (message.destMac.isNotEmpty()) it.sendToDevice(message.destMac, message.toTransportString())
            else it.send(message.toTransportString())
            
            transaction.status = TransactionStatus.SENT
            if (message.requiresAck()) waitForAckOrTimeout(transaction)
        }
    }

    private suspend fun waitForAckOrTimeout(transaction: CommandTransaction) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < COMMAND_TIMEOUT_MS) {
            val tx = transactions[transaction.message.id]
            if (tx != null && tx.status == TransactionStatus.ACK) return
            delay(100)
        }
        transaction.status = TransactionStatus.TIMEOUT
    }

    private fun logTransaction(transaction: CommandTransaction) {
        transactionLog.addLast(transaction.copy())
        while (transactionLog.size > maxLogSize) transactionLog.pollFirst()
    }

    private fun notifyError(msg: String) {
        onCommandError.forEach { it(msg) }
        Log.e(TAG, msg)
    }
}

data class BrokerStats(val totalTransactions: Int, val avgLatencyMs: Double, val successRate: Double, val errorCount: Int, val logSize: Int)
