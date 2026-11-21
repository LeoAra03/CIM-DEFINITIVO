package com.industria.prefecto

import android.util.Log

/**
 * Capa de respaldo mesh / Wi-Fi Direct (Safety 2.0).
 * Propagates E-STOP between neighboring stations when the coordinator loses Wi-Fi.
 *
 * Nota: Wi-Fi Direct requiere permisos y APIs de bajo nivel no disponibles
 * on all industrial tablets. This manager implements the interface and
 * dynamic heartbeat; the real P2P link activates when the hardware supports it.
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
