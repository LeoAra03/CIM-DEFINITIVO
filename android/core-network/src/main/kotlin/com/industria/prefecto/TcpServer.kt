package com.industria.prefecto

import java.io.*
import java.net.*
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import com.industria.prefecto.AuthorizationManager
import com.industria.prefecto.AppType
import com.industria.prefecto.CimMessage
import com.industria.prefecto.CimProtocol
import com.industria.prefecto.CimTransportCodec

class TcpServer(private val port: Int, private val collisionPolicy: CollisionPolicy = CollisionPolicy.PREFER_NEW) {
    private var serverSocket: ServerSocket? = null
    // connectionId -> Socket (connectionId = "ip:remotePort")
    private val clients = ConcurrentHashMap<String, Socket>()
    // connectionId -> ClientInfo
    private val clientInfos = ConcurrentHashMap<String, ClientInfo>()
    // MAC -> connectionId for O(1) lookup when sending by MAC
    private val macToConnId = ConcurrentHashMap<String, String>()
    // Optional concurrent connection cap - reduced to 50 to mitigate DoS
    private val maxClients = 50
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    // Rate limiting per IP: ip -> lastTime
    private val rateLimitMap = ConcurrentHashMap<String, Long>()
    private var lastBroadcastHash = ""
    private var lastBroadcastTime = 0L

    var onMessageReceived: ((String, String) -> Unit)? = null
    var onClientConnected: ((String) -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    data class ClientInfo(
        val ip: String,
        var mac: String = "",
        var appType: AppType = AppType.UNKNOWN,
        var lastSeen: Long = System.currentTimeMillis()
    )

    enum class CollisionPolicy {
        PREFER_NEW,
        REJECT_NEW
    }

    private fun handleMacMapping(mac: String, connId: String, socket: Socket): Boolean {
        val prev = macToConnId[mac]
        if (prev == null) {
            macToConnId[mac] = connId
            return true
        }
        if (prev == connId) return true

        return when (collisionPolicy) {
            CollisionPolicy.PREFER_NEW -> {
                // Close the previous connection and replace it with the new one
                clients[prev]?.let { try { it.close() } catch (_: Exception) {} }
                clients.remove(prev)
                clientInfos.remove(prev)
                macToConnId[mac] = connId
                true
            }
            CollisionPolicy.REJECT_NEW -> {
                // Reject the new connection
                try { socket.close() } catch (_: Exception) {}
                try { Log.w("TcpServer", "Rechazando nueva conexión $connId para MAC $mac debido a política REJECT_NEW") } catch (_: Exception) {}
                false
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                // Verify the port bound correctly
                if (serverSocket == null || !serverSocket!!.isBound) {
                    val errMsg = "ERROR: Puerto $port no se pudo vincular - puede estar en uso"
                    onError?.invoke(errMsg)
                    Log.e("TcpServer", errMsg)
                    isRunning = false
                    return@launch
                }
                Log.d("TcpServer", "[OK] TCP Server escuchando en puerto ${serverSocket!!.localPort}")

                // Broadcast clients only on change (5s debounce) with stale cleanup every 5s
                // Avoids 2s spam with O(n²) cost
                launch {
                    while (isRunning) {
                        val now = System.currentTimeMillis()
                        if (now - lastBroadcastTime > 5000) {
                            broadcastClientListIfChanged()
                        }
                        delay(1000)
                    }
                }

                launch {
                    while (isRunning) {
                        cleanupStaleConnections()
                        delay(5000)
                    }
                }

                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    val ip = socket.inetAddress.hostAddress ?: "unknown"
                    val connId = "$ip:${socket.port}"

                    // Rechazar si excede límite
                    if (clients.size >= maxClients) {
                        try {
                            Log.w("TcpServer", "Límite de conexiones alcanzado: $maxClients - rechazando $connId")
                            socket.close()
                        } catch (_: Exception) {}
                        continue
                    }

                    clients[connId] = socket
                    clientInfos[connId] = ClientInfo(ip = ip)
                    onClientConnected?.invoke(connId)
                    handleClient(socket, connId, ip)
                }
            } catch (e: Exception) {
                // Log unexpected errors to help debugging network crashouts
                try { Log.e("TcpServer", "Error en TcpServer.start: ${e.message}", e) } catch (_: Exception) {}
                isRunning = false
            }
        }
    }
    private fun handleClient(socket: Socket, connId: String, ip: String) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isRunning) {
                    val msg = reader.readLine() ?: break
                    // Update lastSeen
                    clientInfos[connId]?.lastSeen = System.currentTimeMillis()

                    // Identify the TCP client CIM handshake and update its MAC/AppType
                    try {
                        val cim = CimMessage.fromTransportString(msg)
                        if (cim != null) {
                            val oldMac = clientInfos[connId]?.mac
                            clientInfos[connId]?.mac = cim.sourceMac
                            clientInfos[connId]?.appType = cim.sourceApp
                            if (cim.sourceMac.isNotBlank()) {
                                // Handle MAC collisions per policy
                                val accepted = handleMacMapping(cim.sourceMac, connId, socket)
                                if (!accepted) {
                                    // if the new connection was not accepted, leave the loop and clean up
                                    break
                                }
                            }
                        } else if (msg.startsWith(CimProtocol.HANDSHAKE_VERSION)) {
                            val tokens = msg.split(";")
                            if (tokens.size >= 5) {
                                val stationName = tokens[1]
                                val mac = tokens[3]
                                val appType = AppType.values().firstOrNull { it.name.equals(stationName, ignoreCase = true) } ?: AppType.UNKNOWN
                                clientInfos[connId]?.mac = mac
                                clientInfos[connId]?.appType = appType
                                if (mac.isNotBlank()) {
                                    val accepted = handleMacMapping(mac, connId, socket)
                                    if (!accepted) {
                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignorar parse errors
                    }

                    onMessageReceived?.invoke(connId, msg)
                }
            } catch (e: Exception) {
                try { Log.e("TcpServer", "Error manejando cliente $connId: ${e.message}", e) } catch (_: Exception) {}
            } finally {
                disconnectClient(connId)
            }
        }
    }

    fun disconnectClient(connId: String) {
        clients[connId]?.let { try { it.close() } catch (_: Exception) {} }
        clients.remove(connId)
        clientInfos[connId]?.mac?.let { AuthorizationManager.revoke(it) }
        macToConnId.entries.removeIf { it.value == connId }
        clientInfos.remove(connId)
        onClientDisconnected?.invoke(connId)
    }

    fun broadcast(msg: String) {
        scope.launch {
            clients.values.forEach { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(wrapOutgoing(msg))
                } catch (e: Exception) {}
            }
        }
    }

    private fun wrapOutgoing(message: String): String {
        return if (CimProtocol.USE_CRC_V2 && !message.startsWith(CimTransportCodec.PREFIX)) {
            CimTransportCodec.wrap(message)
        } else message
    }

    private fun broadcastClientList() {
        val list = clientInfos.entries.map { (connId, info) -> "$connId:${info.appType.name}" }.joinToString(",")
        if (list.isNotEmpty()) broadcast("CLIENTS|$list")
    }

    private fun broadcastClientListIfChanged() {
        val list = clientInfos.entries.map { (connId, info) -> "$connId:${info.appType.name}" }.joinToString(",")
        if (list.isEmpty()) return
        val hash = list.hashCode().toString()
        if (hash != lastBroadcastHash) {
            lastBroadcastHash = hash
            lastBroadcastTime = System.currentTimeMillis()
            broadcast("CLIENTS|$list")
        }
    }

    private fun cleanupStaleConnections() {
        val now = System.currentTimeMillis()
        val inactive = clientInfos.filter { (_, info) -> (now - info.lastSeen) > 15000 }.keys
        if (inactive.isNotEmpty()) {
            inactive.forEach { ip ->
                onError?.invoke(CimProtocol.formatLog("TcpServer", "Conexión stale detectada y eliminada: $ip", false))
                disconnectClient(ip)
            }
        }
        macToConnId.entries.removeIf { (_, connId) -> !clients.containsKey(connId) }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        clients.values.forEach { it.close() }
        clients.clear()
    }

    /**
     * Sends a message to the client identified by MAC, writing to its socket.
     */
    fun sendToClientByMac(mac: String, msg: String): Boolean {
        // Try a direct MAC -> connectionId lookup
        var connId = macToConnId[mac]
        // Fallback: search clientInfos when the mapping does not exist
        if (connId == null) {
            connId = clientInfos.entries.firstOrNull { (_, info) -> info.mac == mac }?.key
        }

        if (connId != null) {
            clients[connId]?.let { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(wrapOutgoing(msg))
                    return true
                } catch (e: Exception) {
                    try { Log.e("TcpServer", "Error enviando a $connId: ${e.message}", e) } catch (_: Exception) {}
                    return false
                }
            }
        }

        return false
    }
}
