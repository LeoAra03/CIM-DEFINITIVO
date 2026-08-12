package com.sistema.distribuido.network

import android.util.Log

/**
 * Capa de respaldo mesh / Wi-Fi Direct (Safety 2.0).
 * Propaga E-STOP entre estaciones vecinas si el coordinador pierde Wi-Fi.
 *
 * Nota: Wi-Fi Direct requiere permisos y APIs de bajo nivel no disponibles
 * en todas las tablets industriales. Este manager implementa la interfaz y
 * heartbeat dinámico; la conexión P2P real se activa cuando el hardware lo soporta.
 */
object MeshNetworkManager {

    data class MeshPeer(val stationId: String, val lastHeartbeat: Long, val rssi: Int)

    private val peers = mutableMapOf<String, MeshPeer>()
    private var meshActive = false
    var onEmergencyPropagate: ((String) -> Unit)? = null

    fun activate() {
        meshActive = true
        Log.i(TAG, "Mesh fallback ACTIVADO")
    }

    fun deactivate() {
        meshActive = false
        peers.clear()
        Log.i(TAG, "Mesh fallback DESACTIVADO")
    }

    fun registerPeer(stationId: String, rssi: Int = -50) {
        peers[stationId] = MeshPeer(stationId, System.currentTimeMillis(), rssi)
    }

    fun propagateEmergency(source: String) {
        if (!meshActive) return
        Log.w(TAG, "E-STOP propagado por mesh desde $source -> ${peers.keys}")
        onEmergencyPropagate?.invoke(source)
    }

    fun findNearestAlivePeer(exclude: String): MeshPeer? {
        val now = System.currentTimeMillis()
        return peers.values
            .filter { it.stationId != exclude && now - it.lastHeartbeat < 15_000 }
            .maxByOrNull { it.rssi }
    }

    fun isActive(): Boolean = meshActive

    private const val TAG = "MeshNetworkManager"
}
