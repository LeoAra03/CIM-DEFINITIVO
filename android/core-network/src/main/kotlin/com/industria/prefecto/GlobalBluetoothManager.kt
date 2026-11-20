package com.industria.prefecto

import android.content.Context

/**
 * Global singleton for BluetoothHardwareManager
 */
object GlobalBluetoothManager {
    private var instance: BluetoothHardwareManager? = null

    fun init(context: Context, onLog: (String) -> Unit = {}, onDataReceived: ((String, String) -> Unit)? = null) {
        if (instance == null) {
            instance = BluetoothHardwareManager(context.applicationContext, onLog, onDataReceived)
        }
    }

    fun getInstance(): BluetoothHardwareManager {
        return instance ?: throw IllegalStateException("GlobalBluetoothManager no inicializado. Llama a init() primero.")
    }

    fun getInstanceOrNull(): BluetoothHardwareManager? = instance
}
