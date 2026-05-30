package com.sistema.distribuido.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.*

/**
 * Ayuda a las estaciones a obtener una identidad física (MAC) escaneando periféricos.
 */
class BluetoothIdentityHelper(private val context: Context) {
    private val adapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        manager.adapter
    }

    @SuppressLint("MissingPermission")
    fun getStationMac(onResult: (String) -> Unit) {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            onResult("00:00:00:00:00:00")
            return
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Buscamos preferentemente un ESP32 o el primer dispositivo fuerte
                if (result.device.name?.contains("ESP32") == true) {
                    scanner.stopScan(this)
                    onResult(result.device.address)
                }
            }
        }

        scanner.startScan(scanCallback)
        
        // Timeout
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            scanner.stopScan(scanCallback)
            // Si no encontró nada, devolvemos una MAC genérica o la del adapter si es posible (legacy)
            @Suppress("DEPRECATION")
            val address = adapter?.address ?: "02:00:00:00:00:00"
            onResult(address)
        }
    }
}
