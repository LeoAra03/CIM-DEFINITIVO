package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.CimTransportCodec
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpClient(private val host: String, private val port: Int, private val maxRetries: Int = 3) {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var heartbeatJob: Job? = null

    var onMessageReceived: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    fun connect() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            var attempts = 0
            var connected = false

            while (isRunning && attempts < maxRetries && !connected) {
                try {
                    PerformanceProfiler.trace("TCP_CONNECT") {
                        socket = TlsSocketHelper.createClientSocket(host, port, 2000)
                        socket?.soTimeout = 2000
                        // socket?.getOutputStream() puede ser null por interoperabilidad Java; asignamos solo si no es null
                        socket?.getOutputStream()?.let { out ->
                            writer = PrintWriter(out, true)
                        }
                        onConnectionStateChanged?.invoke(true)
                        connected = true
                        attempts = 0
                    }

                    // Start heartbeat
                    startHeartbeat()

                    val reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                    var inputLine: String?
                    while (isRunning && connected) {
                        try {
                            inputLine = reader.readLine()
                            if (inputLine == null) break
                            onMessageReceived?.invoke(CimTransportCodec.tryUnwrap(inputLine!!))
                        } catch (e: java.net.SocketTimeoutException) {
                            // continue loop to allow heartbeat and retries
                            continue
                        } catch (e: Exception) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    onConnectionStateChanged?.invoke(false)
                    attempts++
                    delay(2000)
                } finally {
                    stopHeartbeat()
                    socket?.close()
                }
            }

            if (!connected) {
                onConnectionStateChanged?.invoke(false)
            }
            isRunning = false
        }
    }

    private fun wrapOutgoing(message: String): String {
        return if (CimProtocol.USE_CRC_V2 && !message.startsWith(CimTransportCodec.PREFIX)) {
            CimTransportCodec.wrap(message)
        } else message
    }

    fun send(message: String) {
        scope.launch {
            try {
                writer?.println(wrapOutgoing(message))
                writer?.flush()
            } catch (e: Exception) {
                // log silently
            }
        }
    }

    fun isSocketConnected(): Boolean {
        val s = socket
        return s != null && s.isConnected && !s.isClosed
    }

    suspend fun sendSafe(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = socket
            val w = writer
            if (s != null && s.isConnected && !s.isClosed && w != null) {
                w.println(wrapOutgoing(message))
                w.flush()
                return@withContext !w.checkError()
            }
        } catch (e: Exception) {
            // log silently
        }
        return@withContext false
    }

    fun disconnect() {
        isRunning = false
        scope.launch {
            socket?.close()
            scope.cancel()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    val heartbeat = "HEARTBEAT|${System.currentTimeMillis()}"
                    writer?.println(heartbeat)
                } catch (_: Exception) { }
                delay(5000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
