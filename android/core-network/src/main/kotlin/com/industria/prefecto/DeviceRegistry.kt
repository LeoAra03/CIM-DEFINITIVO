package com.industria.prefecto

import com.industria.prefecto.AppType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * CIM DEVICE REGISTRY v5.1 (O(1) GUARANTEED)
 *
 * Soporta dos modos:
 * 1. LEGACY: by IP (ESP32 compatibility)
 * 2. NEW: by MAC address (Android app identification)
 */

// ============= DATA CLASSES =============

data class DeviceInfo(
    val ip: String,
    val nombre: String,
    val tipo: DeviceType,
    @Volatile var estado: String = "IDLE",
    @Volatile var ultimoComando: String = "NONE",
    // New fields for CIM protocol v5.1
    var mac: String = "",
    var appType: AppType = AppType.UNKNOWN,
    val stationUuid: String = "",
    val hardwareModel: String = "",
    val capabilities: String = "",
    var rssi: Int = 0,
    @Volatile var lastSeen: Long = System.currentTimeMillis(),
    @Volatile var authorized: Boolean = false,
    val version: String = "1.0",
    @Volatile var isConnected: Boolean = false
) {
    fun isAlive(): Boolean = (System.currentTimeMillis() - lastSeen) < 10000
    fun canExecute(): Boolean = isConnected && authorized && isAlive()
}

enum class DeviceType {
    CONVEYOR,   // Cinta transportadora
    ROBOT_ARM,  // Scorbot / Brazo
    SENSOR_HUB, // Sensores Aruco/Proximidad
    GATEWAY,    // ESP-NOW Bridge
    UNKNOWN
}

// ============= LEGACY REGISTRY (by IP) =============

object DeviceRegistry {
    // IP -> device information (LEGACY)
    val dispositivosHardware = ConcurrentHashMap<String, DeviceInfo>()

    fun registrarDispositivo(ip: String, nombre: String, tipo: DeviceType) {
        dispositivosHardware[ip] = DeviceInfo(ip, nombre, tipo)
    }

    fun actualizarEstado(ip: String, nuevoEstado: String) {
        dispositivosHardware[ip]?.estado = nuevoEstado
    }
}

// ============= NEW REGISTRY (by MAC - O(1)) =============

class MobileDeviceRegistry {

    // ConcurrentHashMap guarantees O(1) access and thread safety.
    // MAC -> DeviceInfo (Acceso O(1))
    private val devicesByMac: ConcurrentHashMap<String, DeviceInfo> = ConcurrentHashMap()

    // AppType -> Set<MAC> (lookup by type). keySet keeps add/remove O(1).
    private val devicesByType: ConcurrentHashMap<AppType, MutableSet<String>> = ConcurrentHashMap()

    // Listeners thread-safe
    private val listeners: CopyOnWriteArrayList<RegistryListener> = CopyOnWriteArrayList()

    interface RegistryListener {
        suspend fun onDeviceAdded(device: DeviceInfo)
        suspend fun onDeviceRemoved(mac: String)
        suspend fun onDeviceUpdated(device: DeviceInfo)
        suspend fun onAuthorizationChanged(mac: String, authorized: Boolean)
    }

    suspend fun addListener(listener: RegistryListener) {
        // CopyOnWriteArrayList es thread-safe, no lock requerido
        listeners.add(listener)
    }

    suspend fun register(mac: String, device: DeviceInfo) {
        val now = System.currentTimeMillis()
        val existing = devicesByMac.put(mac, device.copy(lastSeen = now, isConnected = true))

        if (existing != null) {
            // If the app type changed, update the indexes
            if (existing.appType != device.appType) {
                removeFromTypeIndex(existing.appType, mac)
                addToTypeIndex(device.appType, mac)
            }
            listeners.forEach { try { it.onDeviceUpdated(device) } catch (_: Exception) {} }
        } else {
            addToTypeIndex(device.appType, mac)
            listeners.forEach { try { it.onDeviceAdded(device) } catch (_: Exception) {} }
        }
    }

    suspend fun getDeviceByMac(mac: String): DeviceInfo? {
        // Lectura O(1) directa en ConcurrentHashMap
        return devicesByMac[mac]
    }

    suspend fun getDevicesByType(appType: AppType): List<DeviceInfo> {
        val macs = devicesByType[appType] ?: return emptyList()
        // Map only the MACs of that type (O(k) where k is the device count)
        return macs.mapNotNull { devicesByMac[it] }
    }

    suspend fun getAuthorizedDevices(): List<DeviceInfo> {
        return devicesByMac.values.filter { it.authorized && it.isConnected }
    }

    suspend fun getAllDevices(): List<DeviceInfo> {
        return devicesByMac.values.toList()
    }

    suspend fun authorize(mac: String): Boolean {
        val device = devicesByMac[mac] ?: return false
        devicesByMac[mac] = device.copy(authorized = true)
        listeners.forEach { try { it.onAuthorizationChanged(mac, true) } catch (_: Exception) {} }
        return true
    }

    suspend fun disconnect(mac: String): Boolean {
        val device = devicesByMac[mac] ?: return false
        devicesByMac[mac] = device.copy(isConnected = false)
        // Remove from the type indexes
        removeFromTypeIndex(device.appType, mac)
        listeners.forEach { try { it.onDeviceRemoved(mac) } catch (_: Exception) {} }
        return true
    }

    suspend fun updateRssi(mac: String, rssi: Int) {
        val device = devicesByMac[mac] ?: return
        devicesByMac[mac] = device.copy(rssi = rssi, lastSeen = System.currentTimeMillis())
    }

    suspend fun ping(mac: String) {
        val device = devicesByMac[mac] ?: return
        devicesByMac[mac] = device.copy(lastSeen = System.currentTimeMillis())
    }

    suspend fun clear() {
        devicesByMac.clear()
        devicesByType.clear()
    }

    private fun addToTypeIndex(appType: AppType, mac: String) {
        val set = devicesByType.computeIfAbsent(appType) { ConcurrentHashMap.newKeySet<String>() }
        set.add(mac)
    }

    private fun removeFromTypeIndex(appType: AppType, mac: String) {
        val set = devicesByType[appType] ?: return
        set.remove(mac)
        if (set.isEmpty()) devicesByType.remove(appType)
    }
}

/**
 * Global singleton for use anywhere
 */
object GlobalDeviceRegistry {
    val registry = MobileDeviceRegistry()
}

