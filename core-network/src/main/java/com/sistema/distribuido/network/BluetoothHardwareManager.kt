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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.sistema.distribuido.network.protocol.CimMessageBuilder
import com.sistema.distribuido.network.protocol.CimMessage
import com.sistema.distribuido.network.protocol.CommandType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * BLUETOOTH HARDWARE MANAGER v6.0
 *
 * Escaneo híbrido BLE + Classic, multiconexión GATT y fragmentación MTU.
 */
class BluetoothHardwareManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onDataReceived: ((String, String) -> Unit)? = null
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val scanner = adapter?.bluetoothLeScanner
    private val mainHandler = Handler(Looper.getMainLooper())

    private val connectedDevices = ConcurrentHashMap<String, BluetoothGatt>()
    private val gattCallbacks = ConcurrentHashMap<String, BluetoothGattCallback>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val reconnectAttempts = ConcurrentHashMap<String, Int>()
    private val reconnectJobs = ConcurrentHashMap<String, Job>()

    private val _connectionStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val connectionStates: StateFlow<Map<String, Boolean>> = _connectionStates.asStateFlow()

    val discoveredDevicesMap: SnapshotStateMap<String, DiscoveredBluetoothDevice> = mutableStateMapOf()
    val discoveredHardware: SnapshotStateMap<String, String> = mutableStateMapOf()

    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID_RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID_TX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val MAX_BLE_PACKET = 20
    private val receivingBuffers = ConcurrentHashMap<String, StringBuilder>()
    private var activeBleScanCallback: ScanCallback? = null
    private var discoveryReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000) {
        if (adapter == null || !adapter.isEnabled) {
            onLog("ERROR: Bluetooth OFF")
            return
        }

        discoveredDevicesMap.clear()
        discoveredHardware.clear()
        onLog("ESCANEANDO MALLA BLE + CLASSIC...")

        startClassicDiscovery()

        val cb = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(ct: Int, res: ScanResult) {
                registerDiscoveredDevice(res.device.address, res.device.name, res.isConnectable)
            }
        }
        activeBleScanCallback = cb
        scanner?.startScan(cb)

        mainHandler.postDelayed({
            stopScanInternal()
            onLog("✓ ESCANEO COMPLETADO (${discoveredDevicesMap.size} dispositivos)")
        }, durationMs)
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        if (adapter == null) return
        try {
            if (discoveryReceiver == null) {
                discoveryReceiver = object : BroadcastReceiver() {
                    @SuppressLint("MissingPermission")
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (BluetoothDevice.ACTION_FOUND == intent.action) {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            device?.let { registerDiscoveredDevice(it.address, it.name, true) }
                        }
                    }
                }
            }
            if (!isReceiverRegistered) {
                context.registerReceiver(
                    discoveryReceiver,
                    IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
                isReceiverRegistered = true
            }
            adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: Exception) {
            onLog("⚠ Classic discovery: ${e.message}")
        }
    }

    private fun registerDiscoveredDevice(address: String, rawName: String?, connectable: Boolean) {
        val name = rawName?.takeIf { it.isNotBlank() } ?: "Nodo_${address.takeLast(5)}"
        val isCimDevice = name.contains("ESP32", ignoreCase = true) ||
            name.contains("CIM", ignoreCase = true) ||
            name.contains("NODO", ignoreCase = true) ||
            connectable

        if (!isCimDevice) return
        if (discoveredDevicesMap.containsKey(address)) return

        val device = DiscoveredBluetoothDevice(address, name)
        discoveredDevicesMap[address] = device
        discoveredHardware[address] = name
        onLog("✓ ENCONTRADO: $name [$address]")
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, onConnectionChange: (Boolean) -> Unit = {}) {
        if (adapter == null) return

        if (connectedDevices.containsKey(address)) {
            onConnectionChange(true)
            return
        }

        cancelReconnect(address)
        onLog("→ CONECTANDO A $address...")
        val device = adapter.getRemoteDevice(address)
        val callback = createGattCallback(address, onConnectionChange)
        gattCallbacks[address] = callback

        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, callback)
        }
        connectedDevices[address] = gatt
    }

    @SuppressLint("MissingPermission")
    fun reconnect(address: String, onConnectionChange: (Boolean) -> Unit = {}) {
        disconnect(address)
        val attempt = reconnectAttempts.getOrDefault(address, 0) + 1
        reconnectAttempts[address] = attempt
        val delayMs = min(1000L * (1 shl (attempt - 1)), 30_000L)
        onLog("↻ Reconexión $address en ${delayMs}ms (intento $attempt)")

        reconnectJobs[address]?.cancel()
        reconnectJobs[address] = scope.launch {
            delay(delayMs)
            connect(address, onConnectionChange)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect(address: String? = null) {
        if (address == null) {
            reconnectJobs.values.forEach { it.cancel() }
            reconnectJobs.clear()
            reconnectAttempts.clear()
            connectedDevices.keys.toList().forEach { disconnect(it) }
            updateConnectionStates()
            return
        }

        cancelReconnect(address)
        reconnectAttempts.remove(address)
        gattCallbacks.remove(address)

        connectedDevices.remove(address)?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (_: Exception) {
            }
        }
        receivingBuffers.remove(address)
        setConnectionState(address, false)
        onLog("✗ DESCONECTADO: $address")
    }

    private fun cancelReconnect(address: String) {
        reconnectJobs.remove(address)?.cancel()
    }

    private fun setConnectionState(address: String, connected: Boolean) {
        val updated = _connectionStates.value.toMutableMap()
        if (connected) {
            updated[address] = true
            reconnectAttempts.remove(address)
        } else {
            updated.remove(address)
        }
        _connectionStates.value = updated
    }

    private fun updateConnectionStates() {
        _connectionStates.value = connectedDevices.keys.associateWith { true }
    }

    private fun createGattCallback(address: String, onConnectionChange: (Boolean) -> Unit): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onLog("✓ CONECTADO: $address")
                        connectedDevices[address] = g
                        setConnectionState(address, true)
                        g.discoverServices()
                        mainHandler.post { onConnectionChange(true) }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedDevices.remove(address)
                        setConnectionState(address, false)
                        try {
                            g.close()
                        } catch (_: Exception) {
                        }
                        mainHandler.post { onConnectionChange(false) }
                        scheduleAutoReconnect(address)
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                onLog("✓ SERVICIOS DESCUBIERTOS [$address]")
                try {
                    val txChar = g.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID_TX) ?: return
                    g.setCharacteristicNotification(txChar, true)
                    val desc = txChar.getDescriptor(CCCD_UUID) ?: return
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (Build.VERSION.SDK_INT >= 33) {
                        g.writeDescriptor(desc)
                    } else {
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(desc)
                    }
                    scope.launch {
                        delay(500)
                        sendIdentification(address)
                    }
                } catch (e: Exception) {
                    onLog("⚠ Error setup GATT [$address]: ${e.message}")
                }
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleIncomingData(gatt, String(value, Charsets.UTF_8).trim())
            }

            @Deprecated("Deprecated in API 33")
            @SuppressLint("MissingPermission")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                handleIncomingData(gatt, String(value, Charsets.UTF_8).trim())
            }
        }
    }

    private fun scheduleAutoReconnect(address: String) {
        if (reconnectJobs.containsKey(address)) return
        reconnectJobs[address] = scope.launch {
            reconnect(address)
        }
    }

    private fun handleIncomingData(gatt: BluetoothGatt, data: String) {
        val mac = gatt.device.address
        val deviceName = gatt.device.name ?: "UNNAMED"
        onLog("← RX [$mac] from [$deviceName]: ${data.take(60)}")

        val buffer = receivingBuffers.getOrPut(mac) { StringBuilder() }
        buffer.append(data)

        if (data.endsWith("*") || data.endsWith("\n")) {
            val fullMessage = buffer.toString().trim()
            buffer.clear()
            onDataReceived?.invoke(mac, fullMessage)
            handleIdentifyHandshake(gatt, mac, fullMessage)
        }
    }

    private fun handleIdentifyHandshake(gatt: BluetoothGatt, mac: String, fullMessage: String) {
        try {
            val cim = CimMessage.fromTransportString(fullMessage) ?: return
            if (cim.commandType != CommandType.IDENTIFY) return

            scope.launch {
                try {
                    val deviceInfo = DeviceInfo(
                        ip = "",
                        nombre = gatt.device.name ?: "Device_${mac.takeLast(5)}",
                        tipo = DeviceType.UNKNOWN,
                        mac = mac,
                        appType = cim.sourceApp,
                        rssi = 0,
                        lastSeen = System.currentTimeMillis(),
                        authorized = false,
                        isConnected = true
                    )
                    GlobalDeviceRegistry.registry.register(mac, deviceInfo)

                    val decision = GlobalPermissionManager.getInstance()
                        .requestPermission(mac, cim.sourceApp, deviceInfo.nombre)

                    val responsePayload = when (decision) {
                        PermissionDecision.APPROVED -> "AUTHORIZED"
                        PermissionDecision.REJECTED -> "REJECTED"
                        PermissionDecision.TIMEOUT -> "TIMEOUT"
                        else -> "REJECTED"
                    }

                    val appIdentifier = AppIdentifier.getInstance()
                    val response = CimMessage(
                        sourceMac = appIdentifier.deviceMac,
                        sourceApp = getCurrentAppType(context),
                        destMac = mac,
                        destApp = cim.sourceApp,
                        commandType = CommandType.IDENTIFIED,
                        payload = responsePayload
                    )

                    val rxChar = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID_RX)
                    if (rxChar != null) {
                        sendLargeCommand(gatt, rxChar, response.toTransportString())
                        onLog("→ SENT IDENTIFIED to $mac: $responsePayload")
                    }
                } catch (e: Exception) {
                    onLog("⚠ Error IDENTIFY [$mac]: ${e.message}")
                }
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    fun send(cmd: String, requireAuthorization: Boolean = false, authorized: Boolean = true) {
        if (requireAuthorization && !authorized) {
            onLog("✗ No autorizado para enviar: $cmd")
            return
        }
        val target = connectedDevices.keys.firstOrNull()
        if (target == null) {
            onLog("✗ ERROR: NO CONEXIÓN BLE")
            return
        }
        sendToDevice(target, cmd)
    }

    @SuppressLint("MissingPermission")
    fun sendToDevice(address: String, cmd: String) {
        val targetGatt = connectedDevices[address] ?: run {
            onLog("✗ ERROR: No GATT para $address")
            return
        }
        try {
            val rxChar = targetGatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID_RX)
            if (rxChar == null) {
                onLog("✗ ERROR: CARACTERÍSTICA NO ENCONTRADA en $address")
                return
            }
            sendLargeCommand(targetGatt, rxChar, cmd)
        } catch (e: Exception) {
            onLog("✗ ERROR TX a $address: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendLargeCommand(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, cmd: String) {
        val bytes = (cmd + "\n").toByteArray(Charsets.UTF_8)
        if (bytes.size <= MAX_BLE_PACKET) {
            writeChunk(gatt, characteristic, bytes)
            onLog("→ TX: $cmd")
            return
        }

        var offset = 0
        var fragmentNumber = 0
        while (offset < bytes.size) {
            val chunkSize = min(MAX_BLE_PACKET, bytes.size - offset)
            val chunk = bytes.sliceArray(offset until offset + chunkSize)
            offset += chunkSize
            writeChunk(gatt, characteristic, chunk)
            onLog("→ TX FRAG[$fragmentNumber]: ${String(chunk, Charsets.UTF_8)}")
            fragmentNumber++
            Thread.sleep(20)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeChunk(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, chunk: ByteArray) {
        if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeCharacteristic(characteristic, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = chunk
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendIdentification(mac: String) {
        try {
            val appIdentifier = AppIdentifier.getInstance()
            val identifyMsg = CimMessageBuilder.createIdentifyMessage(
                mac = appIdentifier.deviceMac,
                appType = getCurrentAppType(context),
                version = appIdentifier.appVersion
            ).toTransportString()
            sendToDevice(mac, identifyMsg)
            onLog("→ SENT IDENTIFY to $mac")
        } catch (e: Exception) {
            onLog("⚠ Cannot send IDENTIFY to $mac: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        activeBleScanCallback?.let { cb ->
            try {
                scanner?.stopScan(cb)
            } catch (e: Exception) {
                onLog("⚠ Error deteniendo escaneo BLE: ${e.message}")
            }
        }
        activeBleScanCallback = null
        try {
            adapter?.cancelDiscovery()
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() = stopScanInternal()

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun isConnected(address: String? = null): Boolean {
        return if (address != null) connectedDevices.containsKey(address) else connectedDevices.isNotEmpty()
    }

    fun getConnectedAddresses(): Set<String> = connectedDevices.keys.toSet()

    fun disconnectAll() = disconnect(null)

    fun release() {
        stopScan()
        disconnect()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: Exception) {
            }
            isReceiverRegistered = false
        }
        scope.cancel()
    }
}
