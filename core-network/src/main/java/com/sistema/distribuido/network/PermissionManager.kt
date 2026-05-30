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
    }

    suspend fun addListener(listener: PermissionListener) {
        listeners.add(listener)
    }

    /**
     * Solicita permiso para un dispositivo
     */
    suspend fun requestPermission(
        mac: String,
        appType: AppType,
        deviceName: String = "Unknown Device"
    ): PermissionDecision {
        // Si estamos en modo test y auto-approve activado, responder inmediatamente
        try {
            if (GlobalPermissionManager.autoApproveTestMode) {
                // Guardar decisión recordada y devolver APPROVED
                remembereddecisions[mac] = Pair(true, System.currentTimeMillis())
                saveRememberedDecision(mac, true)
                // Mantener sincronía con AuthorizationManager
                try {
                    AuthorizationManager.authorize(mac)
                } catch (_: Exception) {}
                return PermissionDecision.APPROVED
            }
        } catch (_: Exception) {}
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
        return withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeout) {
                val request = pendingRequests[mac]
                if (request != null && request.respondedAt > 0) {
                    return@withContext if (request.approved) {
                        PermissionDecision.APPROVED
                    } else {
                        PermissionDecision.REJECTED
                    }
                }

                Thread.sleep(100)
            }

            // Timeout
            pendingRequests.remove(mac)
            listeners.forEach { it.onPermissionExpired(mac) }
            PermissionDecision.TIMEOUT
        }
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
 */
object GlobalPermissionManager {
    lateinit var manager: PermissionManager
    // Modo de test: cuando true, todas las solicitudes se aprueban automáticamente (útil para E2E sin interacción)
    var autoApproveTestMode: Boolean = false

    fun init(context: Context) {
        manager = PermissionManager(context)
    }

    fun getInstance(): PermissionManager {
        return manager
    }
}




