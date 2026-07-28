// FIX Lote 9: Edge case handling
/**
 * GCodeTranslator
 * @author CIM Team
 */
package com.industria.calidad
import android.util.Log

import android.graphics.Bitmap

object GCodeTranslator {
    fun translate(bitmap: Bitmap, scale: Float = 1f): List<String> {
        val commands = mutableListOf<String>()
        val width = bitmap.width
        val height = bitmap.height
        val blackPixels = mutableListOf<Pair<Int, Int>>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.BLACK == pixel) {
                    blackPixels.add(x to y)
                }
            }
        }

        blackPixels.sortedBy { it.second }.forEachIndexed { index, (x, y) ->
            val xPos = x * scale
            val yPos = y * scale
            commands.add("G0 X${"%.2f".format(xPos)} Y${"%.2f".format(yPos)}")
            commands.add("G1 X${"%.2f".format(xPos)} Y${"%.2f".format(yPos)}")
            if (index % 16 == 0) {
                commands.add("G1 F1000")
            }
        }
        return commands.distinct()
    }
}

// FIX: Límite de colección (MAX=500)
private val MAX_COLLECTION_SIZE = 500
