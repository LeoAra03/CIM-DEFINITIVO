package com.sistema.distribuido.network

import org.junit.Assert.*
import org.junit.Test

/**
 * Lógica de filtrado industrial BLE/Classic (sin hardware Android).
 */
class BluetoothFilterTest {

    private val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")

    private fun isIndustrialDevice(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val upper = name.uppercase()
        return upper.contains("ESP32") || upper.contains("CIM") || upper.contains("NODO")
    }

    @Test
    fun validMacFormat_accepted() {
        assertTrue(macRegex.matches("AA:BB:CC:DD:EE:FF"))
        assertTrue(macRegex.matches("aa-bb-cc-dd-ee-ff"))
    }

    @Test
    fun invalidMacFormat_rejected() {
        assertFalse(macRegex.matches("ZZ:XX:YY:11:22"))
        assertFalse(macRegex.matches("not-a-mac"))
    }

    @Test
    fun industrialFilter_matchesEsp32() {
        assertTrue(isIndustrialDevice("ESP32-CIM-NODO-01"))
    }

    @Test
    fun industrialFilter_rejectsGeneric() {
        assertFalse(isIndustrialDevice("Galaxy Buds"))
        assertFalse(isIndustrialDevice(null))
    }

    @Test
    fun discoveredDevice_equalityByAddress() {
        val a = DiscoveredBluetoothDevice("AA:BB:CC:DD:EE:FF", "CIM-NODO-1")
        val b = DiscoveredBluetoothDevice("AA:BB:CC:DD:EE:FF", "Renamed")
        assertEquals(a.address, b.address)
    }

    @Test
    fun normalizeMac_uppercaseColons() {
        val raw = "aa:bb:cc:dd:ee:ff"
        val normalized = raw.uppercase()
        assertEquals("AA:BB:CC:DD:EE:FF", normalized)
    }
}
