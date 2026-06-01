package com.industria.calidad

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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
        AppIdentifier.init(this, AppType.CALIDAD)
        GlobalPermissionManager.init(this)
        GlobalBluetoothManager.init(this)
        enableEdgeToEdge()
        setContent {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
            LaunchedEffect(Unit) {
                val p = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    p.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                }
                launcher.launch(p.toTypedArray())
            }
            CalidadApp()
        }
    }
}

@Composable
fun CalidadApp() {
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
    var selectedTab by remember { mutableStateOf(0) }
    var approvedCount by remember { mutableStateOf(1240) }
    var rejectedCount by remember { mutableStateOf(68) }

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
            command == "STATS:RESET" -> {
                approvedCount = 0
                rejectedCount = 0
                addLog("✓ Contadores reiniciados desde coordinador")
            }
            command.startsWith("CAM:") -> {
                sendAuthorizedHardwareCommand(command, "CMD RECIBIDO: $command")
            }
            else -> {
                addLog("⚠ Comando desconocido: $command")
            }
        }
    }

    val stationClient = remember(ipCoordinator) {
        StationClient(host = ipCoordinator, port = 8888, stationName = "CALIDAD", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-CAL-03").apply {
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

    IndustrialScaffold(
        titulo = "Quality Pro v6.0", 
        subtitulo = "CONTROL DE CALIDAD & VISIÓN",
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.Black, contentColor = IndustrialTheme.Primario, edgePadding = 16.dp, divider = {}) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("VISIÓN", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("STATS", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("SINCRO", fontSize = 12.sp) })
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Análisis ArUco / YOLO", Icons.Default.Camera) {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black).border(1.dp, IndustrialTheme.Borde), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                CameraPreviewWithVision(
                                    isDetecting = isConnectedBt && isAuthorized,
                                    onArucoFound = { results ->
                                        if (results.isNotEmpty()) {
                                            addLog("VISIÓN: Detectado ArUco #${results[0].id}")
                                        }
                                    },
                                    onQrFound = { qr ->
                                        addLog("VISIÓN: QR Detectado -> $qr")
                                    }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            IndustrialActionButton("Capturar y Validar", Icons.Default.Camera, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("CAM:SNAP", "CMD: TRIGGER SCAN") })
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("PASS", Icons.Default.CheckCircle, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Exito, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = {
                                    approvedCount += 1
                                    sendAuthorizedHardwareCommand("VAL:PASS", "RESULT: APPROVED")
                                })
                                IndustrialActionButton("FAIL", Icons.Default.Cancel, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Error, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = {
                                    rejectedCount += 1
                                    sendAuthorizedHardwareCommand("VAL:FAIL", "RESULT: REJECTED")
                                })
                            }
                        }
                    }
                    1 -> {
                        IndustrialCard("Estadísticas de Producción", Icons.Default.BarChart) {
                            val totalPieces = approvedCount + rejectedCount
                            val approvalRate = if (totalPieces > 0) (approvedCount * 100.0 / totalPieces) else 0.0
                            Text("Tasa de Aprobación: ${"%.1f".format(approvalRate)}%", color = IndustrialTheme.Exito, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Piezas Totales: $totalPieces", color = Color.White)
                            Text("Piezas Aprobadas: $approvedCount", color = IndustrialTheme.Exito)
                            Text("Piezas Rechazadas: $rejectedCount", color = IndustrialTheme.Error)
                            Spacer(Modifier.height(16.dp))
                            IndustrialActionButton("Limpiar Contador", Icons.Default.Delete, colorFondo = Color.DarkGray, onClick = {
                                // Reset local counters
                                approvedCount = 0
                                rejectedCount = 0
                                addLog("STATS: contadores reiniciados")
                                // Notify hardware/coordinator about reset (if authorized)
                                sendAuthorizedHardwareCommand("STATS:RESET", "CMD: STATS_RESET")
                            })
                        }
                    }
                    2 -> {
                        IndustrialCard("Enlace al Hub Maestro", Icons.Default.Lan, headerColor = IndustrialTheme.Secundario) {
                            IndustrialTextField(valor = ipCoordinator, onValueChange = { ipCoordinator = it }, label = "IP Coordinador")
                            IndustrialStatusRow("Conexión HUB", if(isConnectedNet) "SINCRONIZADO" else "STANDBY", isConnectedNet)
                            IndustrialStatusRow("Autorización", authorizationState, isAuthorized)
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Modo Autónomo", color = IndustrialTheme.TextoSecundario)
                                Switch(checked = independentMode, onCheckedChange = { independentMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = IndustrialTheme.Exito))
                            }
                            IndustrialStatusRow("Modo Autónomo", if(independentMode) "ACTIVO" else "DESACTIVADO", independentMode)
                            IndustrialActionButton(texto = if(isConnectedNet) "OPERATIVO" else "VINCULAR", icono = Icons.Default.Router, colorFondo = if(isConnectedNet) IndustrialTheme.Exito else IndustrialTheme.Primario, onClick = { stationClient.connect() })
                        }
                    }
                }

                IndustrialCard("Simulador ESP32-CAM", Icons.Default.DeveloperMode, headerColor = Color.Magenta) {
                    IndustrialActionButton(texto = "Simular Captura Exitosa", icono = Icons.Default.PhotoCamera, colorFondo = Color.DarkGray, onClick = { addLog("SIM_ESP32: CAM_SNAP_OK | DATA SENT") })
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}
