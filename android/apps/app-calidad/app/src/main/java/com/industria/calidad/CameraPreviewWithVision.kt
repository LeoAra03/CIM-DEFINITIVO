/**
 * CameraPreviewWithVision (delegado)
 * Reutiliza el componente compartido de core-network para evitar
 * instancias duplicadas de cámara que provocaban congelamientos.
 */
package com.industria.calidad

import androidx.compose.runtime.Composable
import com.sistema.distribuido.network.ArucoDictionary
import com.sistema.distribuido.network.IndustrialVisionAnalyzer

@Composable
fun CameraPreviewWithVision(
    isDetecting: Boolean,
    visionMode: IndustrialVisionAnalyzer.VisionMode = IndustrialVisionAnalyzer.VisionMode.ARUCO,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArucoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onYoloFound: (List<IndustrialVisionAnalyzer.YoloResult>) -> Unit = {}
) {
    com.sistema.distribuido.network.prefecto.CameraPreviewWithVision(
        isDetecting = isDetecting,
        visionMode = visionMode,
        arucoDictionary = ArucoDictionary.DEFAULT,
        onArucoFound = onArucoFound,
        onQrFound = onQrFound,
        onYoloFound = onYoloFound
    )
}
