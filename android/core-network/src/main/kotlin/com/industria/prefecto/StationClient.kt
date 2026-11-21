package com.industria.prefecto

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import android.util.Log
import com.industria.prefecto.AppType
import com.industria.prefecto.CimMessage
import com.industria.prefecto.CimMessageBuilder
import com.industria.prefecto.CimProtocol
import com.industria.prefecto.CommandType

/**
 * Standardized industrial client for CIM stations.
 * Encapsula TcpClient y maneja el protocolo de Handshake y Status.
 * 
 * IMPORTANT: every send method validates the connection before sending
 * to avoid crashes in network operations.
 */
class StationClient(
    private val host: String,
    private val port: Int,
    private val stationName: String,
    private val password: String,
    private val stationUuid: String,
    private val macAddress: String = "00:00:00:00:00:00"
) {
    private val tcpClient = TcpClient(host, port)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectDelayBase = 2000L
    private var reconnectDelayMax = 30000L
    
    var onCommandReceived: ((String) -> Unit)? = null
    var onStatusChanged: ((Boolean) -> Unit)? = null
    var onAuthorizationStateChanged: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null
    private var authorizationState = CimProtocol.AUTH_STATE_DISCONNECTED

    val isAuthorized: Boolean
        get() = authorizationState == CimProtocol.AUTH_STATE_VALIDATED

    fun canSendCommand(): Boolean = isAuthorized

    private fun setAuthorizationState(newState: String) {
        authorizationState = newState
        onAuthorizationStateChanged?.invoke(newState)
    }

    init {
        tcpClient.onConnectionStateChanged = { connected ->
            onStatusChanged?.invoke(connected)
            if (connected) {
                // Cancel retries and start the handshake and heartbeat
                reconnectJob?.cancel()
                reconnectJob = null
                setAuthorizationState(CimProtocol.AUTH_STATE_PENDING)
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Conectado al coordinador", true))
                scope.launch { performHandshakeSafe() }
                startHeartbeat()
            } else {
                setAuthorizationState(CimProtocol.AUTH_STATE_DISCONNECTED)
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Desconectado - iniciando reintentos...", false))
                stopHeartbeat()
                scheduleReconnect()
            }
        }

        tcpClient.onMessageReceived = { msg ->
            try {
                val cim = CimMessage.fromTransportString(msg)
                if (cim != null) {
                    when (cim.commandType) {
                        CommandType.PERMISSION_GRANTED -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_VALIDATED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización exitosa recibida", true))
                        }
                        CommandType.PERMISSION_DENIED -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_REJECTED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización denegada por coordinador", false))
                        }
                        else -> {
                            if (msg.startsWith("COMMAND;")) {
                                val cmd = msg.removePrefix("COMMAND;")
                                onCommandReceived?.invoke(cmd)
                                onLog?.invoke(CimProtocol.formatLog("StationClient", "Comando recibido: $cmd", true))
                            } else {
                                onCommandReceived?.invoke(cim.payload)
                                onLog?.invoke(CimProtocol.formatLog("StationClient", "Payload recibido: ${cim.payload.take(80)}", true))
                            }
                        }
                    }
                } else {
                    when {
                        msg.startsWith(CimProtocol.RESPONSE_AUTHORIZED) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_VALIDATED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización exitosa recibida", true))
                        }
                        msg.startsWith(CimProtocol.RESPONSE_DENIED) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_REJECTED)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Autorización denegada por coordinador", false))
                        }
                        msg.startsWith(CimProtocol.RESPONSE_WAITING) -> {
                            setAuthorizationState(CimProtocol.AUTH_STATE_PENDING)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Esperando autorización del coordinador...", null))
                        }
                        msg.startsWith("COMMAND;") -> {
                            val cmd = msg.removePrefix("COMMAND;")
                            onCommandReceived?.invoke(cmd)
                            onLog?.invoke(CimProtocol.formatLog("StationClient", "Comando recibido: $cmd", true))
                        }
                        else -> onCommandReceived?.invoke(msg)
                    }
                }
            } catch (e: Exception) {
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Error procesando mensaje: ${e.message}", false))
                Log.e("StationClient", "Error procesando mensaje", e)
            }
        }
    }

    // Thread-safe anti-spam guard using a Mutex and an atomic reference.
    private val sendMutex = kotlinx.coroutines.sync.Mutex()
    private val lastSent = java.util.concurrent.atomic.AtomicReference<Pair<String, Long>>(Pair("", 0L))
    private var handshakeAttempts = 0

    @Volatile
    private var lastStatus: String = CimProtocol.STATUS_IDLE

    fun connect() {
        onLog?.invoke("Connecting to $host:$port [$stationName]")
        tcpClient.connect()
    }

    /**
     * Sends a message with anti-spam and sanitisation (non-blocking).
     */
    private fun sendSecure(msg: String) {
        scope.launch {
            if (!tcpClient.isSocketConnected()) {
                onLog?.invoke("No conectado - No se puede enviar: $msg")
                return@launch
            }
            if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
                onLog?.invoke("Comando bloqueado: estación no autorizada")
                return@launch
            }
            val now = System.currentTimeMillis()
            val (lastMsg, lastTime) = lastSent.get()
            if (msg == lastMsg && (now - lastTime) < 300) {
                onLog?.invoke("Duplicate message ignored (anti-spam)")
                return@launch
            }
            lastSent.set(Pair(msg, now))
            try {
                val cleanMsg = IndustrialErrorManager.sanitizeInput(msg)
                tcpClient.send(cleanMsg)
            } catch (e: Exception) {
                Log.w("StationClient", "sendSecure error: ${e.message}")
            }
        }
    }

    /**
     * Sends a message SAFELY and SYNCHRONOUSLY with full error handling
     * Thread-safe with a Mutex
     */
    suspend fun sendSafe(msg: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            sendMutex.withLock {
                if (!tcpClient.isSocketConnected()) {
                    onLog?.invoke("sendSafe: Socket NO conectado")
                    return@withContext false
                }
                if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
                    onLog?.invoke("sendSafe: comando bloqueado - estación no autorizada")
                    return@withContext false
                }
                val now = System.currentTimeMillis()
                val (lastMsg, lastTime) = lastSent.get()
                if (msg == lastMsg && (now - lastTime) < 300) {
                    return@withContext true
                }
                lastSent.set(Pair(msg, now))
                val cleanMsg = IndustrialErrorManager.sanitizeInput(msg)
                val success = tcpClient.sendSafe(cleanMsg)
                if (success) {
                    onLog?.invoke("Enviado: ${msg.take(80)}")
                } else {
                    onLog?.invoke("Fallo al enviar: ${msg.take(80)}")
                }
                success
            }
        } catch (e: Exception) {
            onLog?.invoke("Excepción en sendSafe: ${e.message}")
            Log.e("StationClient", "Error en sendSafe", e)
            false
        }
    }

    /**
     * Performs the permission handshake.
     */
    private suspend fun performHandshakeSafe() {
        handshakeAttempts = 0
        val handshake = CimMessageBuilder.createPermissionHandshake(
            sourceMac = macAddress,
            sourceApp = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN,
            stationName = stationName,
            password = password,
            stationUuid = stationUuid
        ).let { if (CimProtocol.USE_CRC_V2) it.toSecureTransportString() else it.toTransportString() }

        while (scope.isActive && handshakeAttempts < 5) {
            try {
                val success = sendSafe(handshake)
                if (success) {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Handshake completado", true))
                    return
                }
                handshakeAttempts++
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Handshake fallido, intento ${handshakeAttempts}", false))
                delay(1500L * handshakeAttempts)
            } catch (e: Exception) {
                handshakeAttempts++
                onLog?.invoke(CimProtocol.formatLog("StationClient", "Excepción en handshake (intento ${handshakeAttempts}): ${e.message}", false))
                delay(1500L * handshakeAttempts)
            }
        }
        if (handshakeAttempts >= 5) {
            onLog?.invoke(CimProtocol.formatLog("StationClient", "Handshake abortado tras 5 intentos", false))
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob != null && reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            var delayMs = reconnectDelayBase
            while (scope.isActive && !tcpClient.isSocketConnected()) {
                try {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Intentando reconectar en ${delayMs}ms...", false))
                    delay(delayMs)
                    tcpClient.connect()
                    delay(2500)
                    if (tcpClient.isSocketConnected()) {
                        onLog?.invoke(CimProtocol.formatLog("StationClient", "Reconexión exitosa", true))
                        break
                    }
                } catch (e: Exception) {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Error reconectando: ${e.message}", false))
                }
                delayMs = (delayMs * 2).coerceAtMost(reconnectDelayMax)
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (scope.isActive && tcpClient.isSocketConnected()) {
                try {
                    // Resend the last explicit status every ten seconds.
                    sendStatusSafe(lastStatus)
                } catch (e: Exception) {
                    onLog?.invoke(CimProtocol.formatLog("StationClient", "Heartbeat error: ${e.message}", false))
                }
                delay(10000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun sendStatus(status: String) {
        // Frame layout: STATUS;UUID;STATE
        lastStatus = status
        sendSecure("STATUS;$stationUuid;$status")
    }

    /**
     * Sends a status frame without blocking the caller.
     */
    suspend fun sendStatusSafe(status: String): Boolean {
        lastStatus = status
        return sendSafe("STATUS;$stationUuid;$status")
    }

    fun sendEvent(event: String) {
        // Formato: EVENT;UUID;DATA
        sendSecure("EVENT;$stationUuid;$event")
    }

    /**
     * Sends a workflow event without blocking the caller.
     */
    suspend fun sendEventSafe(event: String): Boolean {
        return sendSafe("EVENT;$stationUuid;$event")
    }

    fun disconnect() {
        onLog?.invoke("Desconectando...")
        // Stop reconnect/heartbeat and disconnect the client without cancelling the inner scope
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()
        tcpClient.disconnect()
    }
}
