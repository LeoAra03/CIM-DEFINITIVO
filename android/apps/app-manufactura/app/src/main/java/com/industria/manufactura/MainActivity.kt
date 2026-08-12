// FIX: Constantes extraídas
package com.industria.manufactura

import android.util.Log

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import android.util.Base64
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.sistema.distribuido.network.*
import com.sistema.distribuido.network.prefecto.*
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimProtocol
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var commCoordinator: CommunicationCoordinator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IndustrialErrorManager.install(this) {}
        AppIdentifier.init(this, AppType.MANUFACTURA)
        GlobalPermissionManager.init(this)
        GlobalBluetoothManager.init(this)
        enableEdgeToEdge()
        setContent {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
            LaunchedEffect(Unit) {
                val p = mutableListOf(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    p.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                } else {
                    p.addAll(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                launcher.launch(p.toTypedArray())
            }
            ManufacturaApp(commCoordinator)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManufacturaApp(commCoordinator: CommunicationCoordinator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    val bt = GlobalBluetoothManager.getInstance()
    val connectionStates by bt.connectionStates.collectAsState()
    val isConnectedBt by remember { derivedStateOf { connectionStates.values.any { it } } }

    var isConnectedNet by remember { mutableStateOf(false) }
    var authorizationState by remember { mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED) }
    val isAuthorized by remember { derivedStateOf { authorizationState == CimProtocol.AUTH_STATE_VALIDATED } }
    var independentMode by remember { mutableStateOf(false) }
    var ipCoordinator by remember { mutableStateOf("192.168.1.100") }
    val discoveredHubIp = rememberHubIp(context)
    LaunchedEffect(discoveredHubIp.value) {
        discoveredHubIp.value?.let { ip ->
            if (ip != ipCoordinator) ipCoordinator = ip
        }
    }
    var selectedTab by remember { mutableStateOf(0) }
    var laserPower by remember { mutableStateOf("80") }
    var laserSpeed by remember { mutableStateOf("1200") }
    val pendingArucoGenerate = remember { mutableStateOf<String?>(null) }
    val isOperationalReady by remember {
        derivedStateOf { isConnectedBt && (isAuthorized || independentMode) }
    }
    var lastDetectedArucoId by remember { mutableStateOf<Int?>(null) }
    // Anti-softlock: retroceder entre pestañas en vez de cerrar la app
    androidx.activity.compose.BackHandler(enabled = selectedTab > 0) {
        selectedTab -= 1
    }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
    }

    fun sendAuthorizedHardwareCommand(command: String, logText: String) {
        if (!isAuthorized && !independentMode) {
            addLog("✗ No autorizado - activar modo autónomo o esperar VALIDADO por coordinador")
            return
        }
        bt.send(command, requireAuthorization = !independentMode, authorized = isAuthorized)
        if (isAuthorized) {
            scope.launch {
                commCoordinator.routeCommand(AppIdentifier.getInstance().deviceMac, command)
            }
        }
        addLog(if (independentMode) "[AUTÓNOMO] $logText" else logText)
    }

    fun handleIncomingCoordinatorCommand(command: String) {
        addLog("← COORDINADOR: $command")
        when {
            command.startsWith("ARUCO_GENERATE:") -> {
                val payload = command.removePrefix("ARUCO_GENERATE:")
                pendingArucoGenerate.value = payload
                addLog("✓ Solicitud ArUco recibida: $payload")
            }
            command.startsWith("LASER_LOAD:") -> {
                val parts = command.split(":", limit = 3)
                if (parts.size == 3) {
                    val rawFilename = parts[1].ifBlank { "archivo.gcode" }
                    val base64 = parts[2]
                    try {
                        val safeName = IndustrialErrorManager.sanitizeFileName(rawFilename)
                        val bytes = Base64.decode(base64, Base64.NO_WRAP)
                        IndustrialErrorManager.validateGcodeSize(bytes)
                        context.openFileOutput(safeName, Context.MODE_PRIVATE).use { output ->
                            output.write(bytes)
                        }
                        addLog("✓ G-code recibido: $safeName (${bytes.size} bytes)")
                    } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                        addLog("✗ Error guardando G-code: ${e.message ?: "desconocido"}")
                    }
                } else {
                    addLog("⚠ Formato LASER_LOAD inválido")
                }
            }
            command.startsWith("GCODE_LOAD;") -> {
                val parts = command.split(";", limit = 3)
                if (parts.size == 3) {
                    val rawFilename = parts[1].ifBlank { "archivo.gcode" }
                    val base64 = parts[2]
                    try {
                        val safeName = IndustrialErrorManager.sanitizeFileName(rawFilename)
                        val bytes = Base64.decode(base64, Base64.NO_WRAP)
                        IndustrialErrorManager.validateGcodeSize(bytes)
                        context.openFileOutput(safeName, Context.MODE_PRIVATE).use { output ->
                            output.write(bytes)
                        }
                        addLog("✓ G-code recibido (legacy): $safeName (${bytes.size} bytes)")
                    } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                        addLog("✗ Error guardando G-code legacy: ${e.message ?: "desconocido"}")
                    }
                } else {
                    addLog("⚠ Formato GCODE_LOAD inválido")
                }
            }
            command.startsWith("L:") || command.startsWith("R:") || command.startsWith("M:") || command.startsWith("C:") -> {
                sendAuthorizedHardwareCommand(command, "CMD RECIBIDO: $command")
            }
            else -> {
                addLog("⚠ Comando desconocido: $command")
            }
        }
    }

    val stationClient = remember(ipCoordinator) {
        StationClient(host = ipCoordinator, port = 8888, stationName = "MANUFACTURA", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-ST-MAN-X2").apply {
            onLog = { msg -> logs.add(0, "[NET] $msg") }
            onStatusChanged = { isConnectedNet = it }
            onAuthorizationStateChanged = { authorizationState = it }
            onCommandReceived = { cmd ->
                scope.launch {
                    handleIncomingCoordinatorCommand(cmd)
                }
            }
        }
    }

    // File picker for external G-code / Imagen para Laser - CORREGIDO con sanitización + LaserImageProcessor
    val gcodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    val isImage = mime.startsWith("image/") || uri.toString().endsWith(".png") || uri.toString().endsWith(".jpg") || uri.toString().endsWith(".jpeg")
                    val input = context.contentResolver.openInputStream(uri)
                    val bytes = input?.readBytes() ?: ByteArray(0)
                    if (isImage) {
                        // Convertir imagen a G-code usando LaserImageProcessor (port integrated_panel.py)
                        try {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                val params = LaserImageProcessor.LaserParams(
                                    powerPercent = laserPower.toIntOrNull() ?: 80,
                                    speedMmMin = laserSpeed.toIntOrNull() ?: 1200,
                                    threshold = 128,
                                    pixelSizeMm = 0.1f,
                                    maxWidthPx = 400
                                )
                                val gcode = LaserImageProcessor.bitmapToGcode(bitmap, params)
                                val safeName = "laser_image_${System.currentTimeMillis()}.gcode"
                                context.openFileOutput(safeName, Context.MODE_PRIVATE).use { it.write(gcode.toByteArray()) }
                                val b64 = Base64.encodeToString(gcode.toByteArray(), Base64.NO_WRAP)
                                val payload = "LASER_LOAD:$safeName:$b64"
                                stationClient.sendEventSafe(payload)
                                addLog("✓ Imagen → G-code: $safeName (${gcode.length} chars, ${gcode.lines().size} líneas)")
                            } else {
                                addLog("✗ No se pudo decodificar imagen")
                            }
                        } catch (e: Exception) {
                            Log.e("CIM", "Error procesando imagen: ${e.message}", e)
                            addLog("✗ Error imagen→G-code: ${e.message}")
                        }
                    } else {
                        // G-code directo
                        IndustrialErrorManager.validateGcodeSize(bytes)
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val rawName = uri.lastPathSegment ?: "gcode.gcode"
                        val safeName = try { IndustrialErrorManager.sanitizeFileName(rawName) } catch(_:Exception){ "gcode_${System.currentTimeMillis()}.gcode" }
                        val payload = "GCODE_LOAD;$safeName;$b64"
                        val sent = stationClient.sendEventSafe(payload)
                        if (sent) addLog("IMG: archivo '$safeName' cargado y enviado") else addLog("IMG: fallo al enviar archivo '$safeName'")
                    }
                } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                    addLog("IMG: error leyendo archivo: ${e.message ?: "desconocido"}")
                }
            }
        } else {
            addLog("IMG: selección de archivo cancelada")
        }
    }

    IndustrialScaffold(
        titulo = "Manufactura Pro v6.0", 
        subtitulo = "ESTACIÓN DE MECANIZADO INTEGRADA",
        estado = {
            // Chip de estado en cabecera (punto + texto), como en los mockups HMI.
            val enLinea = isConnectedBt || isConnectedNet
            IndustrialStatusChip(
                texto = when {
                    independentMode -> "AUTONOMO"
                    enLinea -> "READY"
                    else -> "OFFLINE"
                },
                color = when {
                    independentMode -> IndustrialTheme.Advertencia
                    enLinea -> IndustrialTheme.Primario
                    else -> IndustrialTheme.Error
                },
                parpadeo = !enLinea && !independentMode
            )
        },
        bottomBar = {
            // Bottom-nav de la figura de referencia (CONTROL / LÁSER / IMAGEN / SINCRO).
            IndustrialBottomNav(
                items = listOf(
                    IndustrialNavItem("CONTROL", Icons.Default.ControlCamera),
                    IndustrialNavItem("LÁSER", Icons.Default.FlashOn),
                    IndustrialNavItem("IMAGEN", Icons.Default.Image),
                    IndustrialNavItem("SINCRO", Icons.Default.Wifi)
                ),
                seleccion = selectedTab,
                onSelect = { selectedTab = it }
            )
        },
        floatingActionButton = { BluetoothConnectionFAB() },
        navigationIcon = {
            Box(Modifier.testModeSecretGesture(context) { enabled ->
                addLog(if (enabled) "MODO INGENIERÍA ACTIVADO" else "MODO INGENIERÍA DESACTIVADO")
            }.padding(8.dp)) {
                Icon(Icons.Default.PrecisionManufacturing, null, tint = IndustrialTheme.Primario)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Control Scorbot", Icons.Default.PrecisionManufacturing) {
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("HOME", Icons.Default.Home, Modifier.weight(1f), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:HOME", "CMD: HOME") })
                                IndustrialActionButton("READY", Icons.Default.Check, Modifier.weight(1f), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:READY", "CMD: READY") })
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("JOGGING", color = IndustrialTheme.Primario, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))

                            // D-pad XY de la figura de referencia: cruceta con lectura central.
                            var jogStep by remember { mutableStateOf("10") }
                            fun jog(axis: String, signo: String) {
                                sendAuthorizedHardwareCommand("R:MOVE:$axis:$signo$jogStep", "CMD: MOVE $axis $signo$jogStep")
                            }

                            Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                JogButton(Icons.Default.KeyboardArrowUp, isOperationalReady) { jog("Y", "+") }
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    JogButton(Icons.Default.KeyboardArrowLeft, isOperationalReady) { jog("X", "-") }
                                    Column(
                                        Modifier.width(96.dp).padding(horizontal = 6.dp),
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        Text("X", color = IndustrialTheme.Primario, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("0.00 mm", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(IndustrialTheme.Borde))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Y", color = IndustrialTheme.Primario, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("0.00 mm", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                                    }
                                    JogButton(Icons.Default.KeyboardArrowRight, isOperationalReady) { jog("X", "+") }
                                }
                                JogButton(Icons.Default.KeyboardArrowDown, isOperationalReady) { jog("Y", "-") }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("STEP", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(6.dp))
                            IndustrialChipRow(
                                opciones = listOf("0.1", "1", "10", "100"),
                                seleccion = jogStep,
                                onSelect = { jogStep = it }
                            )
                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton("GUARDAR PUNTO", Icons.Default.Save, colorFondo = IndustrialTheme.Exito, enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:SAVE", "CMD: SAVE") })
                        }
                        ScorbotRunConsole(
                            enabled = isOperationalReady,
                            presets = listOf("ARU" to "ARU", "ARU1" to "ARU1", "ARU2" to "ARU2", "ARU3" to "ARU3", "ARU4" to "ARU4"),
                            initialProgram = "ARU",
                            manualLabel = "Programa (ej: ARU, MYPROG)",
                            onRun = { prog -> sendAuthorizedHardwareCommand("R:RUN $prog", "RUN $prog") },
                            onAuto = { sendAuthorizedHardwareCommand("R:AUTO", "AUTO") }
                        )
                    }
                    1 -> {
                        // Panel de láser con el lenguaje de la figura de referencia:
                        // steppers -/+ con valor grande y barra, presets y START/STOP pareados.
                        val powerValue = laserPower.toIntOrNull() ?: 80
                        val speedValue = laserSpeed.toIntOrNull() ?: 1200
                        val presetActual = when {
                            powerValue <= 35 && speedValue <= 700 -> "LOW"
                            powerValue in 36..70 -> "MEDIUM"
                            powerValue >= 71 && speedValue >= 1500 -> "HIGH"
                            else -> "CUSTOM"
                        }

                        IndustrialCard(
                            titulo = "Control Láser",
                            icono = Icons.Default.FlashOn,
                            headerColor = IndustrialTheme.Advertencia,
                            subtitulo = "Grabado CNC",
                            trailing = {
                                IndustrialStatusChip(
                                    if (isOperationalReady) "ON" else "OFF",
                                    if (isOperationalReady) IndustrialTheme.Primario else IndustrialTheme.TextoTenue
                                )
                            }
                        ) {
                            IndustrialStepper(
                                label = "Power",
                                valor = powerValue.toFloat(),
                                unidad = "%",
                                paso = 5f,
                                minimo = 0f,
                                maximo = 100f,
                                onValorChange = { laserPower = it.toInt().toString() }
                            )
                            IndustrialStepper(
                                label = "Speed",
                                valor = speedValue.toFloat(),
                                unidad = "mm/min",
                                paso = 100f,
                                minimo = 100f,
                                maximo = 3000f,
                                onValorChange = { laserSpeed = it.toInt().toString() }
                            )

                            Spacer(Modifier.height(10.dp))
                            Text("PRESETS", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(6.dp))
                            IndustrialChipRow(
                                opciones = listOf("LOW", "MEDIUM", "HIGH", "CUSTOM"),
                                seleccion = presetActual,
                                onSelect = { preset ->
                                    when (preset) {
                                        "LOW" -> { laserPower = "30"; laserSpeed = "600" }
                                        "MEDIUM" -> { laserPower = "60"; laserSpeed = "1200" }
                                        "HIGH" -> { laserPower = "90"; laserSpeed = "1800" }
                                    }
                                    addLog("LÁSER: preset $preset seleccionado")
                                }
                            )

                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                                IndustrialActionButton(
                                    texto = "START",
                                    icono = Icons.Default.PlayArrow,
                                    modifier = Modifier.weight(1f),
                                    colorFondo = IndustrialTheme.Primario,
                                    enabled = isOperationalReady,
                                    onClick = { sendAuthorizedHardwareCommand("L:START", "CMD: L:START") }
                                )
                                IndustrialActionButton(
                                    texto = "STOP",
                                    icono = Icons.Default.Stop,
                                    modifier = Modifier.weight(1f),
                                    colorFondo = IndustrialTheme.Error,
                                    enabled = isOperationalReady,
                                    onClick = { sendAuthorizedHardwareCommand("L:STOP", "CMD: L:STOP") }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            IndustrialActionButton(
                                texto = "APLICAR PARÁMETROS",
                                icono = Icons.Default.Settings,
                                colorFondo = IndustrialTheme.TarjetaAlta,
                                enabled = isOperationalReady,
                                onClick = {
                                    sendAuthorizedHardwareCommand("L:POWER:$powerValue", "CMD: POWER $powerValue")
                                    sendAuthorizedHardwareCommand("L:SPEED:$speedValue", "CMD: SPEED $speedValue")
                                }
                            )
                        }
                    }
                    2 -> {
                        var showArucoGenerator by remember { mutableStateOf(false) }
                        var arucoGenId by remember { mutableStateOf("0") }
                        var arucoGenSizeMm by remember { mutableStateOf("100") }
                        var selectedDictionary by remember { mutableStateOf(ArucoDictionary.DICT_4X4_50) }
                        var dictExpanded by remember { mutableStateOf(false) }
                        var generatedArucoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                        var isGeneratingAruco by remember { mutableStateOf(false) }

                        LaunchedEffect(pendingArucoGenerate.value) {
                            val payload = pendingArucoGenerate.value ?: return@LaunchedEffect
                            pendingArucoGenerate.value = null
                            showArucoGenerator = true
                            var id = 0
                            var sizeMm = 100
                            var dict = ArucoDictionary.DICT_4X4_50
                            payload.split("|").forEach { part ->
                                when {
                                    part.startsWith("ID:") -> id = part.removePrefix("ID:").toIntOrNull() ?: id
                                    part.startsWith("SIZE:") -> sizeMm = part.removePrefix("SIZE:").toIntOrNull() ?: sizeMm
                                    part.startsWith("DICT:") -> dict = ArucoDictionary.fromName(part.removePrefix("DICT:"))
                                    part.toIntOrNull() != null -> id = part.toInt()
                                }
                            }
                            arucoGenId = id.toString()
                            arucoGenSizeMm = sizeMm.toString()
                            selectedDictionary = dict
                            isGeneratingAruco = true
                            try {
                                generatedArucoBitmap = IndustrialVisionAnalyzer.generateArucoMarkerMm(id, sizeMm, dict)
                                addLog("VISIÓN: ArUco #$id generado desde coordinador (${dict.label}, ${sizeMm}mm)")
                            } finally {
                                isGeneratingAruco = false
                            }
                        }

                        IndustrialCard("Procesamiento de Imagen", Icons.Default.Image) {
                            if (!showArucoGenerator) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(IndustrialTheme.TarjetaAlta, androidx.compose.foundation.shape.RoundedCornerShape(IndustrialTheme.RadioControl))
                                        .border(1.dp, IndustrialTheme.Borde, androidx.compose.foundation.shape.RoundedCornerShape(IndustrialTheme.RadioControl)),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text("VISTA PREVIA G-CODE", color = IndustrialTheme.TextoSecundario, fontSize = 12.sp, letterSpacing = 0.8.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                CameraPreviewWithVision(
                                    isDetecting = true,
                                    arucoDictionary = selectedDictionary,
                                    onArucoFound = { results ->
                                        if (results.isNotEmpty()) {
                                            lastDetectedArucoId = results[0].id
                                            addLog("VISIÓN: Detectado ArUco #${results[0].id} (${selectedDictionary.label})")
                                            scope.launch {
                                                stationClient.sendEventSafe("ARUCO_DETECTED:${results[0].id}|DICT:${selectedDictionary.name}")
                                            }
                                        }
                                    },
                                    onQrFound = { qr ->
                                        addLog("VISIÓN: QR Detectado -> $qr")
                                    }
                                )
                                if (lastDetectedArucoId != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = IndustrialTheme.Exito.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, IndustrialTheme.Exito),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    ) {
                                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ARUCO IDENTIFICADO", color = IndustrialTheme.Exito, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                            Text("#${lastDetectedArucoId}", color = IndustrialTheme.TextoPrincipal, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                                            Text("Marcador ArUco ${selectedDictionary.label} · listo para grabado", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                IndustrialActionButton("SIMULAR DETECCIÓN ArUco", Icons.Default.Sensors, colorFondo = IndustrialTheme.Secundario, onClick = {
                                    val demoId = (0..49).random()
                                    lastDetectedArucoId = demoId
                                    addLog("DEMO: Detección simulada ArUco #$demoId (${selectedDictionary.label})")
                                    scope.launch { stationClient.sendEventSafe("ARUCO_DETECTED:$demoId|DICT:${selectedDictionary.name}") }
                                })
                                Spacer(Modifier.height(12.dp))
                                Text("Diccionario detección", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                                ExposedDropdownMenuBox(expanded = dictExpanded, onExpandedChange = { dictExpanded = it }) {
                                    OutlinedTextField(
                                        value = selectedDictionary.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        label = { Text("Diccionario ArUco") }
                                    )
                                    ExposedDropdownMenu(expanded = dictExpanded, onDismissRequest = { dictExpanded = false }) {
                                        ArucoDictionary.entries.filter { it.label.startsWith("4x4") || it.label.startsWith("5x5") }.forEach { dict ->
                                            DropdownMenuItem(
                                                text = { Text(dict.label) },
                                                onClick = { selectedDictionary = dict; dictExpanded = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                IndustrialActionButton("GENERAR ArUco PARA GRABAR", Icons.Default.AutoFixHigh, colorFondo = IndustrialTheme.Secundario, onClick = { showArucoGenerator = true })
                                Spacer(Modifier.height(8.dp))
                                IndustrialActionButton("CARGAR ARCHIVO EXTERNO", Icons.Default.Folder, onClick = {
                                    try {
                                        gcodeLauncher.launch(arrayOf("*/*"))
                                    } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                                        addLog("IMG: error abriendo selector de archivos: ${e.message ?: "desconocido"}")
                                    }
                                })
                            } else {
                                Text("Generador de ArUco para Láser", color = IndustrialTheme.Primario, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                ExposedDropdownMenuBox(expanded = dictExpanded, onExpandedChange = { dictExpanded = it }) {
                                    OutlinedTextField(
                                        value = selectedDictionary.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        label = { Text("Diccionario") }
                                    )
                                    ExposedDropdownMenu(expanded = dictExpanded, onDismissRequest = { dictExpanded = false }) {
                                        ArucoDictionary.entries.forEach { dict ->
                                            DropdownMenuItem(
                                                text = { Text(dict.label) },
                                                onClick = { selectedDictionary = dict; dictExpanded = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                IndustrialTextField(
                                    valor = arucoGenId,
                                    onValueChange = { arucoGenId = it.filter { c -> c.isDigit() }.take(4) },
                                    label = "ID Marcador (0-${selectedDictionary.maxId})"
                                )
                                Spacer(Modifier.height(8.dp))
                                IndustrialTextField(
                                    valor = arucoGenSizeMm,
                                    onValueChange = { arucoGenSizeMm = it.filter { c -> c.isDigit() }.take(4) },
                                    label = "Tamaño físico (mm, ej: 100)"
                                )
                                Spacer(Modifier.height(12.dp))
                                IndustrialActionButton(
                                    texto = if (isGeneratingAruco) "Generando..." else "Generar Pattern",
                                    icono = Icons.Default.Autorenew,
                                    loading = isGeneratingAruco,
                                    onClick = {
                                        scope.launch {
                                            isGeneratingAruco = true
                                            try {
                                                val id = arucoGenId.toIntOrNull() ?: 0
                                                val sizeMm = arucoGenSizeMm.toIntOrNull() ?: 100
                                                generatedArucoBitmap = IndustrialVisionAnalyzer.generateArucoMarkerMm(id, sizeMm, selectedDictionary)
                                                if (generatedArucoBitmap != null) {
                                                    addLog("VISIÓN: ArUco #$id generado (${selectedDictionary.label}, ${sizeMm}mm)")
                                                }
                                            } catch (e: Exception) {
            Log.e("CIM", "Error: ${e.message}", e)
                                                addLog("ERROR: ${e.message ?: "desconocido"}")
                                            } finally {
                                                isGeneratingAruco = false
                                            }
                                        }
                                    }
                                )
                                
                                if (generatedArucoBitmap != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(IndustrialTheme.TextoPrincipal, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = generatedArucoBitmap!!.asImageBitmap(),
                                            contentDescription = "ArUco ${arucoGenId}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    IndustrialActionButton(
                                        texto = "ENVIAR AL LÁSER",
                                        icono = Icons.Default.FlashOn,
                                        colorFondo = IndustrialTheme.Advertencia,
                                        onClick = {
                                            val bitmap = generatedArucoBitmap ?: return@IndustrialActionButton
                                            val id = arucoGenId.toIntOrNull() ?: 0
                                            val b64 = IndustrialVisionAnalyzer.bitmapToPngBase64(bitmap)
                                            val rawFilename = "aruco_${selectedDictionary.name}_${id}.png"
                                            val filename = try { IndustrialErrorManager.sanitizeFileName(rawFilename, setOf(".png",".gcode",".txt")) } catch(_:Exception){ "aruco_${id}.png" }
                                            sendAuthorizedHardwareCommand(
                                                "L:ARUCO:${id}|DICT:${selectedDictionary.name}|SIZE:${arucoGenSizeMm}",
                                                "LÁSER: Grabando ArUco #$id"
                                            )
                                            scope.launch {
                                                val payload = "LASER_LOAD:$filename:$b64"
                                                if (isConnectedNet) {
                                                    stationClient.sendEventSafe(payload)
                                                }
                                                val decoded = android.util.Base64.decode(b64, Base64.NO_WRAP)
                                                IndustrialErrorManager.validateGcodeSize(decoded, 10*1024*1024)
                                                context.openFileOutput(filename, Context.MODE_PRIVATE).use { it.write(decoded) }
                                                addLog("LÁSER: Patrón ArUco #$id enviado (${filename})")
                                            }
                                            showArucoGenerator = false
                                        }
                                    )
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                IndustrialActionButton(
                                    texto = "Volver a Cámara",
                                    icono = Icons.Default.Close,
                                    colorFondo = IndustrialTheme.Error,
                                    onClick = { showArucoGenerator = false; generatedArucoBitmap = null }
                                )
                            }
                        }
                    }
                    3 -> {
                        EasyConnectCard(
                            ipCoordinator = ipCoordinator,
                            onIpChange = { ipCoordinator = it },
                            discoveredIp = discoveredHubIp.value,
                            isConnectedNet = isConnectedNet,
                            isAuthorized = isAuthorized,
                            authorizationState = authorizationState,
                            independentMode = independentMode,
                            onIndependentChange = { independentMode = it },
                            onConnect = { stationClient.connect() }
                        )
                    }
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(200.dp))
            }
        }
    }
}

// FIX: Límite de colección (MAX=500)
private val MAX_COLLECTION_SIZE = 500

// FIX CRÍTICO: Validación de G-code
private fun isValidGcode(content: String): Boolean {
    if (content.isBlank()) return false
    if (content.length > 1024 * 1024) return false // Máximo 1MB
    
    val validCommands = setOf("G0", "G1", "G2", "G3", "M0", "M1", "M2", "M3", "M5", "M30")
    val lines = content.lines()
    
    return lines.all { line ->
        val trimmed = line.trim()
        trimmed.isEmpty() || 
        trimmed.startsWith(";") || 
        validCommands.any { trimmed.startsWith(it) }
    }
}


/** Botón redondo del D-pad de jogging (figura `ui_app_manufactura.png`). */
@Composable
private fun JogButton(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(54.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(IndustrialTheme.TarjetaAlta)
            .border(1.dp, IndustrialTheme.Borde, androidx.compose.foundation.shape.CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            icono,
            null,
            Modifier.size(26.dp),
            tint = if (enabled) IndustrialTheme.TextoPrincipal else IndustrialTheme.TextoTenue
        )
    }
}
