package com.sistema.distribuido.network

import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Dictionary
import org.opencv.objdetect.Objdetect
import java.nio.ByteBuffer

/**
 * ANALIZADOR INDUSTRIAL: ArUco + QR
 * Procesa frames de CameraX en tiempo real.
 */
class IndustrialVisionAnalyzer(
    private val onArucoDetected: (List<ArucoResult>) -> Unit,
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    data class ArucoResult(val id: Int, val corners: Mat, val center: Point)

    init {
        if (!OpenCVLoader.initDebug()) {
            Log.e("IndustrialVision", "✗ No se pudo inicializar OpenCV")
        } else {
            Log.d("IndustrialVision", "✓ OpenCV inicializado correctamente")
        }
    }

    private val arucoDictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)
    private val arucoDetector = ArucoDetector(arucoDictionary)
    
    private val qrScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private var lastProcessTime = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < 200) { // ~5 FPS
            imageProxy.close()
            return
        }
        lastProcessTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // 1. Detección de QR (ML Kit)
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            qrScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onQrDetected(it) }
                    }
                }

            // 2. Detección de ArUco (OpenCV)
            try {
                // Convertir ImageProxy a Bitmap (usando el método nativo de CameraX 1.3+)
                val bitmap = imageProxy.toBitmap()
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                
                val gray = Mat()
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

                val corners = mutableListOf<Mat>()
                val ids = Mat()
                val rejected = mutableListOf<Mat>()
                arucoDetector.detectMarkers(gray, corners, ids, rejected)

                if (!ids.empty()) {
                    val results = mutableListOf<ArucoResult>()
                    for (i in 0 until ids.rows()) {
                        val idArray = DoubleArray(1)
                        ids.get(i, 0, idArray)
                        val id = idArray[0].toInt()
                        
                        val cornerMat = corners[i]
                        
                        // Calcular centro
                        var sumX = 0.0
                        var sumY = 0.0
                        // cornerMat es 1x4 CV_32FC2
                        for (j in 0 until 4) {
                            val ptArray = DoubleArray(2)
                            cornerMat.get(0, j, ptArray)
                            sumX += ptArray[0]
                            sumY += ptArray[1]
                        }
                        val center = Point(sumX / 4.0, sumY / 4.0)
                        
                        results.add(ArucoResult(id, cornerMat, center))
                    }
                    onArucoDetected(results)
                }

                mat.release()
                gray.release()
                ids.release()
                
            } catch (e: Exception) {
                Log.e("IndustrialVision", "Error OpenCV: ${e.message}")
            }
        }
        imageProxy.close()
    }

    companion object {
        /**
         * Genera una imagen ArUco real usando OpenCV
         * @param markerId ID del marcador (0-99 para DICT_4X4_50)
         * @param sizePixels Tamaño de la imagen en píxeles (recomendado 250-500)
         * @return Bitmap con el marcador ArUco generado
         */
        fun generateArucoMarker(markerId: Int, sizePixels: Int = 250): Bitmap? {
            return try {
                if (!OpenCVLoader.initDebug()) {
                    Log.e("ArucoGenerator", "OpenCV no inicializado")
                    return null
                }

                // Crear diccionario y generar marcador
                val dictionary = Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)
                val markerImage = Mat()
                
                // Validar que el ID esté dentro del rango válido para DICT_4X4_50 (0-49)
                val validId = if (markerId > 49) 49 else markerId
                
                Objdetect.generateImageMarker(dictionary, validId, sizePixels, markerImage, 1)

                // Convertir Mat a Bitmap
                val bitmap = Bitmap.createBitmap(sizePixels, sizePixels, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(markerImage, bitmap)
                
                markerImage.release()
                Log.d("ArucoGenerator", "✓ Marcador $validId generado ($sizePixels x $sizePixels)")
                
                bitmap
            } catch (e: Exception) {
                Log.e("ArucoGenerator", "Error generando ArUco: ${e.message}")
                null
            }
        }
    }
}
