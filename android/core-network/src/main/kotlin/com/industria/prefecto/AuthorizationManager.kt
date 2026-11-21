package com.industria.prefecto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-device authorization state.
 *
 * Device states move through a single path:
 *
 * ```
 * PENDING -> VALIDATED -> DENIED
 * PENDING -> DENIED -> VALIDATED
 * -> BLOCKED  (only via PermissionManager.block)
 * -> PENDING  (via PermissionManager.unblock)
 * ```
 *
 * [deny] leaves the device unauthorized until the coordinator explicitly
 * re-authorizes it. Command authorization must never be inferred from
 * session liveness; use [isAuthorized].
 */
@Singleton
class AuthorizationManager @Inject constructor(
    private val permissions: PermissionManager,
) {
    enum class State { PENDING, VALIDATED, DENIED, BLOCKED }

    private val devices = kotlinx.coroutines.flow.MutableStateFlow<Map<String, DeviceAuth>>(emptyMap())

    val state: kotlinx.coroutines.flow.StateFlow<Map<String, DeviceAuth>> = devices

    fun observe(mac: String, appName: String) {
        val key = normalise(mac)
        val current = devices.value[key] ?: DeviceAuth(key, appName)
        val state = when {
            permissions.isBlocked(key) -> State.BLOCKED
            current.state == State.BLOCKED -> State.PENDING
            else -> current.state
        }
        devices.value = devices.value + (key to current.copy(appName = appName, state = state))
    }

    fun validate(mac: String) = transition(mac, State.VALIDATED)

    fun deny(mac: String) = transition(mac, State.DENIED)

    fun block(mac: String) {
        permissions.block(normalise(mac))
        transition(mac, State.BLOCKED)
    }

    fun unblock(mac: String) {
        permissions.unblock(normalise(mac))
        transition(mac, State.PENDING)
    }

    fun isAuthorized(mac: String): Boolean = stateOf(mac) == State.VALIDATED

    fun stateOf(mac: String): State = devices.value[normalise(mac)]?.state ?: State.PENDING

    fun forget(mac: String) {
        devices.value = devices.value - normalise(mac)
    }

    fun clearSession() {
        devices.value = devices.value.mapValues { (_, device) ->
            device.copy(authorizedAt = 0L, deniedAt = 0L)
        }
    }

    private fun transition(mac: String, to: State) {
        val key = normalise(mac)
        val existing = devices.value[key] ?: return
        val now = System.currentTimeMillis()
        val updated = when (to) {
            State.VALIDATED -> existing.copy(state = to, authorizedAt = now, deniedAt = 0L)
            State.DENIED -> existing.copy(state = to, deniedAt = now)
            else -> existing.copy(state = to)
        }
        devices.value = devices.value + (key to updated)
    }

    private fun normalise(mac: String): String =
        mac.trim().uppercase(java.util.Locale.ROOT)

    companion object {
        private val macRegex = Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")

        /**
         * Access point for non-Hilt call sites (activities, services, and
         * plain view models). Call [install] once during application start.
         */
        @Volatile
        var default: AuthorizationManager? = null
            private set

        fun install(permissions: PermissionManager): AuthorizationManager =
            AuthorizationManager(permissions).also { default = it }

        /** Re-derives the state of [mac] from the block list. */
        fun refresh(mac: String) {
            val manager = default ?: return
            val key = mac.trim().uppercase(java.util.Locale.ROOT)
            if (!macRegex.matches(key)) return
            val appName = manager.state.value[key]?.appName ?: UNKNOWN_APP
            manager.observe(key, appName)
        }

        // --- Compatibility facades for legacy static call sites -------------

        fun authorize(mac: String) {
            val manager = default ?: return
            val key = mac.trim().uppercase(java.util.Locale.ROOT)
            if (manager.state.value[key] == null) manager.observe(key, UNKNOWN_APP)
            manager.validate(key)
        }

        fun deny(mac: String) {
            val manager = default ?: return
            val key = mac.trim().uppercase(java.util.Locale.ROOT)
            if (manager.state.value[key] == null) manager.observe(key, UNKNOWN_APP)
            manager.deny(key)
        }

        fun revoke(mac: String) {
            default?.forget(mac)
        }

        fun isAuthorized(mac: String): Boolean = default?.isAuthorized(mac) == true

        private const val UNKNOWN_APP = "unknown"
    }
}

/** Authorization bookkeeping for one device. */
data class DeviceAuth(
    val mac: String,
    val appName: String,
    val state: AuthorizationManager.State = AuthorizationManager.State.PENDING,
    val authorizedAt: Long = 0L,
    val deniedAt: Long = 0L,
)
