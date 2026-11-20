package com.industria.prefecto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizationManagerTest {

    private class FakePermissions : PermissionManager {
        private val blocked = mutableSetOf<String>()

        override fun isBlocked(mac: String): Boolean = mac in blocked

        override fun block(mac: String) {
            blocked += mac
        }

        override fun unblock(mac: String) {
            blocked -= mac
        }

        override fun getBlockedDevices(): Set<String> = blocked.toSet()
    }

    @Test
    fun unauthorizedDeviceCannotSend() {
        val permissions = FakePermissions()
        val manager = AuthorizationManager(permissions)
        manager.observe("AA:BB:CC:DD:EE:FF", "PLC")
        assertFalse(manager.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun validateThenDeny() {
        val permissions = FakePermissions()
        val manager = AuthorizationManager(permissions)
        manager.observe("AA:BB:CC:DD:EE:FF", "PLC")
        manager.validate("AA:BB:CC:DD:EE:FF")
        assertTrue(manager.isAuthorized("AA:BB:CC:DD:EE:FF"))
        manager.deny("AA:BB:CC:DD:EE:FF")
        assertFalse(manager.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun denyThenValidate() {
        val permissions = FakePermissions()
        val manager = AuthorizationManager(permissions)
        manager.observe("AA:BB:CC:DD:EE:FF", "PLC")
        manager.deny("AA:BB:CC:DD:EE:FF")
        manager.validate("AA:BB:CC:DD:EE:FF")
        assertTrue(manager.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun blockedDeviceIsNotObservable() {
        val permissions = FakePermissions()
        val manager = AuthorizationManager(permissions)
        manager.observe("AA:BB:CC:DD:EE:FF", "PLC")
        permissions.block("AA:BB:CC:DD:EE:FF")
        manager.observe("AA:BB:CC:DD:EE:FF", "PLC")
        assertFalse(manager.isAuthorized("AA:BB:CC:DD:EE:FF"))
    }
}
