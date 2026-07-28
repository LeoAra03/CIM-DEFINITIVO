/**
 * GCodeTranslator
 * @author CIM Team
 */
package com.industria.calidad
import android.util.Log

import android.graphics.Bitmap

object GCodeTranslator {
    fun translate(bitmap: Bitmap, scale: Float = 1f): List<String> =
        translateMask(bitmap.width, bitmap.height, scale) { x, y ->
            bitmap.getPixel(x, y) == android.graphics.Color.BLACK
        }

    /** Pure logic seam: usable from JVM tests without Camera/OpenCV/Bitmap runtime. */
    internal fun translateMask(
        width: Int,
        height: Int,
        scale: Float = 1f,
        isBlack: (x: Int, y: Int) -> Boolean
    ): List<String> {
        require(width > 0 && height > 0) { "Dimensiones inválidas" }
        require(scale > 0f) { "Escala inválida" }
        val commands = mutableListOf<String>()
        val blackPixels = mutableListOf<Pair<Int, Int>>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isBlack(x, y)) blackPixels.add(x to y)
            }
        }

        blackPixels.sortedBy { it.second }.forEachIndexed { index, (x, y) ->
            val xPos = x * scale
            val yPos = y * scale
            commands.add("G0 X${"%.2f".format(xPos)} Y${"%.2f".format(yPos)}")
            commands.add("G1 X${"%.2f".format(xPos)} Y${"%.2f".format(yPos)}")
            if (index % 16 == 0) commands.add("G1 F1000")
        }
        return commands.distinct()
    }
}

// FIX: Límite de colección (MAX=500)
private val MAX_COLLECTION_SIZE = 500
