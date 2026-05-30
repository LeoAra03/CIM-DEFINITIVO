package com.sistema.distribuido.network

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.sistema.distribuido.network.protocol.CimProtocol
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager de Bluetooth SPP (Serial Port Profile) Robusto.
 * Maneja múltiples conexiones, modo servidor y cliente.
 * Diseñado para evitar crashes y ConcurrentModificationException.
 */
class BluetoothSppManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onDataReceived: (String, String) -> Unit // MAC, Data
) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        manager.adapter
    }

    private val activeConnections = ConcurrentHashMap<String, ConnectedThread>()
    private var acceptThread: AcceptThread? = null
    private var isServerRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun isEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) { false }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServer() {
        if (isServerRunning) return
        if (bluetoothAdapter == null) { onLog("Bluetooth no soportado"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            onLog("⚠ Permiso BLUETOOTH_CONNECT no concedido, no se puede iniciar servidor SPP")
            return
        }

        isServerRunning = true
        acceptThread = AcceptThread()
        acceptThread?.start()
        onLog("Servidor BT SPP Activo")
    }

    fun stopServer() {
        isServerRunning = false
        acceptThread?.cancel()
        acceptThread = null
        onLog("Servidor BT SPP Detenido")
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice) {
        val mac = device.address
        if (activeConnections.containsKey(mac)) { onLog("Ya conectado a $mac"); return }

        scope.launch {
            var socket: BluetoothSocket? = null
            try {
                onLog("Intentando conectar a ${device.name ?: mac}...")
                socket = device.createRfcommSocketToServiceRecord(CimProtocol.SPP_UUID)
                socket.connect()
                manageConnectedSocket(socket)
            } catch (e: IOException) {
                // Fallback Inseguro (Anti-crash #3)
                try {
                    onLog("Reintentando conexión insegura...")
                    socket = device.createInsecureRfcommSocketToServiceRecord(CimProtocol.SPP_UUID)
                    socket.connect()
                    manageConnectedSocket(socket)
                } catch (e2: IOException) {
                    onLog("Error final de conexión: ${e2.message}")
                    socket?.close()
                }
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        val mac = socket.remoteDevice.address
        val thread = ConnectedThread(socket)
        activeConnections[mac] = thread
        thread.start()
        onLog("Vínculo establecido con $mac")
        // Enviar IDENTIFY inicial para que la otra parte responda con IDENTIFIED
        try {
            val appId = AppIdentifier.getInstance()
            val identify = com.sistema.distribuido.network.protocol.CimMessageBuilder.createIdentifyMessage(appId.deviceMac, appId.appType, appId.appVersion).toTransportString()
            // Pequeño delay y envío asíncrono
            scope.launch {
                try {
                    sendToDevice(mac, identify)
                    onLog("→ SENT IDENTIFY via SPP to $mac")
                } catch (e: Exception) {
                    onLog("⚠ Error enviando IDENTIFY SPP a $mac: ${e.message}")
                }
            }
        } catch (_: Exception) { }
    }

    fun sendToAll(message: String) {
        val data = (message + "\n").toByteArray()
        activeConnections.values.forEach { it.write(data) }
    }

    fun sendToDevice(mac: String, message: String) {
        val data = (message + "\n").toByteArray()
        activeConnections[mac]?.write(data)
    }

    fun disconnectAll() {
        activeConnections.values.forEach { it.cancel() }
        activeConnections.clear()
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        override fun run() {
            try {
                // Verificar permisos en Android S+
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            onLog("⚠ Permiso BLUETOOTH_CONNECT no concedido, no se puede iniciar servidor SPP")
                            isServerRunning = false
                            return
                        }
                    }
                } catch (_: Exception) {}

                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("CIM_HUB", CimProtocol.SPP_UUID)
            } catch (e: IOException) {
                onLog("Error al crear ServerSocket: ${e.message}")
                return
            }

            while (isServerRunning) {
                val socket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    if (isServerRunning) onLog("Servidor cerrado: ${e.message}")
                    break
                }
                socket?.let { manageConnectedSocket(it) }
            }
            isServerRunning = false
        }

        fun cancel() {
            try { serverSocket?.close() } catch (e: IOException) { }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?
        private val mac: String = socket.remoteDevice.address

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) { onLog("Error al obtener streams de $mac") }
            inputStream = tmpIn
            outputStream = tmpOut
        }

        override fun run() {
            // Anti-crash #2: Verificar streams nulos
            if (inputStream == null || outputStream == null) {
                onLog("Error: Streams nulos para $mac")
                closeConnection()
                return
            }

            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val received = String(buffer, 0, bytes).trim()
                    if (received.isNotEmpty()) {
                        mainHandler.post { onDataReceived(mac, received) }
                    }
                } catch (e: IOException) {
                    onLog("Conexión perdida con $mac")
                    closeConnection()
                    break
                }
            }
        }

        fun write(data: ByteArray) {
            try {
                outputStream?.write(data)
            } catch (e: IOException) { onLog("Error al enviar datos a $mac") }
        }

        fun cancel() {
            closeConnection()
        }

        private fun closeConnection() {
            try {
                activeConnections.remove(mac)
                socket.close()
            } catch (e: IOException) { }
        }
    }
}
