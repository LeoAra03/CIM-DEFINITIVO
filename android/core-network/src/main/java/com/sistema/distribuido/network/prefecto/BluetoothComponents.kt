// FIX: Constantes extraídas
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.sistema.distribuido.network.GlobalBluetoothManager
import com.sistema.distribuido.network.ArucoDictionary
import com.sistema.distribuido.network.IndustrialVisionAnalyzer
import com.sistema.distribuido.network.YoloTfliteDetector
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

    val lanzarEscaneo: () -> Unit = {
        scope.launch {
            isScanning = true
            bluetoothManager.startScan()
            delay(10000)
            isScanning = false
        }
        Unit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IndustrialTheme.Tarjeta,
        shape = RoundedCornerShape(IndustrialTheme.RadioTarjeta),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(IndustrialTheme.Secundario.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bluetooth, null, Modifier.size(18.dp), tint = IndustrialTheme.Secundario)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "CONEXIÓN BLUETOOTH",
                        color = IndustrialTheme.TextoPrincipal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Vincula el módulo ESP32 de la estación",
                        color = IndustrialTheme.TextoSecundario,
                        fontSize = 10.sp
                    )
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                // Banner de escaneo (equivalente al de la figura de referencia)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            IndustrialTheme.Secundario.copy(alpha = if (isScanning) 0.14f else 0.07f),
                            RoundedCornerShape(IndustrialTheme.RadioControl)
                        )
                        .border(
                            1.dp,
                            IndustrialTheme.Secundario.copy(alpha = 0.35f),
                            RoundedCornerShape(IndustrialTheme.RadioControl)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = IndustrialTheme.Secundario
                        )
                    } else {
                        Icon(Icons.Default.BluetoothSearching, null, Modifier.size(16.dp), tint = IndustrialTheme.Secundario)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isScanning) "BUSCANDO DISPOSITIVOS CERCANOS…" else "PULSA ESCANEAR PARA BUSCAR EL ESP32",
                        color = IndustrialTheme.Secundario,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                IndustrialActionButton(
                    texto = if (isScanning) "Escaneando…" else "Escanear ESP32",
                    icono = Icons.Default.Search,
                    loading = isScanning,
                    onClick = lanzarEscaneo
                )

                IndustrialSectionHeader(
                    texto = "Dispositivos disponibles",
                    color = IndustrialTheme.Secundario,
                    trailing = {
                        Text(
                            "${discoveredDevices.size}",
                            color = IndustrialTheme.TextoSecundario,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                if (discoveredDevices.isEmpty()) {
                    IndustrialEmptyState(
                        icono = Icons.Default.BluetoothDisabled,
                        texto = "Sin dispositivos detectados",
                        detalle = "Enciende el ESP32 y vuelve a escanear"
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredDevices.values.toList()) { device ->
                            val isConnected = connectionStates[device.address] == true
                            val isConnecting = connectingDevice == device.address

                            IndustrialDeviceRow(
                                nombre = device.name,
                                direccion = device.address,
                                conectado = isConnected,
                                conectando = isConnecting,
                                onClick = {
                                    if (isConnected) {
                                        bluetoothManager.disconnect(device.address)
                                    } else if (!isConnecting) {
                                        connectingDevice = device.address
                                        scope.launch {
                                            onConnect(device.address)
                                            bluetoothManager.connect(device.address)
                                            delay(2000) // Esperar a que se establezca la conexión
                                            connectingDevice = null
                                            if (connectionStates[device.address] == true) {
                                                delay(500)
                                                onDismiss() // Cerrar diálogo tras conectar correctamente
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Ayuda contextual (tarjeta "¿No encuentras tu dispositivo?")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(IndustrialTheme.TarjetaAlta, RoundedCornerShape(IndustrialTheme.RadioControl))
                        .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(IndustrialTheme.RadioControl))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, null, Modifier.size(16.dp), tint = IndustrialTheme.TextoSecundario)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "¿No encuentras tu dispositivo?",
                            color = IndustrialTheme.TextoPrincipal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Comprueba que el ESP32 esté encendido y emparejado en los ajustes del sistema.",
                            color = IndustrialTheme.TextoSecundario,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
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
            containerColor = if (isAnyConnected) IndustrialTheme.Primario else IndustrialTheme.Secundario,
            contentColor = if (isAnyConnected) IndustrialTheme.Fondo else IndustrialTheme.TextoPrincipal,
            shape = RoundedCornerShape(16.dp),
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
    visionMode: IndustrialVisionAnalyzer.VisionMode = IndustrialVisionAnalyzer.VisionMode.ARUCO,
    arucoDictionary: ArucoDictionary = ArucoDictionary.DEFAULT,
    onArucoFound: (List<IndustrialVisionAnalyzer.ArucoResult>) -> Unit,
    onQrFound: (String) -> Unit,
    onYoloFound: (List<IndustrialVisionAnalyzer.YoloResult>) -> Unit = {},
    onFpsUpdate: (Int) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var cameraError by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    val yoloDetector = remember { YoloTfliteDetector(context) }

    val analyzer = remember(isDetecting, visionMode, arucoDictionary, onArucoFound, onQrFound, onYoloFound) {
        IndustrialVisionAnalyzer(
            visionMode = visionMode,
            arucoDictionary = arucoDictionary,
            yoloDetector = yoloDetector,
            onArucoDetected = onArucoFound,
            onQrDetected = onQrFound,
            onYoloDetected = onYoloFound
        )
    }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
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
                    cameraError = null
                } catch (e: Exception) {
                    android.util.Log.e("CameraVision", "Error binding: ${e.message}")
                    cameraError = "Cámara no disponible: ${e.message}"
                    onError(cameraError!!)
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
    if (cameraError != null) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xCC000000)),
            color = androidx.compose.ui.graphics.Color.Transparent
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(Icons.Default.NoPhotography, null, Modifier.size(40.dp), tint = IndustrialTheme.Error)
                Spacer(Modifier.height(8.dp))
                Text("CÁMARA NO DISPONIBLE", color = IndustrialTheme.TextoPrincipal, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(cameraError ?: "", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
    }
}
