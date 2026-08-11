package com.sistema.distribuido.network

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import android.util.Log
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CimMessageBuilder
import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.CommandType

/**
 * Cliente Industrial Estandarizado para Estaciones CIM.
 * Encapsula TcpClient y maneja el protocolo de Handshake y Status.
 * 
 * IMPORTANTE: Todos los métodos de envío ahora validan conexión antes de enviar
 * para evitar crashouts en operaciones de red.
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
                // Cancelar reintentos y arrancar handshake + heartbeat
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

    // CORREGIDO: thread-safe anti-spam usando Mutex + Atomic
    private val sendMutex = kotlinx.coroutines.sync.Mutex()
    private val lastSent = java.util.concurrent.atomic.AtomicReference<Pair<String, Long>>(Pair("", 0L))
    private var handshakeAttempts = 0

    fun connect() {
        onLog?.invoke("→ Iniciando conexión a $host:$port [${stationName}]")
        // Validar token strength
        if (CimProtocol.isDefaultTokenInUse()) {
            onLog?.invoke("⚠ Token default en uso - cambiar en Coordinador para producción")
        }
        tcpClient.connect()
    }

    /**
     * Envía un mensaje con anti-spam y sanitización (no-bloqueante) - CORREGIDO thread-safe
     */
    private fun sendSecure(msg: String) {
        scope.launch {
            if (!tcpClient.isSocketConnected()) {
                onLog?.invoke("✗ No conectado - No se puede enviar: $msg")
                return@launch
            }
            if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
                onLog?.invoke("✗ Comando bloqueado: estación no autorizada")
                return@launch
            }
            val now = System.currentTimeMillis()
            val (lastMsg, lastTime) = lastSent.get()
            if (msg == lastMsg && (now - lastTime) < 300) {
                onLog?.invoke("⟳ Mensaje duplicado ignorado (anti-spam)")
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
     * Envía mensaje de forma SEGURA y SÍNCRONA con manejo de error completo
     * CORREGIDO: thread-safe con Mutex
     */
    suspend fun sendSafe(msg: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            sendMutex.withLock {
                if (!tcpClient.isSocketConnected()) {
                    onLog?.invoke("✗ sendSafe: Socket NO conectado")
                    return@withContext false
                }
                if ((msg.startsWith("COMMAND;") || msg.startsWith("CMD;")) && !isAuthorized) {
                    onLog?.invoke("✗ sendSafe: comando bloqueado - estación no autorizada")
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
                    onLog?.invoke("✓ Enviado: ${msg.take(80)}")
                } else {
                    onLog?.invoke("✗ Fallo al enviar: ${msg.take(80)}")
                }
                success
            }
        } catch (e: Exception) {
            onLog?.invoke("✗ Excepción en sendSafe: ${e.message}")
            Log.e("StationClient", "Error en sendSafe", e)
            false
        }
    }

    /**
     * Realiza handshake de forma segura
     */
    private suspend fun performHandshakeSafe() {
        handshakeAttempts = 0
        val handshake = CimMessageBuilder.createPermissionHandshake(
            sourceMac = macAddress,
            sourceApp = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN,
            stationName = stationName,
            password = CimProtocol.pairingSecretForTransport(password),
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
                    // Enviar heartbeat o estado cada 10s
                    val status = if (isAuthorized) CimProtocol.READY else CimProtocol.IDLE
                    sendStatusSafe(status)
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
        // Formato: STATUS;UUID;ESTADO
        sendSecure("STATUS;$stationUuid;$status")
    }

    /**
     * Envía status de forma segura
     */
    suspend fun sendStatusSafe(status: String): Boolean {
        return sendSafe("STATUS;$stationUuid;$status")
    }

    fun sendEvent(event: String) {
        // Formato: EVENT;UUID;DATA
        sendSecure("EVENT;$stationUuid;$event")
    }

    /**
     * Envía evento de forma segura
     */
    suspend fun sendEventSafe(event: String): Boolean {
        return sendSafe("EVENT;$stationUuid;$event")
    }

    fun disconnect() {
        onLog?.invoke("→ Desconectando...")
        // Detener reconexión/heartbeat y desconectar el cliente, pero no cancelar scope interno
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()
        tcpClient.disconnect()
    }
}
