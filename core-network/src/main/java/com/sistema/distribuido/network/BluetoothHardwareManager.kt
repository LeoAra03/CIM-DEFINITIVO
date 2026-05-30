package com.sistema.distribuido.network

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.sistema.distribuido.network.protocol.CimMessageBuilder
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import com.sistema.distribuido.network.protocol.CimProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

enum class BluetoothType {
    BLE, CLASSIC, UNKNOWN
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val type: BluetoothType,
    val rssi: Int = 0,
    val device: BluetoothDevice
)

/**
 * BLUETOOTH HARDWARE MANAGER v6.0 (HÍBRIDO)
 */
class BluetoothHardwareManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onDataReceived: ((String, String) -> Unit)? = null
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val scanner = adapter?.bluetoothLeScanner
    
    private val connectedGatts = ConcurrentHashMap<String, BluetoothGatt>()
    private val connectedSockets = ConcurrentHashMap<String, ClassicConnectedThread>()

    private val _connectionStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val connectionStates: StateFlow<Map<String, Boolean>> = _connectionStates.asStateFlow()

    val discoveredDevicesMap: SnapshotStateMap<String, DiscoveredDevice> = mutableStateMapOf()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID_RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID_TX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val SPP_UUID = CimProtocol.SPP_UUID

    private val MAX_BLE_PACKET = 20
    private val receivingBuffers = ConcurrentHashMap<String, StringBuilder>()

    // 📡 Receiver para Bluetooth Clásico
    private val classicReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                
                device?.let {
                    val name = it.name ?: "Nodo_${it.address.takeLast(5)}"
                    // Filtrar por dispositivos industriales
                    if (name.contains("ESP32", ignoreCase = true) || name.contains("CIM", ignoreCase = true)) {
                        if (!discoveredDevicesMap.containsKey(it.address)) {
                            discoveredDevicesMap[it.address] = DiscoveredDevice(
                                name = name,
                                address = it.address,
                                type = BluetoothType.CLASSIC,
                                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt(),
                                device = it
                            )
                            onLog("✓ ENCONTRADO (Clásico): $name [${it.address}]")
                        }
                    }
                }
            }
        }
    }

    init {
        // Registrar el receptor para búsqueda de dispositivos clásicos
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(classicReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000) {
        if (adapter == null || !adapter.isEnabled) {
            onLog("ERROR: Bluetooth OFF")
            return
        }
        discoveredDevicesMap.clear()
        onLog("ESCANEANDO DISPOSITIVOS...")

        val bleCb = object : ScanCallback() {
            override fun onScanResult(ct: Int, res: ScanResult) {
                val device = res.device
                val name = device.name ?: "BLE_${device.address.takeLast(5)}"
                if (!discoveredDevicesMap.containsKey(device.address)) {
                    discoveredDevicesMap[device.address] = DiscoveredDevice(name, device.address, BluetoothType.BLE, res.rssi, device)
                }
            }
        }
        scanner?.startScan(bleCb)
        adapter.startDiscovery()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                scanner?.stopScan(bleCb)
                adapter.cancelDiscovery()
                onLog("✓ ESCANEO COMPLETADO")
            } catch (_: Exception) {}
        }, durationMs)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, onConnectionChange: (Boolean) -> Unit = {}) {
        val device = try { adapter.getRemoteDevice(address) } catch (e: Exception) { null }
        if (device == null) {
            onLog("✗ ERROR: MAC Inválida: $address")
            onConnectionChange(false)
            return
        }
        val type = if (device.type == BluetoothDevice.DEVICE_TYPE_LE) BluetoothType.BLE else BluetoothType.CLASSIC
        connectInternal(device, type, onConnectionChange)
    }

    private fun connectInternal(device: BluetoothDevice, type: BluetoothType, callback: (Boolean) -> Unit) {
        when (type) {
            BluetoothType.BLE -> connectBle(device, callback)
            BluetoothType.CLASSIC -> connectClassic(device, callback)
            else -> onLog("✗ ERROR: Tipo Desconocido")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBle(device: BluetoothDevice, callback: (Boolean) -> Unit) {
        val address = device.address
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onLog("✓ BLE CONECTADO: $address")
                    connectedGatts[address] = g
                    updateConnectionState(address, true)
                    g.discoverServices()
                    callback(true)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onLog("✗ BLE DESCONECTADO: $address")
                    connectedGatts.remove(address)
                    updateConnectionState(address, false)
                    g.close()
                    callback(false)
                }
            }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    setupBleNotifications(g)
                    scope.launch { delay(500); sendIdentification(address) }
                }
            }
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                processIncomingData(g.device.address, String(value, Charsets.UTF_8))
            }
        }
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun setupBleNotifications(gatt: BluetoothGatt) {
        val txChar = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID_TX) ?: return
        gatt.setCharacteristicNotification(txChar, true)
        txChar.getDescriptor(CCCD_UUID)?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectClassic(device: BluetoothDevice, callback: (Boolean) -> Unit) {
        val address = device.address
        scope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val thread = ClassicConnectedThread(socket)
                connectedSockets[address] = thread
                thread.start()
                onLog("✓ SPP CONECTADO: $address")
                updateConnectionState(address, true)
                callback(true)
                delay(500); sendIdentification(address)
            } catch (e: Exception) {
                onLog("✗ ERROR SPP: ${e.message}"); callback(false)
            }
        }
    }

    private fun updateConnectionState(address: String, connected: Boolean) {
        val current = _connectionStates.value.toMutableMap()
        current[address] = connected
        _connectionStates.value = current
    }

    private fun processIncomingData(mac: String, data: String) {
        val buffer = receivingBuffers.getOrPut(mac) { StringBuilder() }
        buffer.append(data)
        if (data.contains("\n") || data.contains("*")) {
            val fullMsg = buffer.toString().trim()
            buffer.clear()
            onDataReceived?.invoke(mac, fullMsg)
            handleIdentifyResponse(mac, fullMsg)
        }
    }

    private fun handleIdentifyResponse(mac: String, msg: String) {
        val cim = try { CimMessage.fromTransportString(msg) } catch (e: Exception) { null }
        if (cim != null && cim.commandType == CommandType.IDENTIFY) {
            scope.launch {
                val deviceInfo = DeviceInfo(ip = "", mac = mac, nombre = discoveredDevicesMap[mac]?.name ?: "Nodo_$mac", tipo = DeviceType.UNKNOWN, appType = cim.sourceApp, isConnected = true)
                GlobalDeviceRegistry.registry.register(mac, deviceInfo)
                val decision = GlobalPermissionManager.getInstance().requestPermission(mac, cim.sourceApp, deviceInfo.nombre)
                val responsePayload = if (decision == PermissionDecision.APPROVED) "AUTHORIZED" else "REJECTED"
                val response = CimMessageBuilder.createIdentifiedResponse(AppIdentifier.getInstance().deviceMac, mac, cim.sourceApp, responsePayload).toTransportString()
                sendToDevice(mac, response)
            }
        }
    }

    private suspend fun sendIdentification(mac: String) {
        val appId = AppIdentifier.getInstance()
        val identify = CimMessageBuilder.createIdentifyMessage(appId.deviceMac, appId.appType, appId.appVersion).toTransportString()
        sendToDevice(mac, identify)
    }

    fun sendToDevice(address: String, cmd: String) {
        connectedGatts[address]?.let { sendBle(it, cmd); return }
        connectedSockets[address]?.let { it.write((cmd + "\n").toByteArray()); return }
        onLog("✗ ERROR: No conexión para $address")
    }

    fun send(cmd: String, targetAddress: String? = null) {
        if (targetAddress != null) sendToDevice(targetAddress, cmd)
        else {
            connectedGatts.keys.forEach { sendToDevice(it, cmd) }
            connectedSockets.keys.forEach { sendToDevice(it, cmd) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendBle(gatt: BluetoothGatt, cmd: String) {
        val rxChar = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID_RX) ?: return
        val bytes = (cmd + "\n").toByteArray()
        if (bytes.size <= MAX_BLE_PACKET) {
            if (Build.VERSION.SDK_INT >= 33) gatt.writeCharacteristic(rxChar, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            else { rxChar.value = bytes; gatt.writeCharacteristic(rxChar) }
        } else {
            scope.launch {
                bytes.asIterable().chunked(MAX_BLE_PACKET).forEach { chunk ->
                    val arr = chunk.toByteArray()
                    if (Build.VERSION.SDK_INT >= 33) gatt.writeCharacteristic(rxChar, arr, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    else { rxChar.value = arr; gatt.writeCharacteristic(rxChar) }
                    delay(25)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect(address: String) {
        connectedGatts[address]?.let { it.disconnect(); it.close() }
        connectedGatts.remove(address)
        connectedSockets[address]?.cancel()
        connectedSockets.remove(address)
        updateConnectionState(address, false)
    }

    fun disconnectAll() {
        connectedGatts.keys.forEach { disconnect(it) }
        connectedSockets.keys.forEach { disconnect(it) }
    }

    fun isConnected(address: String): Boolean = _connectionStates.value[address] == true
    fun getConnectedDeviceAddresses(): Set<String> = connectedGatts.keys + connectedSockets.keys
    fun reconnect(mac: String, cb: (Boolean) -> Unit) { disconnect(mac); connect(mac, cb) }

    private inner class ClassicConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = socket.inputStream
        private val mmOutStream: OutputStream = socket.outputStream
        private val address = socket.remoteDevice.address
        override fun run() {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = mmInStream.read(buffer)
                    processIncomingData(address, String(buffer, 0, bytes))
                } catch (e: IOException) { disconnect(address); break }
            }
        }
        fun write(bytes: ByteArray) { try { mmOutStream.write(bytes) } catch (_: IOException) {} }
        fun cancel() { try { socket.close() } catch (_: IOException) {} }
    }
}
