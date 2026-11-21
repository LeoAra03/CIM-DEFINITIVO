/**
 * CameraPreviewWithVision (delegado)
 * Reuses the shared core-network component to avoid
 * instancias duplicadas de cámara que provocaban congelamientos.
 */
package com.industria.calidad

import androidx.compose.runtime.Composable
import com.industria.prefecto.ArucoDictionary
import com.industria.prefecto.IndustrialVisionAnalyzer

@Composable
fun CameraPreviewWithVision(
    isDetecting: Boolean,
    visionMode: IndustrialVisionAnalyzer.VisionMode = IndustrialVisionAnalyzer.VisionMode.ARUCO,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArucoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onYoloFound: (List<IndustrialVisionAnalyzer.YoloResult>) -> Unit = {}
) {
    com.industria.prefecto.CameraPreviewWithVision(
        isDetecting = isDetecting,
        visionMode = visionMode,
        arucoDictionary = ArucoDictionary.DEFAULT,
        onArucoFound = onArucoFound,
        onQrFound = onQrFound,
        onYoloFound = onYoloFound
    )
}
