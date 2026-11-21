package com.industria.prefecto

import android.graphics.Bitmap

/**
 * Converts a binary mask into deterministic G-code.
 *
 * Contiguous runs of dark pixels share one G0/G1 pair. The output preserves
 * the input resolution and never depends on hash iteration order.
 */
fun Bitmap.toGCode(
    originX: Float = 0f,
    originY: Float = 0f,
    scale: Float = 1f,
): String {
    val output = StringBuilder()
    output.appendLine("G21")
    output.appendLine("G90")
    output.appendLine("M3 S1000")
    val darkPixels = IntArray(width * height)
    getPixels(darkPixels, 0, width, 0, 0, width, height)
    for (y in 0 until height) {
        var runStart = -1
        for (x in 0..width) {
            val dark = x < width && isDark(darkPixels[y * width + x])
            if (dark && runStart < 0) runStart = x
            if (!dark && runStart >= 0) {
                val x0 = originX + runStart * scale
                val x1 = originX + x * scale
                val y0 = originY + y * scale
                output.append("G0 X%.3f Y%.3f\n".format(x0, y0))
                output.append("G1 X%.3f Y%.3f\n".format(x1, y0))
                runStart = -1
            }
        }
    }
    output.appendLine("M5")
    output.appendLine("G0 X0.000 Y0.000")
    return output.toString()
}

private fun isDark(color: Int): Boolean {
    val red = color shr 16 and 0xFF
    val green = color shr 8 and 0xFF
    val blue = color and 0xFF
    return (red + green + blue) / 3 < 128
}
