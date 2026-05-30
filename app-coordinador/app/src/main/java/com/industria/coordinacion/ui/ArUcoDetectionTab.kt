package com.industria.coordinacion.ui

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.sistema.distribuido.network.IndustrialVisionAnalyzer
import com.sistema.distribuido.network.prefecto.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import android.util.Log
import org.opencv.core.Point

// 🎯 Estructura para almacenar un marcador detectado
data class DetectedArUco(
    val id: Int,
    val confidence: Float,
    val center: Pair<Float, Float>,
    val rotation: Float = 0f
)

@Composable
fun ArUcoDetectionTab(
    onArucoDetected: (DetectedArUco) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var detectedArucos by remember { mutableStateOf<List<DetectedArUco>>(emptyList()) }
    var isDetecting by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var selectedAruco by remember { mutableStateOf<DetectedArUco?>(null) }
    var fps by remember { mutableStateOf(0) }
    var confidenceThreshold by remember { mutableStateOf(0.5f) }
    var lastQrContent by remember { mutableStateOf("") }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkCameraPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                cameraPermissionGranted = checkCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        cameraPermissionGranted = checkCameraPermission()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!cameraPermissionGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IndustrialCard("Permiso Requerido", Icons.Default.PhotoCamera) {
                    Text("Se necesita acceso a la cámara para visión industrial.", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Zona de Cámara (60%)
                Box(Modifier.weight(0.6f).fillMaxWidth().background(Color.Black)) {
                    CameraPreviewWithVision(
                        isDetecting = isDetecting,
                        onArucoFound = { results ->
                            detectedArucos = results.map { 
                                DetectedArUco(it.id, 1.0f, it.center.x.toFloat() to it.center.y.toFloat()) 
                            }
                        },
                        onQrFound = { qr ->
                            lastQrContent = qr
                        },
                        onFpsUpdate = { fps = it }
                    )

                    // Informes en pantalla
                    Column(Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        Text("FPS: $fps", color = IndustrialTheme.Exito, fontSize = 10.sp)
                        if (lastQrContent.isNotEmpty()) {
                            Text("QR: ${lastQrContent.take(15)}...", color = IndustrialTheme.Primario, fontSize = 10.sp)
                        }
                    }
                }

                // Panel de Control (40%)
                Box(Modifier.weight(0.4f).fillMaxWidth().background(IndustrialTheme.Fondo).padding(12.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            IndustrialCard("Controles de Visión", Icons.Default.Settings) {
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    IndustrialActionButton(
                                        texto = if (isDetecting) "Pausar" else "Detectar",
                                        icono = if (isDetecting) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        modifier = Modifier.weight(1f),
                                        colorFondo = if (isDetecting) IndustrialTheme.Error else IndustrialTheme.Exito,
                                        onClick = { isDetecting = !isDetecting }
                                    )
                                    IndustrialActionButton(
                                        texto = "Limpiar",
                                        icono = Icons.Default.Delete,
                                        modifier = Modifier.weight(1f),
                                        colorFondo = IndustrialTheme.Advertencia,
                                        onClick = {
                                            detectedArucos = emptyList()
                                            lastQrContent = ""
                                            selectedAruco = null
                                        }
                                    )
                                }
                            }
                        }

                        if (detectedArucos.isNotEmpty()) {
                            items(detectedArucos) { aruco ->
                                ArucoMarkerCard(
                                    aruco = aruco,
                                    isSelected = selectedAruco?.id == aruco.id,
                                    onSelect = { selectedAruco = aruco }
                                )
                            }
                        }

                        selectedAruco?.let { aruco ->
                            item {
                                IndustrialCard("Acción sobre Marcador", Icons.Default.PrecisionManufacturing) {
                                    Text("ID: ArUco-${aruco.id}", color = Color.White)
                                    Text("Posición: ${aruco.center}", color = Color.Gray, fontSize = 11.sp)
                                    Spacer(Modifier.height(8.dp))
                                    IndustrialActionButton("Enviar al Robot", Icons.Default.Send) {
                                        onArucoDetected(aruco)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArucoMarkerCard(aruco: DetectedArUco, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.border(1.dp, if(isSelected) IndustrialTheme.Primario else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = IndustrialTheme.Tarjeta)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCode, null, tint = IndustrialTheme.Exito)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("ArUco #${aruco.id}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Detectado", color = IndustrialTheme.Exito, fontSize = 10.sp)
            }
        }
    }
}
