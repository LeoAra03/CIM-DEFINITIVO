package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.AppType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * REGISTRO DE DISPOSITIVOS CIM v5.1 (O(1) GUARANTEE)
 *
 * Soporta dos modos:
 * 1. LEGACY: Por IP (para compatibilidad con ESP32)
 * 2. NUEVO: Por MAC Address (para identificación de apps Android)
 */

// ============= DATA CLASSES =============

data class DeviceInfo(
    val ip: String,
    val nombre: String,
    val tipo: DeviceType,
    @Volatile var estado: String = "IDLE",
    @Volatile var ultimoComando: String = "NONE",
    // Nuevos campos para protocolo CIM v5.1
    var mac: String = "",
    var appType: AppType = AppType.UNKNOWN,
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

// ============= LEGACY REGISTRY (Por IP) =============

object DeviceRegistry {
    // IP -> Información del Dispositivo (LEGACY)
    val dispositivosHardware = ConcurrentHashMap<String, DeviceInfo>()

    fun registrarDispositivo(ip: String, nombre: String, tipo: DeviceType) {
        dispositivosHardware[ip] = DeviceInfo(ip, nombre, tipo)
    }

    fun actualizarEstado(ip: String, nuevoEstado: String) {
        dispositivosHardware[ip]?.estado = nuevoEstado
    }
}

// ============= NUEVO REGISTRY (Por MAC - O(1)) =============

class MobileDeviceRegistry {

    // Cambiado a ConcurrentHashMap para garantizar acceso O(1) y seguridad en concurrencia.
    // MAC -> DeviceInfo (Acceso O(1))
    private val devicesByMac: ConcurrentHashMap<String, DeviceInfo> = ConcurrentHashMap()

    // AppType -> Set<MAC> (búsqueda por tipo). Usamos keySet para operaciones O(1) en add/remove.
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
            // Si cambió el tipo de app, actualizar índices
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
        // Mapear solo las MACs del tipo (O(k) donde k = dispositivos del tipo)
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
        // Quitar de índices por tipo
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
 * Singleton global para uso desde cualquier parte
 */
object GlobalDeviceRegistry {
    val registry = MobileDeviceRegistry()
}

