package com.sistema.distribuido.network

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

/**
 * Procesador de imágenes G-code para láser
 * Port de integrated_panel.py laser panel + chev.me/arucogen style
 * Convierte Bitmap (ArUco o imagen subida) a G-code raster para Wemos D1 ESP32 R32
 */
object LaserImageProcessor {

    data class LaserParams(
        val powerPercent: Int = 80, // 0-100%
        val speedMmMin: Int = 1200,
        val threshold: Int = 128, // 0-255
        val pixelSizeMm: Float = 0.1f, // cada pixel = 0.1mm (10px/mm)
        val maxWidthPx: Int = 400, // redimensionar si más grande
        val invert: Boolean = false,
        val dithering: Boolean = false
    )

    /**
     * Redimensiona bitmap manteniendo aspecto si excede maxWidth
     */
    fun resizeIfNeeded(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width.toFloat()
        val newH = (src.height * ratio).toInt()
        return Bitmap.createScaledBitmap(src, maxWidth, newH, true)
    }

    /**
     * Convierte Bitmap ArUco o imagen a G-code raster simple
     * Algoritmo: escanea filas, detecta segmentos negros contiguos, genera G0 + M3 + G1
     * Similar a _start_laser_sim de integrated_panel.py
     */
    fun bitmapToGcode(bitmap: Bitmap, params: LaserParams = LaserParams()): String {
        val bmp = resizeIfNeeded(bitmap, params.maxWidthPx)
        val w = bmp.width
        val h = bmp.height

        val sb = StringBuilder()
        sb.appendLine("; ArUco / Image Laser G-code")
        sb.appendLine("; Generated ${java.time.LocalDateTime.now()} by CIM Manufactura v6.0")
        sb.appendLine("; Based on integrated_panel.py + chev.me/arucogen/ port")
        sb.appendLine("; bestMH.pt compatible - YOLO detects product")
        sb.appendLine("; Power ${params.powerPercent}% Speed ${params.speedMmMin}mm/min Threshold ${params.threshold}")
        sb.appendLine("; Size ${w}x${h}px @ ${params.pixelSizeMm}mm/px = ${w * params.pixelSizeMm}mm x ${h * params.pixelSizeMm}mm")
        sb.appendLine("G21 ; mm")
        sb.appendLine("G90 ; absolute positioning")
        sb.appendLine("G28 ; home all")
        sb.appendLine("M3 S0 ; laser off")
        sb.appendLine("")

        var laserOn = false
        var totalSegments = 0

        // Skip every 2px for speed, similar to web version
        for (y in 0 until h step 2) {
            var rowHasBlack = false
            for (x in 0 until w) {
                val pixel = bmp.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val isBlack = if (params.invert) gray > params.threshold else gray < params.threshold
                if (isBlack) {
                    rowHasBlack = true
                    break
                }
            }
            if (!rowHasBlack) continue

            val yMm = y * params.pixelSizeMm
            sb.appendLine("G0 Y${"%.3f".format(yMm)} ; move to row $y")

            var xStart = -1
            for (x in 0 until w) {
                val pixel = bmp.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                val isBlack = if (params.invert) gray > params.threshold else gray < params.threshold

                if (isBlack && xStart == -1) {
                    xStart = x
                }
                if ((!isBlack || x == w - 1) && xStart != -1) {
                    var xEnd = if (isBlack) x else x - 1
                    if (xEnd >= xStart) {
                        val xStartMm = xStart * params.pixelSizeMm
                        val xEndMm = xEnd * params.pixelSizeMm
                        if (xEndMm - xStartMm >= 0.01f) {
                            sb.appendLine("G0 X${"%.3f".format(xStartMm)} ; start segment")
                            val sPower = (params.powerPercent * 10).coerceIn(0, 1000)
                            sb.appendLine("M3 S$sPower ; laser on")
                            sb.appendLine("G1 X${"%.3f".format(xEndMm)} F${params.speedMmMin} ; cut")
                            sb.appendLine("M3 S0 ; laser off")
                            totalSegments++
                        }
                    }
                    xStart = -1
                }
            }
        }

        sb.appendLine("")
        sb.appendLine("; Stats: $totalSegments segments")
        sb.appendLine("M5 ; laser off")
        sb.appendLine("G0 X0 Y0 ; return home")
        sb.appendLine("; END")

        Log.d("LaserImageProcessor", "[OK] G-code generado: $totalSegments segmentos, ${sb.length} chars")
        return sb.toString()
    }

    /**
     * Convierte imagen subida (cualquier formato) a G-code
     * Aplica grayscale + threshold + dithering opcional
     */
    fun imageToGcode(bitmap: Bitmap, params: LaserParams = LaserParams()): String {
        // Si dithering activado, aplicar Floyd-Steinberg simple
        val processed = if (params.dithering) {
            applyFloydSteinbergDithering(bitmap, params.threshold)
        } else {
            bitmap
        }
        return bitmapToGcode(processed, params)
    }

    private fun applyFloydSteinbergDithering(src: Bitmap, threshold: Int): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
        val gray = Array(h) { FloatArray(w) }

        // Convert to grayscale float
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = src.getPixel(x, y)
                gray[y][x] = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3f
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val old = gray[y][x]
                val new = if (old < threshold) 0f else 255f
                bmp.setPixel(x, y, if (new == 0f) Color.BLACK else Color.WHITE)
                val err = old - new
                if (x + 1 < w) gray[y][x + 1] += err * 7 / 16
                if (y + 1 < h) {
                    if (x > 0) gray[y + 1][x - 1] += err * 3 / 16
                    gray[y + 1][x] += err * 5 / 16
                    if (x + 1 < w) gray[y + 1][x + 1] += err * 1 / 16
                }
            }
        }
        return bmp
    }

    /**
     * Valida G-code generado (seguridad)
     */
    fun validateGcode(gcode: String, maxLines: Int = 10000, maxSizeBytes: Int = 500 * 1024): Boolean {
        if (gcode.isBlank()) return false
        if (gcode.length > maxSizeBytes) return false
        val lines = gcode.lines()
        if (lines.size > maxLines) return false
        // Solo comandos permitidos
        val allowed = setOf("G0", "G1", "G21", "G28", "G90", "M3", "M5")
        return lines.all { line ->
            val trimmed = line.trim()
            trimmed.isEmpty() || trimmed.startsWith(";") || allowed.any { trimmed.startsWith(it) }
        }
    }
}
