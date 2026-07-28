// FIX Lote 9: Edge case handling
/**
 * GCodeTranslatorTest
 * @author CIM Team
 */
package com.industria.calidad
import android.util.Log
import org.junit.Before
import org.junit.After

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Before
import org.junit.After

class GCodeTranslatorTest {
    @Test
    fun translatesBitmapEdgesIntoGCodeCommands() {
        val bitmap = ArUcoGenerator.buildBitmap(size = 8, markerId = 7)
        val commands = GCodeTranslator.translate(bitmap)

        assertEquals(true, commands.isNotEmpty())
        assertEquals(true, commands.any { it.startsWith("G0") || it.startsWith("G1") })
    }
}
