package com.sistema.distribuido.network

import android.content.Context
import android.content.SharedPreferences
import com.sistema.distribuido.network.protocol.AppType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * GESTOR DE PERMISOS CIM v5.1
 *
 * Sistema autom ático de autorización con:
 * - Handshake de identificación
 * - Dialogs de autorización
 * - Persistencia de decisiones (recordar decisión)
 * - Timeout automático (5s)
 * - Revocación en cualquier momento
 */

data class BlockedDevice(
    val mac: String,
    val reason: String,
    val blockedAt: Long
)

data class PermissionRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mac: String,
    val appType: AppType,
    val deviceName: String,
    val requestedAt: Long = System.currentTimeMillis(),
    var respondedAt: Long = 0,
    var approved: Boolean = false,
    var rememberDecision: Boolean = false
)

enum class PermissionDecision {
    PENDING,
    APPROVED,
    REJECTED,
    TIMEOUT
}

class PermissionManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("cim_permissions", Context.MODE_PRIVATE)

    // Solicitudes en vuelo: MAC -> PermissionRequest
    private val pendingRequests = ConcurrentHashMap<String, PermissionRequest>()

    // Decisiones recordadas: MAC -> (Approved: Boolean, Timestamp)
    private val remembereddecisions = ConcurrentHashMap<String, Pair<Boolean, Long>>()

    // Blocked devices are persistent and are denied before any auto-approval path.
    private val blockedDevices = ConcurrentHashMap<String, BlockedDevice>()

    // Listeners para cambios
    private val listeners: MutableList<PermissionListener> = mutableListOf()

    interface PermissionListener {
        suspend fun onPermissionRequested(request: PermissionRequest)
        suspend fun onPermissionApproved(mac: String)
        suspend fun onPermissionRejected(mac: String)
        suspend fun onPermissionExpired(mac: String)
    }

    init {
        loadRememberedDecisions()
        loadBlockedDevices()
    }

    suspend fun addListener(listener: PermissionListener) {
        listeners.add(listener)
    }

    /**
     * Solicita permiso para un dispositivo
     * CORREGIDO: autoApprove solo permitido en DEBUG y con protección extra
     */
    suspend fun requestPermission(
        mac: String,
        appType: AppType,
        deviceName: String = "Unknown Device"
    ): PermissionDecision {
        if (isBlocked(mac)) {
            AuthorizationManager.deny(mac)
            return PermissionDecision.REJECTED
        }

        // CORREGIDO: Si estamos en modo test y auto-approve activado, solo en DEBUG builds
        // y nunca si el dispositivo está bloqueado
        try {
            if (GlobalPermissionManager.isTestAutoApproveAllowed()) {
                // Guardar decisión recordada y devolver APPROVED solo en modo debug
                remembereddecisions[mac] = Pair(true, System.currentTimeMillis())
                saveRememberedDecision(mac, true)
                // Mantener sincronía con AuthorizationManager
                try {
                    AuthorizationManager.authorize(mac)
                } catch (_: Exception) {
                    android.util.Log.w("PermissionManager", "No se pudo autorizar $mac en AuthorizationManager")
                }
                return PermissionDecision.APPROVED
            }
        } catch (e: Exception) {
            android.util.Log.w("PermissionManager", "Error en autoApprove check: ${e.message}")
        }
        // Verificar si ya tiene permiso recordado
        val remembered = remembereddecisions[mac]
        if (remembered != null) {
            val (approved, timestamp) = remembered
            // Recordar decisión cada 24 horas
            if (System.currentTimeMillis() - timestamp < 86400000) {
                return if (approved) PermissionDecision.APPROVED else PermissionDecision.REJECTED
            }
        }

        // Crear nueva solicitud
        val request = PermissionRequest(
            mac = mac,
            appType = appType,
            deviceName = deviceName
        )

        pendingRequests[mac] = request
        listeners.forEach { it.onPermissionRequested(request) }

        // Esperar respuesta con timeout de 5 segundos
        return waitForApproval(mac, timeout = 5000)
    }

    /**
     * Aprueba una solicitud de permiso
     */
    suspend fun approve(mac: String, rememberDecision: Boolean = true) {
        if (isBlocked(mac)) return
        val request = pendingRequests[mac] ?: return

        request.approved = true
        request.respondedAt = System.currentTimeMillis()
        request.rememberDecision = rememberDecision

        if (rememberDecision) {
            remembereddecisions[mac] = Pair(true, System.currentTimeMillis())
            saveRememberedDecision(mac, true)
        }

        // Actualizar AuthorizationManager para reflejar la aprobación
        try {
            AuthorizationManager.authorize(mac)
        } catch (_: Exception) {}

        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionApproved(mac) }
    }

    /**
     * Rechaza una solicitud de permiso
     */
    suspend fun reject(mac: String, rememberDecision: Boolean = true) {
        val request = pendingRequests[mac] ?: return

        request.approved = false
        request.respondedAt = System.currentTimeMillis()
        request.rememberDecision = rememberDecision

        if (rememberDecision) {
            remembereddecisions[mac] = Pair(false, System.currentTimeMillis())
            saveRememberedDecision(mac, false)
        }

        // Actualizar AuthorizationManager para reflejar el rechazo
        try {
            AuthorizationManager.deny(mac)
        } catch (_: Exception) {}

        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionRejected(mac) }
    }


    /** Bloquea persistentemente un equipo desconocido o no confiable. */
    suspend fun ban(mac: String, reason: String = "Bloqueado por operador") {
        val normalizedMac = mac.trim().uppercase()
        if (normalizedMac.isBlank()) return
        val blocked = BlockedDevice(normalizedMac, reason.take(160), System.currentTimeMillis())
        blockedDevices[normalizedMac] = blocked
        remembereddecisions[normalizedMac] = Pair(false, blocked.blockedAt)
        pendingRequests.remove(normalizedMac)
        sharedPrefs.edit()
            .putString("blocked_${normalizedMac}_reason", blocked.reason)
            .putLong("blocked_${normalizedMac}_timestamp", blocked.blockedAt)
            .apply()
        AuthorizationManager.deny(normalizedMac)
        listeners.forEach { it.onPermissionRejected(normalizedMac) }
    }

    /** Retira un bloqueo persistente; no autoriza el dispositivo automáticamente. */
    suspend fun unban(mac: String) {
        val normalizedMac = mac.trim().uppercase()
        blockedDevices.remove(normalizedMac)
        sharedPrefs.edit()
            .remove("blocked_${normalizedMac}_reason")
            .remove("blocked_${normalizedMac}_timestamp")
            .apply()
        AuthorizationManager.revoke(normalizedMac)
    }

    fun isBlocked(mac: String): Boolean = blockedDevices.containsKey(mac.trim().uppercase())

    fun getBlockedDevices(): List<BlockedDevice> =
        blockedDevices.values.sortedByDescending { it.blockedAt }

    /**
     * Revoca los permisos de un dispositivo
     */
    suspend fun revoke(mac: String) {
        remembereddecisions.remove(mac)
        pendingRequests.remove(mac)
        sharedPrefs.edit().remove("perm_$mac").apply()
        // Revocar en AuthorizationManager
        try {
            AuthorizationManager.revoke(mac)
        } catch (_: Exception) {}
    }

    /**
     * Verifica si un dispositivo tiene permiso
     */
    suspend fun hasPermission(mac: String): Boolean {
        return remembereddecisions[mac]?.first ?: false
    }

    /**
     * Obtiene todas las solicitudes pendientes
     */
    fun getPendingRequests(): List<PermissionRequest> {
        return pendingRequests.values.toList()
    }

    /**
     * Obtiene decisiones recordadas
     */
    fun getRememberedDecisions(): Map<String, Boolean> {
        return remembereddecisions.mapValues { it.value.first }
    }

    /**
     * Limpia decisiones antiguas (> 30 días)
     */
    suspend fun cleanupOldDecisions() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        remembereddecisions.filter { (_, pair) ->
            (now - pair.second) > thirtyDaysMs
        }.forEach { (mac, _) ->
            revoke(mac)
        }
    }

    // ============= PRIVATE METHODS =============

    private suspend fun waitForApproval(
        mac: String,
        timeout: Long = 5000
    ): PermissionDecision {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            val request = pendingRequests[mac]
            if (request != null && request.respondedAt > 0) {
                return if (request.approved) {
                    PermissionDecision.APPROVED
                } else {
                    PermissionDecision.REJECTED
                }
            }
            // CORREGIDO: usar delay no bloqueante en lugar de Thread.sleep
            kotlinx.coroutines.delay(100)
        }
        // Timeout
        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionExpired(mac) }
        return PermissionDecision.TIMEOUT
    }

    private fun loadRememberedDecisions() {
        val prefs = sharedPrefs
        // Leer todas las entradas de timestamp guardadas y su bandera approved asociada
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("perm_") && key.endsWith("_timestamp")) {
                try {
                    val mac = key.removePrefix("perm_").removeSuffix("_timestamp")
                    val timestamp = when (value) {
                        is Long -> value
                        is Int -> value.toLong()
                        is String -> value.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val approvedLong = prefs.getLong("perm_${mac}_approved", 0L)
                    val approved = approvedLong == 1L
                    if (timestamp > 0L) {
                        remembereddecisions[mac] = Pair(approved, timestamp)
                        // Reflejar estado inicial en AuthorizationManager
                        try {
                            if (approved) AuthorizationManager.authorize(mac) else AuthorizationManager.deny(mac)
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    // ignorar entradas mal formateadas
                }
            }
        }
    }

    private fun loadBlockedDevices() {
        sharedPrefs.all.forEach { (key, value) ->
            if (key.startsWith("blocked_") && key.endsWith("_timestamp")) {
                val mac = key.removePrefix("blocked_").removeSuffix("_timestamp")
                val timestamp = value as? Long ?: return@forEach
                val reason = sharedPrefs.getString("blocked_${mac}_reason", "Bloqueado por operador")
                    ?: "Bloqueado por operador"
                blockedDevices[mac] = BlockedDevice(mac, reason, timestamp)
                AuthorizationManager.deny(mac)
            }
        }
    }

    private fun saveRememberedDecision(mac: String, approved: Boolean) {
        sharedPrefs.edit().putLong(
            "perm_${mac}_approved",
            if (approved) 1L else 0L
        ).apply()

        sharedPrefs.edit().putLong(
            "perm_${mac}_timestamp",
            System.currentTimeMillis()
        ).apply()
    }
}

/**
 * Singleton global para acceso centralizado
 * CORREGIDO: autoApprove protegido para solo DEBUG builds
 */
object GlobalPermissionManager {
    lateinit var manager: PermissionManager
    // Modo de test: cuando true, todas las solicitudes se aprueban automáticamente (útil para E2E sin interacción)
    // CORREGIDO: solo permitido en debug, con validación explícita
    @Volatile
    var autoApproveTestMode: Boolean = false
    private var debugAllowed: Boolean = false

    fun init(context: Context) {
        manager = PermissionManager(context)
        // Detectar si es build debuggable
        debugAllowed = try {
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) { false }
    }

    fun getInstance(): PermissionManager = manager

    fun isTestAutoApproveAllowed(): Boolean {
        // Solo true si debugAllowed y flag activado
        return debugAllowed && autoApproveTestMode
    }
}




