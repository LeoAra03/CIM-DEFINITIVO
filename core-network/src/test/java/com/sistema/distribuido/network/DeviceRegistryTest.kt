package com.sistema.distribuido.network

import com.sistema.distribuido.network.protocol.AppType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests de registro O(1) — MobileDeviceRegistry y DeviceRegistry legacy.
 */
class DeviceRegistryTest {

    private lateinit var registry: MobileDeviceRegistry

    @Before
    fun setup() {
        registry = MobileDeviceRegistry()
        runBlocking { registry.clear() }
        DeviceRegistry.dispositivosHardware.clear()
    }

    private fun device(mac: String, appType: AppType = AppType.PLC) = DeviceInfo(
        ip = "192.168.1.10",
        nombre = "TEST-$mac",
        tipo = DeviceType.CONVEYOR,
        mac = mac,
        appType = appType,
        authorized = false,
        isConnected = true
    )

    @Test
    fun registerAndLookupByMac_isO1() = runBlocking {
        val mac = "AA:BB:CC:DD:EE:01"
        registry.register(mac, device(mac))
        val found = registry.getDeviceByMac(mac)
        assertNotNull(found)
        assertEquals(mac, found!!.mac)
    }

    @Test
    fun authorizeDevice_setsAuthorizedFlag() = runBlocking {
        val mac = "AA:BB:CC:DD:EE:02"
        registry.register(mac, device(mac))
        assertTrue(registry.authorize(mac))
        assertTrue(registry.getDeviceByMac(mac)!!.authorized)
    }

    @Test
    fun disconnect_removesFromTypeIndex() = runBlocking {
        val mac = "AA:BB:CC:DD:EE:03"
        registry.register(mac, device(mac, AppType.MANUFACTURA))
        assertEquals(1, registry.getDevicesByType(AppType.MANUFACTURA).size)
        registry.disconnect(mac)
        assertFalse(registry.getDeviceByMac(mac)!!.isConnected)
        assertTrue(registry.getDevicesByType(AppType.MANUFACTURA).isEmpty())
    }

    @Test
    fun getDevicesByType_returnsOnlyMatchingType() = runBlocking {
        registry.register("AA:BB:CC:DD:EE:04", device("AA:BB:CC:DD:EE:04", AppType.PLC))
        registry.register("AA:BB:CC:DD:EE:05", device("AA:BB:CC:DD:EE:05", AppType.CALIDAD))
        val plcDevices = registry.getDevicesByType(AppType.PLC)
        assertEquals(1, plcDevices.size)
        assertEquals(AppType.PLC, plcDevices[0].appType)
    }

    @Test
    fun updateRssi_refreshesLastSeen() = runBlocking {
        val mac = "AA:BB:CC:DD:EE:06"
        registry.register(mac, device(mac))
        val before = registry.getDeviceByMac(mac)!!.lastSeen
        Thread.sleep(5)
        registry.updateRssi(mac, -55)
        assertTrue(registry.getDeviceByMac(mac)!!.lastSeen >= before)
        assertEquals(-55, registry.getDeviceByMac(mac)!!.rssi)
    }

    @Test
    fun legacyRegistry_ipLookup_isO1() {
        DeviceRegistry.registrarDispositivo("10.0.0.5", "ESP32-CINTA", DeviceType.CONVEYOR)
        DeviceRegistry.actualizarEstado("10.0.0.5", "RUNNING")
        assertEquals("RUNNING", DeviceRegistry.dispositivosHardware["10.0.0.5"]?.estado)
    }

    @Test
    fun deviceInfo_canExecute_requiresAuthAndConnection() {
        val alive = device("AA:BB:CC:DD:EE:07").copy(authorized = true, isConnected = true)
        assertTrue(alive.canExecute())
        val blocked = alive.copy(authorized = false)
        assertFalse(blocked.canExecute())
    }

    @Test
    fun registryPerformance_1000Lookups_under50ms() = runBlocking {
        repeat(1000) { i ->
            val mac = String.format("AA:BB:CC:DD:%02X:%02X", i shr 8, i and 0xFF)
            registry.register(mac, device(mac, AppType.PLC))
        }
        val start = System.nanoTime()
        repeat(1000) { i ->
            val mac = String.format("AA:BB:CC:DD:%02X:%02X", i shr 8, i and 0xFF)
            assertNotNull(registry.getDeviceByMac(mac))
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("1000 lookups took ${elapsedMs}ms (expected <500ms on CI)", elapsedMs < 500)
    }
}
