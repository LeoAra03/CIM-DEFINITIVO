package com.industria.prefecto

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Block-list contract consumed by [AuthorizationManager].
 *
 * Kept deliberately small so tests can provide an in-memory fake.
 */
interface PermissionManager {
    fun isBlocked(mac: String): Boolean

    fun block(mac: String)

    fun unblock(mac: String)

    fun getBlockedDevices(): Set<String>
}

/** Persistent record of a blocked station. */
data class BlockedDevice(
    val mac: String,
    val reason: String,
    val blockedAt: Long,
)

/** One in-flight permission request. */
data class PermissionRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mac: String,
    val appType: AppType,
    val deviceName: String,
    val requestedAt: Long = System.currentTimeMillis(),
    var respondedAt: Long = 0,
    var approved: Boolean = false,
    var rememberDecision: Boolean = false,
)

/** Outcome of a permission request. */
enum class PermissionDecision {
    PENDING,
    APPROVED,
    REJECTED,
    TIMEOUT,
}

/**
 * SharedPreferences-backed permission manager.
 *
 * Handles identification handshakes, operator approval dialogs, remembered
 * decisions, a five-second approval timeout, and persistent blocking.
 */
class LocalPermissionManager(private val context: Context) : PermissionManager {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("cim_permissions", Context.MODE_PRIVATE)

    private val pendingRequests = ConcurrentHashMap<String, PermissionRequest>()
    private val remembereddecisions = ConcurrentHashMap<String, Pair<Boolean, Long>>()
    private val blockedDevices = ConcurrentHashMap<String, BlockedDevice>()
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
     * Requests permission for a device.
     *
     * Auto-approval is only possible in debug builds with the explicit test
     * flag enabled, and never for a blocked device.
     */
    suspend fun requestPermission(
        mac: String,
        appType: AppType,
        deviceName: String = "Unknown Device",
    ): PermissionDecision {
        if (isBlocked(mac)) {
            AuthorizationManager.deny(mac)
            return PermissionDecision.REJECTED
        }
        try {
            if (GlobalPermissionManager.isTestAutoApproveAllowed()) {
                remembereddecisions[mac] = Pair(true, System.currentTimeMillis())
                saveRememberedDecision(mac, true)
                try {
                    AuthorizationManager.authorize(mac)
                } catch (_: Exception) {
                    android.util.Log.w("PermissionManager", "Could not authorize $mac")
                }
                return PermissionDecision.APPROVED
            }
        } catch (e: Exception) {
            android.util.Log.w("PermissionManager", "Auto-approve check failed: ${e.message}")
        }

        val remembered = remembereddecisions[mac]
        if (remembered != null) {
            val (approved, timestamp) = remembered
            if (System.currentTimeMillis() - timestamp < 86_400_000) {
                return if (approved) PermissionDecision.APPROVED else PermissionDecision.REJECTED
            }
        }

        val request = PermissionRequest(
            mac = mac,
            appType = appType,
            deviceName = deviceName,
        )
        pendingRequests[mac] = request
        listeners.forEach { it.onPermissionRequested(request) }
        return waitForApproval(mac, timeout = 5_000)
    }

    /** Approves an in-flight request. */
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
        try {
            AuthorizationManager.authorize(mac)
        } catch (_: Exception) {
            // AuthorizationManager is optional for pure persistence flows.
        }
        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionApproved(mac) }
    }

    /** Rejects an in-flight request. */
    suspend fun reject(mac: String, rememberDecision: Boolean = true) {
        val request = pendingRequests[mac] ?: return
        request.approved = false
        request.respondedAt = System.currentTimeMillis()
        request.rememberDecision = rememberDecision
        if (rememberDecision) {
            remembereddecisions[mac] = Pair(false, System.currentTimeMillis())
            saveRememberedDecision(mac, false)
        }
        try {
            AuthorizationManager.deny(mac)
        } catch (_: Exception) {
            // AuthorizationManager is optional for pure persistence flows.
        }
        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionRejected(mac) }
    }

    /** Persistently blocks an untrusted device and notifies listeners. */
    suspend fun ban(mac: String, reason: String = "Blocked by operator") {
        val normalizedMac = mac.trim().uppercase(java.util.Locale.ROOT)
        if (normalizedMac.isBlank()) return
        persistBlock(normalizedMac, reason)
        AuthorizationManager.refresh(normalizedMac)
        listeners.forEach { it.onPermissionRejected(normalizedMac) }
    }

    /** Removes a persistent block without auto-authorizing the device. */
    suspend fun unban(mac: String) {
        val normalizedMac = mac.trim().uppercase(java.util.Locale.ROOT)
        removePersistedBlock(normalizedMac)
        AuthorizationManager.refresh(normalizedMac)
    }

    override fun isBlocked(mac: String): Boolean =
        blockedDevices.containsKey(mac.trim().uppercase(java.util.Locale.ROOT))

    override fun block(mac: String) {
        val normalizedMac = mac.trim().uppercase(java.util.Locale.ROOT)
        if (normalizedMac.isBlank()) return
        persistBlock(normalizedMac, "Blocked by operator")
        AuthorizationManager.refresh(normalizedMac)
    }

    override fun unblock(mac: String) {
        val normalizedMac = mac.trim().uppercase(java.util.Locale.ROOT)
        removePersistedBlock(normalizedMac)
        AuthorizationManager.refresh(normalizedMac)
    }

    override fun getBlockedDevices(): Set<String> = blockedDevices.keys.toSet()

    /** Detailed block records, sorted with the newest first. */
    fun getBlockedDeviceDetails(): List<BlockedDevice> =
        blockedDevices.values.sortedByDescending { it.blockedAt }

    /** Revokes remembered permission for a device. */
    suspend fun revoke(mac: String) {
        remembereddecisions.remove(mac)
        pendingRequests.remove(mac)
        sharedPrefs.edit().remove("perm_$mac").apply()
        try {
            AuthorizationManager.revoke(mac)
        } catch (_: Exception) {
            // AuthorizationManager is optional for pure persistence flows.
        }
    }

    /** Reports whether a remembered approval exists for the device. */
    suspend fun hasPermission(mac: String): Boolean =
        remembereddecisions[mac]?.first ?: false

    fun getPendingRequests(): List<PermissionRequest> = pendingRequests.values.toList()

    fun getRememberedDecisions(): Map<String, Boolean> =
        remembereddecisions.mapValues { it.value.first }

    /** Drops remembered decisions older than thirty days. */
    suspend fun cleanupOldDecisions() {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        remembereddecisions.filter { (_, pair) ->
            (now - pair.second) > thirtyDaysMs
        }.forEach { (mac, _) ->
            revoke(mac)
        }
    }

    private fun persistBlock(normalizedMac: String, reason: String) {
        val blocked = BlockedDevice(normalizedMac, reason.take(160), System.currentTimeMillis())
        blockedDevices[normalizedMac] = blocked
        remembereddecisions[normalizedMac] = Pair(false, blocked.blockedAt)
        pendingRequests.remove(normalizedMac)
        sharedPrefs.edit()
            .putString("blocked_${normalizedMac}_reason", blocked.reason)
            .putLong("blocked_${normalizedMac}_timestamp", blocked.blockedAt)
            .apply()
    }

    private fun removePersistedBlock(normalizedMac: String) {
        blockedDevices.remove(normalizedMac)
        sharedPrefs.edit()
            .remove("blocked_${normalizedMac}_reason")
            .remove("blocked_${normalizedMac}_timestamp")
            .apply()
    }

    private suspend fun waitForApproval(
        mac: String,
        timeout: Long = 5_000,
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
            kotlinx.coroutines.delay(100)
        }
        pendingRequests.remove(mac)
        listeners.forEach { it.onPermissionExpired(mac) }
        return PermissionDecision.TIMEOUT
    }

    private fun loadRememberedDecisions() {
        sharedPrefs.all.forEach { (key, value) ->
            if (key.startsWith("perm_") && key.endsWith("_timestamp")) {
                try {
                    val mac = key.removePrefix("perm_").removeSuffix("_timestamp")
                    val timestamp = when (value) {
                        is Long -> value
                        is Int -> value.toLong()
                        is String -> value.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val approved = sharedPrefs.getLong("perm_${mac}_approved", 0L) == 1L
                    if (timestamp > 0L) {
                        remembereddecisions[mac] = Pair(approved, timestamp)
                        try {
                            if (approved) {
                                AuthorizationManager.authorize(mac)
                            } else {
                                AuthorizationManager.deny(mac)
                            }
                        } catch (_: Exception) {
                            // AuthorizationManager may not be installed yet.
                        }
                    }
                } catch (_: Exception) {
                    // Skip malformed preference entries.
                }
            }
        }
    }

    private fun loadBlockedDevices() {
        sharedPrefs.all.forEach { (key, value) ->
            if (key.startsWith("blocked_") && key.endsWith("_timestamp")) {
                val mac = key.removePrefix("blocked_").removeSuffix("_timestamp")
                val timestamp = value as? Long ?: return@forEach
                val reason = sharedPrefs.getString("blocked_${mac}_reason", "Blocked by operator")
                    ?: "Blocked by operator"
                blockedDevices[mac] = BlockedDevice(mac, reason, timestamp)
                AuthorizationManager.refresh(mac)
            }
        }
    }

    private fun saveRememberedDecision(mac: String, approved: Boolean) {
        sharedPrefs.edit().putLong(
            "perm_${mac}_approved",
            if (approved) 1L else 0L,
        ).apply()
        sharedPrefs.edit().putLong(
            "perm_${mac}_timestamp",
            System.currentTimeMillis(),
        ).apply()
    }
}

/**
 * Process-wide access point for the permission manager.
 *
 * The test auto-approve flag is only honored on debuggable builds.
 */
object GlobalPermissionManager {
    lateinit var manager: LocalPermissionManager

    @Volatile
    var autoApproveTestMode: Boolean = false

    private var debugAllowed: Boolean = false

    fun init(context: Context) {
        manager = LocalPermissionManager(context)
        AuthorizationManager.install(manager)
        debugAllowed = try {
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }

    fun getInstance(): LocalPermissionManager = manager

    fun isTestAutoApproveAllowed(): Boolean = debugAllowed && autoApproveTestMode
}
