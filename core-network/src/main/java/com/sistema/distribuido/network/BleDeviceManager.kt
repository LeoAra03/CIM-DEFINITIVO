package com.sistema.distribuido.network

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.util.*

/**
 * GESTOR DE RED HÍBRIDA BLE + ESP-NOW (COMPLEJIDAD O(1))
 * Basado en las especificaciones oficiales de Espressif.
 * Permite al Coordinador Maestro inyectar identidades para formar la red en malla.
 */
class BleDeviceManager(private val context: Context, private val onLog: (String) -> Unit) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    // Almacenamiento O(1) para acceso instantáneo a dispositivos hallados
    val discoveredDevices: SnapshotStateMap<String, ScanResult> = mutableStateMapOf()
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000

    // UUIDs para Configuración de Malla (Estándar Industrial)
    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CHAR_UUID_RX = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bleScanner == null || isScanning) return

        onLog("ESCANEANDO MALLA ESP-NOW...")
        handler.postDelayed({
            if (isScanning) {
                isScanning = false
                bleScanner.stopScan(scanCallback)
                onLog("ESCANEADO FINALIZADO (O(1) Map listo)")
            }
        }, SCAN_PERIOD)

        isScanning = true
        discoveredDevices.clear()
        bleScanner.startScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address

            // Filtro de seguridad: Solo capturar placas del ecosistema CIM/ESP32
            if (deviceName.contains("ESP32", ignoreCase = true) || deviceName.contains("CIM", ignoreCase = true)) {
                if (!discoveredDevices.containsKey(deviceAddress)) {
                    discoveredDevices[deviceAddress] = result
                    onLog("HARDWARE CIM DETECTADO: $deviceName ($deviceAddress)")
                }
            }
        }
    }

    /**
     * CONEXIÓN Y CONFIGURACIÓN DINÁMICA DE MALLA
     * Envía la MAC del maestro y parámetros de red al ESP32 vía GATT.
     */
    @SuppressLint("MissingPermission")
    fun connectAndConfigurePeer(address: String, meshConfig: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        onLog("CONFIGURANDO PEER: $address...")
        
        device?.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onLog("ENLACE ESTABLECIDO. Descubriendo Servicios...")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onLog("PEER DESCONECTADO.")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(CHAR_UUID_RX)
                    
                    if (characteristic != null) {
                        // Inyectar MAC y Parámetros para ESP-NOW dynamic add_peer
                        val data = meshConfig.toByteArray()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            characteristic.value = data
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(characteristic)
                        }
                        onLog("MESH_SYNC: Enviando trama de configuración...")
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onLog("MESH_READY: Peer configurado en la red en malla.")
                }
                gatt.disconnect()
            }
        })
    }
}
