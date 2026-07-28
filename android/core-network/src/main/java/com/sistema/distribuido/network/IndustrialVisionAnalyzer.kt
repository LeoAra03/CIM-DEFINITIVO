// FIX: Constantes extraídas
package com.sistema.distribuido.network

import android.graphics.Bitmap
import android.graphics.Rect
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
import org.opencv.objdetect.Objdetect

/**
 * ANALIZADOR INDUSTRIAL: ArUco + QR
 * Procesa frames de CameraX en tiempo real.
 */
class IndustrialVisionAnalyzer(
    private val visionMode: VisionMode = VisionMode.ARUCO,
    private val arucoDictionary: ArucoDictionary = ArucoDictionary.DEFAULT,
    private val yoloDetector: YoloTfliteDetector? = null,
    private val onArucoDetected: (List<ArucoResult>) -> Unit,
    private val onQrDetected: (String) -> Unit,
    private val onYoloDetected: (List<YoloResult>) -> Unit = {}
) : ImageAnalysis.Analyzer {

    data class ArucoResult(val id: Int, val corners: Mat, val center: Point, val dictionary: ArucoDictionary)
    data class YoloResult(val label: String, val confidence: Double, val box: Rect)

    sealed class VisionMode {
        object ARUCO : VisionMode()
        object YOLO : VisionMode()
    }

    private val arucoDetector: ArucoDetector

    init {
        if (!OpenCVLoader.initDebug()) {
            Log.e("IndustrialVision", "✗ No se pudo inicializar OpenCV")
        } else {
            Log.d("IndustrialVision", "✓ OpenCV inicializado correctamente")
        }
        arucoDetector = ArucoDetector(Objdetect.getPredefinedDictionary(arucoDictionary.opencvConstant))
    }

    private val qrScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private var lastProcessTime = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < 200) {
            imageProxy.close()
            return
        }
        lastProcessTime = currentTime

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            qrScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onQrDetected(it) }
                    }
                }

            try {
                val bitmap = imageProxy.toBitmap()
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)

                if (visionMode == VisionMode.YOLO) {
                    val yoloResults = if (yoloDetector?.isReady() == true) {
                        val bitmap = imageProxy.toBitmap()
                        yoloDetector.detect(bitmap).map {
                            YoloResult(it.label, it.confidence.toDouble(), it.box)
                        }
                    } else {
                        detectYolo(mat)
                    }
                    if (yoloResults.isNotEmpty()) {
                        onYoloDetected(yoloResults)
                    }
                }

                if (visionMode == VisionMode.ARUCO) {
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
                            var sumX = 0.0
                            var sumY = 0.0
                            for (j in 0 until 4) {
                                val ptArray = DoubleArray(2)
                                cornerMat.get(0, j, ptArray)
                                sumX += ptArray[0]
                                sumY += ptArray[1]
                            }
                            val center = Point(sumX / 4.0, sumY / 4.0)
                            results.add(ArucoResult(id, cornerMat, center, arucoDictionary))
                        }
                        onArucoDetected(results)
                    }

                    gray.release()
                    ids.release()
                }

                mat.release()
            } catch (e: Exception) {
                Log.e("IndustrialVision", "Error OpenCV: ${e.message}")
            }
        }
        imageProxy.close()
    }

    private fun detectYolo(image: Mat): List<YoloResult> {
        val results = mutableListOf<YoloResult>()
        val gray = Mat()
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < 1800) {
                contour.release()
                continue
            }

            val rect = Imgproc.boundingRect(contour)
            val confidence = ((area / 60000.0).coerceAtMost(1.0) * 100.0).coerceIn(0.0, 100.0) / 100.0
            results.add(
                YoloResult(
                    label = "OBJ",
                    confidence = confidence,
                    box = Rect(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height)
                )
            )
            contour.release()
        }

        edges.release()
        gray.release()
        return results
    }

    companion object {
        private var openCvReady = false

        private fun ensureOpenCv(): Boolean {
            if (!openCvReady) {
                openCvReady = OpenCVLoader.initDebug()
            }
            return openCvReady
        }

        /**
         * Genera un marcador ArUco con diccionario configurable.
         * @param markerId ID del marcador (validado según diccionario)
         * @param sizePixels Tamaño en píxeles (recomendado 250-500)
         * @param dictionary Diccionario ArUco (default DICT_4X4_50)
         */
        fun generateArucoMarker(
            markerId: Int,
            sizePixels: Int = 250,
            dictionary: ArucoDictionary = ArucoDictionary.DEFAULT
        ): Bitmap? {
            return try {
                if (!ensureOpenCv()) {
                    Log.e("ArucoGenerator", "OpenCV no inicializado")
                    return null
                }

                val validId = dictionary.clampId(markerId)
                val pixelSize = sizePixels.coerceIn(100, 2000)
                val dict = Objdetect.getPredefinedDictionary(dictionary.opencvConstant)
                val markerImage = Mat()

                Objdetect.generateImageMarker(dict, validId, pixelSize, markerImage, 1)

                val bitmap = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(markerImage, bitmap)

                markerImage.release()
                Log.d("ArucoGenerator", "✓ Marcador $validId (${dictionary.label}) ${pixelSize}x${pixelSize}px")

                bitmap
            } catch (e: Exception) {
                Log.e("ArucoGenerator", "Error generando ArUco: ${e.message}")
                null
            }
        }

        /** Genera marcador a partir de tamaño físico en mm. */
        fun generateArucoMarkerMm(
            markerId: Int,
            sizeMm: Int,
            dictionary: ArucoDictionary = ArucoDictionary.DEFAULT,
            dpi: Int = 96
        ): Bitmap? {
            val pixels = dictionary.mmToPixels(sizeMm, dpi)
            return generateArucoMarker(markerId, pixels, dictionary)
        }

        /** Convierte Bitmap ArUco a PNG base64 para envío al láser/coordinador. */
        fun bitmapToPngBase64(bitmap: Bitmap): String {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        }
    }
}
