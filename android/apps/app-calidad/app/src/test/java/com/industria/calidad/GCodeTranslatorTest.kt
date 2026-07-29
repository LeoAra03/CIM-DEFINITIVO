package com.industria.calidad

import org.junit.Assert.assertTrue
import org.junit.Test

class GCodeTranslatorTest {
    @Test
    fun translatesMaskIntoGCodeCommands() {
        val commands = GCodeTranslator.translateMask(width = 8, height = 8) { x, y ->
            x == y || (x == 0 && y == 7)
        }

        assertTrue(commands.isNotEmpty())
        assertTrue(commands.any { it.startsWith("G0") })
        assertTrue(commands.any { it.startsWith("G1 F1000") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        GCodeTranslator.translateMask(width = 0, height = 1) { _, _ -> false }
    }
}