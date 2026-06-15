package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.CimProtocol
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthorizationManagerTest {

    private val mac = "AA:BB:CC:DD:EE:FF"

    @Before
    fun setup() {
        AuthorizationManager.revoke(mac)
    }

    @Test
    fun testDefaultAuthorizationIsPending() {
        assertEquals(CimProtocol.AUTH_PENDING, AuthorizationManager.getAuthorizationState(mac))
        assertFalse(AuthorizationManager.isAuthorized(mac))
    }

    @Test
    fun testAuthorizeAndDenyTransitions() {
        AuthorizationManager.authorize(mac)
        assertEquals(CimProtocol.AUTH_AUTHORIZED, AuthorizationManager.getAuthorizationState(mac))
        assertTrue(AuthorizationManager.isAuthorized(mac))
        assertTrue(AuthorizationManager.canSendCommand(mac))

        AuthorizationManager.deny(mac)
        assertEquals(CimProtocol.AUTH_BLOCKED, AuthorizationManager.getAuthorizationState(mac))
        assertFalse(AuthorizationManager.isAuthorized(mac))
        assertFalse(AuthorizationManager.canSendCommand(mac))
    }

    @Test
    fun testRevokeRemovesState() {
        AuthorizationManager.authorize(mac)
        AuthorizationManager.revoke(mac)
        assertEquals(CimProtocol.AUTH_PENDING, AuthorizationManager.getAuthorizationState(mac))
        assertFalse(AuthorizationManager.isAuthorized(mac))
    }
}
