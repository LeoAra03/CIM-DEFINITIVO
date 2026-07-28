package com.industria.calidad

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sistema.distribuido.network.IndustrialVisionAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreviewWithVision(
    isDetecting: Boolean,
    visionMode: IndustrialVisionAnalyzer.VisionMode,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArUcoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onYoloFound: (List<IndustrialVisionAnalyzer.YoloResult>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val visionAnalyzer = remember { IndustrialVisionAnalyzer() }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                previewView = this
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier
    )

    LaunchedEffect(previewView, isDetecting) {
        previewView?.let { pv ->
            if (isDetecting) {
                startCamera(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = pv,
                    cameraExecutor = cameraExecutor,
                    visionAnalyzer = visionAnalyzer,
                    visionMode = visionMode,
                    onArucoFound = onArucoFound,
                    onQrFound = onQrFound,
                    onYoloFound = onYoloFound
                )
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraExecutor: ExecutorService,
    visionAnalyzer: IndustrialVisionAnalyzer,
    visionMode: IndustrialVisionAnalyzer.VisionMode,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArUcoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onYoloFound: (List<IndustrialVisionAnalyzer.YoloResult>) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        visionAnalyzer.analyzeImage(
                            imageProxy = imageProxy,
                            mode = visionMode,
                            onArucoResults = onArucoFound,
                            onQrResult = onQrFound,
                            onYoloResults = onYoloFound
                        )
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            Log.d("CameraPreview", "Cámara iniciada correctamente")

        } catch (exc: Exception) {
            Log.e("CameraPreview", "Error al iniciar cámara", exc)
        }
    }, ContextCompat.getMainExecutor(context))
}