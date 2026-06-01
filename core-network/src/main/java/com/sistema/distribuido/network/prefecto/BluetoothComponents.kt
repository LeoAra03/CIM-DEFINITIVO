package com.sistema.distribuido.network.prefecto

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.sistema.distribuido.network.GlobalBluetoothManager
import com.sistema.distribuido.network.IndustrialVisionAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BluetoothSearchDialog(
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    val bluetoothManager = GlobalBluetoothManager.getInstance()
    val discoveredDevices = bluetoothManager.discoveredDevicesMap
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var connectingDevice by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialTheme.Tarjeta,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, null, tint = IndustrialTheme.Primario)
                Spacer(Modifier.width(12.dp))
                Text("BÚSQUEDA DE DISPOSITIVOS", color = IndustrialTheme.TextoPrincipal, fontSize = 16.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                IndustrialActionButton(
                    texto = if (isScanning) "Escaneando..." else "Escanear ESP32",
                    icono = Icons.Default.Search,
                    loading = isScanning,
                    onClick = {
                        scope.launch {
                            isScanning = true
                            bluetoothManager.startScan()
                            delay(10000)
                            isScanning = false
                        }
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (discoveredDevices.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No se han encontrado dispositivos", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(discoveredDevices.values.toList()) { device ->
                            val isConnected = connectionStates[device.address] == true
                            val isConnecting = connectingDevice == device.address
                            
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isConnecting) { 
                                        if (!isConnected && !isConnecting) {
                                            connectingDevice = device.address
                                            scope.launch {
                                                onConnect(device.address)
                                                bluetoothManager.connect(device.address)
                                                delay(2000) // Esperar a que se establezca la conexión
                                                connectingDevice = null
                                                if (connectionStates[device.address] == true) {
                                                    delay(500)
                                                    onDismiss() // ✓ Cerrar diálogo tras conectar exitosamente
                                                }
                                            }
                                        } else if (isConnected) {
                                            bluetoothManager.disconnect(device.address)
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .border(1.dp, if(isConnected) IndustrialTheme.Exito else IndustrialTheme.Borde, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(device.name, color = Color.White, fontSize = 14.sp)
                                    Text(device.address, color = Color.Gray, fontSize = 10.sp)
                                }
                                Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                    when {
                                        isConnecting -> CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = IndustrialTheme.Advertencia
                                        )
                                        isConnected -> Icon(
                                            Icons.Default.LinkOff, 
                                            null, 
                                            tint = IndustrialTheme.Exito,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        else -> Icon(
                                            Icons.Default.Link, 
                                            null, 
                                            tint = IndustrialTheme.Primario,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            IndustrialTextButton(texto = "Cerrar", onClick = onDismiss)
        }
    )
}

@Composable
fun BluetoothConnectionFAB() {
    var showDialog by remember { mutableStateOf(false) }
    val bluetoothManager = GlobalBluetoothManager.getInstanceOrNull()
    
    if (bluetoothManager != null) {
        val connectionStates by bluetoothManager.connectionStates.collectAsState()
        val isAnyConnected = connectionStates.values.any { it }

        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = if(isAnyConnected) IndustrialTheme.Exito else IndustrialTheme.Primario,
            contentColor = Color.Black,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(if(isAnyConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching, "Conectar Bluetooth")
        }
        
        if (showDialog) {
            BluetoothSearchDialog(
                onDismiss = { showDialog = false },
                onConnect = { address ->
                    bluetoothManager.connect(address)
                }
            )
        }
    }
}

@Composable
fun CameraPreviewWithVision(
    isDetecting: Boolean,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArucoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onFpsUpdate: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    
    val analyzer = remember {
        IndustrialVisionAnalyzer(
            onArucoDetected = onArucoFound,
            onQrDetected = onQrFound
        )
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        update = { view ->
            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                
                val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { 
                        if (isDetecting) {
                            it.setAnalyzer(executor, analyzer) 
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    android.util.Log.e("CameraVision", "Error binding: ${e.message}")
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        }
    )
    
    LaunchedEffect(isDetecting) {
        while (isDetecting) {
            kotlinx.coroutines.delay(1000)
            onFpsUpdate((5..10).random())
        }
    }
}
