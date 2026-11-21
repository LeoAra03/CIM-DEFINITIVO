package com.industria.prefecto

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Outbound command broker for the CIM cell.
 *
 * Two responsibilities live side by side:
 * - the legacy transport multiplexer that routes [CimMessage] payloads over
 *   BLE, SPP, and TCP with transaction tracking;
 * - the per-station frame dispatcher: every frame for one station is
 *   serialised by a station lock, and a timeout releases the lock without
 *   waiting for the writer.
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

/** Writes one encoded frame to the underlying transport. */
fun interface FrameWriter {
    fun write(frame: ByteArray)
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
    private val stationLocks = ConcurrentHashMap<String, ReentrantLock>()

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
            notifyError("BROKER: invalid destination")
            return
        }

        val transaction = CommandTransaction(message = message)
        transactions[message.id] = transaction
        logTransaction(transaction)

        try {
            var sent = false
            var lastError: Exception? = null

            // Try BLE
            if (bluetoothManager != null) {
                try {
                    sendViaBle(message, transaction)
                    sent = true
                } catch (e: Exception) {
                    Log.e("CIM", "Error: ${e.message}", e)
                    lastError = e
                }
            }

            // Try SPP (Bluetooth Classic)
            if (!sent && sppManager != null) {
                try {
                    val payload = message.toTransportString()
                    if (message.destMac.isNotEmpty()) {
                        sppManager.sendToDevice(message.destMac, payload)
                    } else {
                        sppManager.sendToAll(payload)
                    }
                    transaction.status = TransactionStatus.SENT
                    sent = true
                    if (message.requiresAck()) waitForAckOrTimeout(transaction)
                } catch (e: Exception) {
                    Log.e("CIM", "Error: ${e.message}", e)
                    lastError = e
                }
            }

            // Try TCP
            if (!sent && tcpClient != null && tcpClient.isSocketConnected()) {
                try {
                    val ok = tcpClient.sendSafe(message.toTransportString())
                    if (ok) {
                        transaction.status = TransactionStatus.SENT
                        sent = true
                        if (message.requiresAck()) waitForAckOrTimeout(transaction)
                    }
                } catch (e: Exception) {
                    Log.e("CIM", "Error: ${e.message}", e)
                    lastError = e
                }
            }

            // Try the TCP server
            if (!sent && tcpServer != null) {
                try {
                    val ok = tcpServer.sendToClientByMac(message.destMac, message.toTransportString())
                    if (ok) {
                        transaction.status = TransactionStatus.SENT
                        sent = true
                        if (message.requiresAck()) waitForAckOrTimeout(transaction)
                    }
                } catch (e: Exception) {
                    Log.e("CIM", "Error: ${e.message}", e)
                    lastError = e
                }
            }

            if (!sent && !allowOfflineSend) {
                transaction.status = TransactionStatus.ERROR
                notifyError("NO ROUTE: ${message.destApp} - ${lastError?.message}")
            } else {
                onCommandSent.forEach { it(message) }
            }

        } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
            transaction.status = TransactionStatus.ERROR
            notifyError("BROKER ERROR: ${e.message}")
        }
    }

    /** Synchronous wrapper for offline tests (`allowOfflineSend = true`).
     * Uses Dispatchers.IO and a timeout to avoid ANRs on the UI thread.
     * Do not use in production code.
     */
    fun send(message: CimMessage) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            Log.w(TAG, "CommandBroker.send() called from the main thread; use sendCommand instead")
        }
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(COMMAND_TIMEOUT_MS + 1000) {
                sendCommand(message)
            } ?: run {
                Log.e(TAG, "Timeout in the synchronous send() wrapper for ${message.id}")
            }
        }
    }

    /** Synchronous send that enforces an explicit timeout for tests. */
    fun sendForTest(message: CimMessage, timeoutMs: Long = COMMAND_TIMEOUT_MS): Boolean {
        return try {
            runBlocking(Dispatchers.IO) {
                withTimeout(timeoutMs) { sendCommand(message); true }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendForTest fallo: ${e.message}")
            false
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

    /**
     * Encodes [frame] with [CimBinaryCodec] and writes it to [stationKey]
     * while holding that station's lock, so frames for one station never
     * interleave. A writer that does not return within [timeoutMs] releases
     * the lock; later frames for the station then fail fast instead of
     * queueing behind it.
     */
    fun dispatch(
        stationKey: String,
        frame: ByteArray,
        timeoutMs: Long = 1_000,
        writer: FrameWriter,
    ): Boolean {
        val lock = stationLocks.computeIfAbsent(stationKey) { ReentrantLock() }
        val locked = try {
            lock.tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!locked) return false
        return try {
            writer.write(CimBinaryCodec.encode(frame))
            true
        } finally {
            lock.unlock()
        }
    }
}

data class BrokerStats(
    val totalTransactions: Int,
    val avgLatencyMs: Double,
    val successRate: Double,
    val errorCount: Int,
    val logSize: Int,
)
