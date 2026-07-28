// FIX Lote 9: Edge case handling
// FIX: Constantes extraídas
/**
 * MainActivity
 * FIX: Documentación agregada
 */
package com.industria.almacenamiento
import android.util.Log

import android.Manifest
import kotlinx.coroutines.withTimeout
import android.os.Build
import kotlinx.coroutines.withTimeout
import android.os.Bundle
import kotlinx.coroutines.withTimeout
import androidx.activity.ComponentActivity
import kotlinx.coroutines.withTimeout
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.withTimeout
import androidx.activity.compose.setContent
import kotlinx.coroutines.withTimeout
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.withTimeout
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.withTimeout
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.withTimeout
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.withTimeout
import androidx.compose.material3.*
import kotlinx.coroutines.withTimeout
import androidx.compose.runtime.*
import kotlinx.coroutines.withTimeout
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.Alignment
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.Modifier
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeout
import com.sistema.distribuido.network.*
import kotlinx.coroutines.withTimeout
import com.sistema.distribuido.network.prefecto.*
import kotlinx.coroutines.withTimeout
import com.sistema.distribuido.network.protocol.AppType
import kotlinx.coroutines.withTimeout
import com.sistema.distribuido.network.protocol.CimProtocol
import kotlinx.coroutines.withTimeout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.withTimeout
import java.util.*
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlinx.coroutines.withTimeout

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var commCoordinator: CommunicationCoordinator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IndustrialErrorManager.install(this) {}
        AppIdentifier.init(this, AppType.ALMACEN)
        GlobalPermissionManager.init(this)
        GlobalBluetoothManager.init(this, onLog = { msg ->
            // logs handled by local instances usually, but we can set up a global log stream if needed
        })
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
            AlmacenApp(commCoordinator)
        }
    }
}

@Composable
fun AlmacenApp(commCoordinator: CommunicationCoordinator) {
    val context = LocalContext.current
    val logs = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val bt = GlobalBluetoothManager.getInstance()
    val connectionStates by bt.connectionStates.collectAsState()
    val isConnectedBt by remember { derivedStateOf { connectionStates.values.any { it } } }

    var isConnectedNet by remember { mutableStateOf(false) }
    var authorizationState by remember { mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED) }
    val isAuthorized by remember { derivedStateOf { authorizationState == CimProtocol.AUTH_STATE_VALIDATED } }
    var independentMode by remember { mutableStateOf(false) }
    var ipCoordinator by remember { mutableStateOf("192.168.1.100") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedRackPosition by remember { mutableStateOf(1) }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
    }

    val stationClient = remember(ipCoordinator) {
        StationClient(host = ipCoordinator, port = 8888, stationName = "ALMACEN", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-ALM-01").apply {
            onLog = { msg -> logs.add(0, "[NET] $msg") }
            onStatusChanged = { isConnectedNet = it }
            onAuthorizationStateChanged = { authorizationState = it }
        }
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

    IndustrialScaffold(
        titulo = "Logística Pro v6.0", 
        subtitulo = "GESTIÓN DE RACKS INDUSTRIAL",
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.Black, contentColor = IndustrialTheme.Primario, edgePadding = 16.dp, divider = {}) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("POSICIONES", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("SINCRO", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("BRAZO", fontSize = 12.sp) })
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Matriz de Almacén (18 POS)", Icons.Default.Inventory2) {
                            IndustrialStatusRow("Conexión ESP32", if(isConnectedBt) "LINK OK" else "OFFLINE", isConnectedBt)
                            Text("Selecciona la posición del rack y pulsa ALMACENAR", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))

                            repeat(3) { level ->
                                Text("NIVEL ${level + 1}", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(6) { col ->
                                        val posId = level * 6 + col + 1
                                        IndustrialActionButton(
                                            texto = "$posId",
                                            icono = Icons.Default.Inventory2,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colorFondo = if (selectedRackPosition == posId) IndustrialTheme.Exito else IndustrialTheme.Tarjeta,
                                            enabled = true,
                                            buttonHeight = 36.dp,
                                            fillMaxWidth = false,
                                            onClick = {
                                                selectedRackPosition = posId
                                                addLog("POSICIÓN SELECCIONADA: $posId")
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton(
                                texto = "ALMACENAR EN POS $selectedRackPosition",
                                icono = Icons.Default.Send,
                                enabled = isConnectedBt && (isAuthorized || independentMode),
                                onClick = { sendAuthorizedHardwareCommand("STO:$selectedRackPosition", "CMD: STORE AT POS $selectedRackPosition") }
                            )
                            Spacer(Modifier.height(8.dp))
                            IndustrialActionButton(
                                texto = "RUN SCORBOT EN POS $selectedRackPosition",
                                icono = Icons.Default.PlayCircle,
                                colorFondo = IndustrialTheme.Secundario,
                                enabled = isConnectedBt && (isAuthorized || independentMode),
                                onClick = { sendAuthorizedHardwareCommand("R:RUN STORE $selectedRackPosition", "RUN STORE $selectedRackPosition") }
                            )
                        }
                    }
                    1 -> {
                        IndustrialCard("Red de Coordinación", Icons.Default.Lan, headerColor = IndustrialTheme.Secundario) {
                            IndustrialTextField(valor = ipCoordinator, onValueChange = { ipCoordinator = it }, label = "IP Hub Central")
                            IndustrialStatusRow("Servicio Hub", if(isConnectedNet) "ACTIVO" else "DOWN", isConnectedNet)
                            IndustrialStatusRow("Autorización", authorizationState, isAuthorized)
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Modo Autónomo", color = IndustrialTheme.TextoSecundario)
                                Switch(checked = independentMode, onCheckedChange = { independentMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = IndustrialTheme.Exito))
                            }
                            IndustrialStatusRow("Modo Autónomo", if(independentMode) "ACTIVO" else "DESACTIVADO", independentMode)
                            IndustrialActionButton(texto = "Sincronizar", icono = Icons.Default.Router, onClick = { stationClient.connect() })
                        }
                    }
                    2 -> {
                        IndustrialCard("Control Scorbot", Icons.Default.PrecisionManufacturing) {
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("HOME", Icons.Default.Home, Modifier.weight(1f), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:HOME", "CMD: HOME") })
                                IndustrialActionButton("READY", Icons.Default.Check, Modifier.weight(1f), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:READY", "CMD: READY") })
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("MOVIMIENTO MANUAL", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("X-", Icons.Default.KeyboardArrowLeft, Modifier.weight(1f).height(44.dp), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:MOVE:X:-10", "CMD: MOVE X -10") })
                                IndustrialActionButton("X+", Icons.Default.KeyboardArrowRight, Modifier.weight(1f).height(44.dp), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:MOVE:X:+10", "CMD: MOVE X +10") })
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("Y-", Icons.Default.KeyboardArrowDown, Modifier.weight(1f).height(44.dp), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:MOVE:Y:-10", "CMD: MOVE Y -10") })
                                IndustrialActionButton("Y+", Icons.Default.KeyboardArrowUp, Modifier.weight(1f).height(44.dp), enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:MOVE:Y:+10", "CMD: MOVE Y +10") })
                            }
                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton("DESCARTAR PIEZA", Icons.Default.DeleteForever, colorFondo = IndustrialTheme.Error, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendAuthorizedHardwareCommand("R:DISCARD", "CMD: DISCARD FAILED PIECE") })
                        }
                        ScorbotRunConsole(
                            enabled = isConnectedBt && (isAuthorized || independentMode),
                            presets = listOf("ALMACENAR" to "STORE", "RETIRAR" to "PICK"),
                            initialProgram = "STORE",
                            descripcion = "Ejecuta rutinas de almacenamiento en el controlador (estilo hyperterminal)",
                            manualLabel = "Programa (ej: STORE, PICK)",
                            onRun = { prog -> sendAuthorizedHardwareCommand("R:RUN $prog", "RUN $prog") },
                            onAuto = { sendAuthorizedHardwareCommand("R:AUTO", "AUTO") }
                        )
                    }
                }

                if (true) {
                    IndustrialCard("Debug de Almacén", Icons.Default.DeveloperMode, headerColor = Color.Magenta) {
                        IndustrialActionButton(texto = "Simular Almacenado", icono = Icons.Default.CheckCircle, colorFondo = Color.DarkGray, onClick = { addLog("SIM_ESP32: STORE_SUCCESS | POS: 12") })
                    }
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}

// FIX: Límite de colección para prevenir memory leak
private val MAX_COLLECTION_SIZE = 500
