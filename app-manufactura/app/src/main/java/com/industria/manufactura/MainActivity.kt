package com.industria.manufactura

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.util.Base64
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.sistema.distribuido.network.*
import com.sistema.distribuido.network.prefecto.*
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimProtocol
import java.util.*

class MainActivity : ComponentActivity() {
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
                val p = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    p.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                }
                launcher.launch(p.toTypedArray())
            }
            ManufacturaApp()
        }
    }
}

@Composable
fun ManufacturaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    val bt = GlobalBluetoothManager.getInstance()
    val connectionStates by bt.connectionStates.collectAsState()
    val isConnectedBt by remember { derivedStateOf { connectionStates.values.any { it } } }

    var isConnectedNet by remember { mutableStateOf(false) }
    var authorizationState by remember { mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED) }
    val isAuthorized by remember { derivedStateOf { authorizationState == CimProtocol.AUTH_STATE_VALIDATED } }
    var ipCoordinator by remember { mutableStateOf("192.168.1.100") }
    var selectedTab by remember { mutableStateOf(0) }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
    }

    fun sendAuthorizedHardwareCommand(command: String, logText: String) {
        if (!isAuthorized) {
            addLog("✗ No autorizado - esperar VALIDADO por coordinador")
            return
        }
        bt.send(command)
        addLog(logText)
    }

    fun handleIncomingCoordinatorCommand(command: String) {
        addLog("← COORDINADOR: $command")
        when {
            command.startsWith("LASER_LOAD:") -> {
                val parts = command.split(":", limit = 3)
                if (parts.size == 3) {
                    val filename = parts[1].ifBlank { "archivo.gcode" }
                    val base64 = parts[2]
                    try {
                        val bytes = Base64.decode(base64, Base64.NO_WRAP)
                        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                            output.write(bytes)
                        }
                        addLog("✓ G-code recibido: $filename (${bytes.size} bytes)")
                    } catch (e: Exception) {
                        addLog("✗ Error guardando G-code: ${e.message}")
                    }
                } else {
                    addLog("⚠ Formato LASER_LOAD inválido")
                }
            }
            command.startsWith("GCODE_LOAD;") -> {
                val parts = command.split(";", limit = 3)
                if (parts.size == 3) {
                    val filename = parts[1].ifBlank { "archivo.gcode" }
                    val base64 = parts[2]
                    try {
                        val bytes = Base64.decode(base64, Base64.NO_WRAP)
                        context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                            output.write(bytes)
                        }
                        addLog("✓ G-code recibido (legacy): $filename (${bytes.size} bytes)")
                    } catch (e: Exception) {
                        addLog("✗ Error guardando G-code legacy: ${e.message}")
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

    val stationClient = remember {
        StationClient(host = ipCoordinator, port = 8888, stationName = "MANUFACTURA", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-MAN-02").apply {
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

    // File picker for external G-code loading
    val gcodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val bytes = input?.readBytes() ?: ByteArray(0)
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    // Send as an EVENT to coordinator (stationClient will handle connection)
                    val filename = uri.lastPathSegment ?: "gcode"
                    val payload = "GCODE_LOAD;$filename;$b64"
                    val sent = stationClient.sendEventSafe(payload)
                    if (sent) addLog("IMG: archivo '$filename' cargado y enviado") else addLog("IMG: fallo al enviar archivo '$filename'")
                } catch (e: Exception) {
                    addLog("IMG: error leyendo archivo: ${e.message}")
                }
            }
        } else {
            addLog("IMG: selección de archivo cancelada")
        }
    }

    IndustrialScaffold(
        titulo = "Manufactura Pro v6.0", 
        subtitulo = "ESTACIÓN DE MECANIZADO INTEGRADA",
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.Black, contentColor = IndustrialTheme.Primario, edgePadding = 16.dp, divider = {}) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ROBOT", fontSize = 11.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("LÁSER", fontSize = 11.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("IMAGEN", fontSize = 11.sp) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("SINCRO", fontSize = 11.sp) })
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Control Scorbot", Icons.Default.PrecisionManufacturing) {
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("HOME", Icons.Default.Home, Modifier.weight(1f), enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("R:HOME", "CMD: HOME") })
                                IndustrialActionButton("READY", Icons.Default.Check, Modifier.weight(1f), enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("R:READY", "CMD: READY") })
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("MOVIMIENTO MANUAL (JOGGING)", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            repeat(2) { axis ->
                                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val axisName = if(axis == 0) "X" else "Y"
                                    Text(axisName, modifier = Modifier.width(20.dp), color = Color.White, fontWeight = FontWeight.Bold)
                                    IndustrialActionButton("-", Icons.Default.Remove, Modifier.weight(1f).height(36.dp), enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("R:MOVE:$axisName:-10", "CMD: MOVE $axisName -10") })
                                    IndustrialActionButton("+", Icons.Default.Add, Modifier.weight(1f).height(36.dp), enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("R:MOVE:$axisName:+10", "CMD: MOVE $axisName +10") })
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton("GUARDAR PUNTO", Icons.Default.Save, colorFondo = IndustrialTheme.Exito, enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("R:SAVE", "CMD: SAVE") })
                        }
                    }
                    1 -> {
                        IndustrialCard("Grabado Láser CNC", Icons.Default.FlashOn, headerColor = IndustrialTheme.Advertencia) {
                            IndustrialActionButton("INICIAR GRABADO", Icons.Default.PlayArrow, colorFondo = IndustrialTheme.Exito, enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("L:START", "CMD: L:START") })
                            Spacer(Modifier.height(8.dp))
                            IndustrialActionButton("STOP EMERGENCIA", Icons.Default.Stop, colorFondo = IndustrialTheme.Error, enabled = isConnectedBt && isAuthorized, onClick = { sendAuthorizedHardwareCommand("L:STOP", "CMD: L:STOP") })
                            Spacer(Modifier.height(16.dp))
                            Text("PARÁMETROS", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            IndustrialTextField(valor = "80%", onValueChange = {}, label = "Potencia Láser")
                            IndustrialTextField(valor = "1200", onValueChange = {}, label = "Velocidad (mm/min)")
                        }
                    }
                    2 -> {
                        IndustrialCard("Procesamiento de Imagen", Icons.Default.Image) {
                            Box(Modifier.fillMaxWidth().height(150.dp).background(Color.DarkGray).border(1.dp, Color.Gray), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text("VISTA PREVIA G-CODE", color = Color.Gray, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton("GENERAR DESDE ARUCO", Icons.Default.AutoFixHigh, colorFondo = IndustrialTheme.Secundario, onClick = { addLog("IMG: Pattern conversion starting...") })
                            Spacer(Modifier.height(8.dp))
                            IndustrialActionButton("CARGAR ARCHIVO EXTERNO", Icons.Default.Folder, onClick = {
                                try {
                                    gcodeLauncher.launch(arrayOf("*/*"))
                                } catch (e: Exception) {
                                    addLog("IMG: error abriendo selector de archivos: ${e.message}")
                                }
                            })
                        }
                    }
                    3 -> {
                        IndustrialCard("Red Industrial", Icons.Default.Lan, headerColor = IndustrialTheme.Secundario) {
                            IndustrialTextField(valor = ipCoordinator, onValueChange = { ipCoordinator = it }, label = "IP Coordinador")
                            IndustrialStatusRow("Estado Red", if(isConnectedNet) "SINCRO OK" else "STANDBY", isConnectedNet)
                            IndustrialStatusRow("Autorización", authorizationState, isAuthorized)
                            IndustrialActionButton(texto = "UNIR AL HUB", icono = Icons.Default.Router, onClick = { stationClient.connect() })
                        }
                    }
                }

                IndustrialCard("Simulador ESP32 (Hardware)", Icons.Default.DeveloperMode, headerColor = Color.Magenta) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IndustrialActionButton("SIM ACK", Icons.Default.Check, Modifier.weight(1f), colorFondo = Color.DarkGray, onClick = { addLog("SIM_ESP32: CMD_RECEIVED_OK") })
                        IndustrialActionButton("SIM FINISH", Icons.Default.Flag, Modifier.weight(1f), colorFondo = Color.DarkGray, onClick = { addLog("SIM_ESP32: CYCLE_COMPLETE") })
                    }
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(200.dp))
            }
        }
    }
}
