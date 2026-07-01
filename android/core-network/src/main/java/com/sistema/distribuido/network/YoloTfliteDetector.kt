package com.sistema.distribuido.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Detector YOLOv8-tiny cuantizado (.tflite) para visión en borde.
 * Colocar modelo en assets: yolov8n-int8.tflite
 * Si no existe, retorna lista vacía (IndustrialVisionAnalyzer usa fallback OpenCV).
 */
class YoloTfliteDetector(context: Context) {

    data class Detection(val label: String, val confidence: Float, val box: Rect)

    private var interpreter: Interpreter? = null
    private val inputSize = 320
    private val labels = listOf("pieza", "pallet", "defecto", "aruco_obj")

    init {
        try {
            val model = loadModelFile(context, MODEL_ASSET)
            interpreter = Interpreter(model, Interpreter.Options().apply { numThreads = 4 })
            Log.i(TAG, "✓ YOLO TFLite cargado: $MODEL_ASSET")
        } catch (e: Exception) {
            Log.w(TAG, "YOLO TFLite no disponible (${e.message}) — usar fallback OpenCV")
        }
    }

    fun isReady(): Boolean = interpreter != null

    fun detect(bitmap: Bitmap, threshold: Float = 0.45f): List<Detection> {
        val model = interpreter ?: return emptyList()
        val input = preprocess(bitmap)
        val output = Array(1) { Array(84) { FloatArray(8400) } } // YOLOv8 export típico
        try {
            model.run(input, output)
            return parseOutput(output[0], bitmap.width, bitmap.height, threshold)
        } catch (e: Exception) {
            Log.e(TAG, "Inferencia YOLO fallida: ${e.message}")
            return emptyList()
        }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
        if (scaled != bitmap) scaled.recycle()
        buffer.rewind()
        return buffer
    }

    private fun parseOutput(
        tensor: Array<FloatArray>,
        imgW: Int,
        imgH: Int,
        threshold: Float
    ): List<Detection> {
        val results = mutableListOf<Detection>()
        val numAnchors = tensor[0].size
        for (i in 0 until numAnchors) {
            var maxScore = 0f
            var maxClass = 0
            for (c in 4 until tensor.size) {
                val score = tensor[c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClass = c - 4
                }
            }
            if (maxScore < threshold) continue
            val cx = tensor[0][i]
            val cy = tensor[1][i]
            val w = tensor[2][i]
            val h = tensor[3][i]
            val left = ((cx - w / 2) * imgW / inputSize).toInt().coerceIn(0, imgW)
            val top = ((cy - h / 2) * imgH / inputSize).toInt().coerceIn(0, imgH)
            val right = ((cx + w / 2) * imgW / inputSize).toInt().coerceIn(0, imgW)
            val bottom = ((cy + h / 2) * imgH / inputSize).toInt().coerceIn(0, imgH)
            val label = labels.getOrElse(maxClass) { "obj_$maxClass" }
            results.add(Detection(label, maxScore, Rect(left, top, right, bottom)))
        }
        return results.take(20)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val TAG = "YoloTfliteDetector"
        const val MODEL_ASSET = "yolov8n-int8.tflite"

        private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
            context.assets.openFd(assetName).use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    return input.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        }
    }
}
