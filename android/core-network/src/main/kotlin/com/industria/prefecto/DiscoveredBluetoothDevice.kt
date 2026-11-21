package com.industria.prefecto

/**
 * Bluetooth device discovered during the hybrid BLE + Classic scan.
 */
data class DiscoveredBluetoothDevice(
    val address: String,
    val name: String
)
