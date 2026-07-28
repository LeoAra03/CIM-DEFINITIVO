package com.industria.calidad
import android.util.Log

import org.junit.Assert.assertEquals
import org.junit.Test

class GCodeTranslatorTest {
    @Test
    fun translatesBitmapEdgesIntoGCodeCommands() {
        val bitmap = ArUcoGenerator.buildBitmap(size = 8, markerId = 7)
        val commands = GCodeTranslator.translate(bitmap)

        assertEquals(true, commands.isNotEmpty())
        assertEquals(true, commands.any { it.startsWith("G0") || it.startsWith("G1") })
    }
}
