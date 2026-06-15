package com.sistema.distribuido.network

/**
 * Dispositivo Bluetooth descubierto durante escaneo híbrido BLE + Classic.
 */
data class DiscoveredBluetoothDevice(
    val address: String,
    val name: String
)
