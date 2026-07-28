package com.industria.plc

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import com.sistema.distribuido.network.AppIdentifier
import com.sistema.distribuido.network.CommunicationCoordinator
import com.sistema.distribuido.network.GlobalBluetoothManager
import com.sistema.distribuido.network.GlobalPermissionManager
import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.prefecto.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var commCoordinator: CommunicationCoordinator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppIdentifier.init(this, AppType.PLC)
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
            PLCApp(commCoordinator)
        }
    }
}

@Composable
fun PLCApp(commCoordinator: CommunicationCoordinator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var isConnectedNet by remember { mutableStateOf(false) }
    var authorizationState by remember { mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED) }
    val isAuthorized by remember { derivedStateOf { authorizationState == CimProtocol.AUTH_STATE_VALIDATED } }
    var independentMode by remember { mutableStateOf(false) }
    var ipCoordinator by remember { mutableStateOf("192.168.1.100") }
    var selectedTab by remember { mutableStateOf(0) }
    val palletPresent = remember { mutableStateMapOf<Int, Boolean>() }
    val holdStations = remember { mutableStateMapOf<Int, Boolean>() }
    var lastTrackingEvent by remember { mutableStateOf("--") }
    val trackingStations = listOf("ALMACEN" to 1, "MANUFACTURA" to 2, "CALIDAD" to 3, "PLC/SALIDA" to 4)

    val bluetoothManager = GlobalBluetoothManager.getInstance()
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val isConnectedBt by remember { derivedStateOf { connectionStates.values.any { it } } }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
    }

    fun sendPlcHardwareCommand(command: String, logText: String) {
        if (!isAuthorized && !independentMode) {
            addLog("✗ No autorizado - activar modo autónomo o esperar VALIDADO por coordinador")
            return
        }
        bluetoothManager.send(command, requireAuthorization = !independentMode, authorized = isAuthorized)
        if (isAuthorized) {
            scope.launch {
                commCoordinator.routeCommand(AppIdentifier.getInstance().deviceMac, command)
            }
        }
        addLog(if (independentMode) "[AUTÓNOMO] $logText" else logText)
    }

    fun handlePlcEvent(raw: String) {
        val cmd = raw.trim()
        val pos = Regex("POS:(\\d+)").find(cmd)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        when {
            cmd.startsWith("SENSOR_ACTIVATED") -> {
                palletPresent[pos] = true
                lastTrackingEvent = "Pallet detectado en estación $pos"
                if (holdStations[pos] == true) {
                    sendPlcHardwareCommand("C:STOP|$pos", "PALLET DETENIDO en estación $pos")
                } else {
                    addLog("TRACKING: pallet pasa por estación $pos")
                }
            }
            cmd.startsWith("PALLET_CLEARED") -> {
                palletPresent[pos] = false
                lastTrackingEvent = "Estación $pos liberada"
                addLog("TRACKING: estación $pos liberada")
            }
        }
    }

    val stationClient = remember(ipCoordinator) {
        com.sistema.distribuido.network.StationClient(
            host = ipCoordinator,
            port = 8888,
            stationName = "PLC",
            password = CimProtocol.PASSWORD_ACTUAL,
            stationUuid = "CIM-PLC-04",
            macAddress = "CIM-PLC-04"
        ).apply {
            onLog = { msg -> logs.add(0, "[NET] $msg") }
            onStatusChanged = { isConnectedNet = it }
            onAuthorizationStateChanged = { authorizationState = it }
            onCommandReceived = { cmd -> scope.launch { handlePlcEvent(cmd) } }
        }
    }

    val manager = remember { PlcStationManager(context) }
    LaunchedEffect(stationClient, isAuthorized) {
        // No hay un CommandBroker real en el manager que use StationClient, 
        // pero podemos inyectar un shim o manejarlo directamente aquí.
        // Para consistencia, el manager debería usar el stationClient.
    }

    IndustrialScaffold(
        titulo = "PLC Master v6.0", 
        subtitulo = "CONTROL DE CINTA TRANSPORTADORA",
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = Color.Black, contentColor = IndustrialTheme.Primario, edgePadding = 16.dp, divider = {}) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("CONTROL", fontSize = 12.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("TRACKING", fontSize = 12.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("SINCRO", fontSize = 12.sp) })
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Energía y Sistema", Icons.Default.PowerSettingsNew) {
                            val isActive = isConnectedBt && (isAuthorized || independentMode)
                            IndustrialStatusRow("Estado Operativo", if(isActive) "SISTEMA VINCULADO" else "STANDBY (BT REQUERIDO)", isActive)
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Modo Autónomo", color = IndustrialTheme.TextoSecundario)
                                Switch(checked = independentMode, onCheckedChange = { independentMode = it }, colors = SwitchDefaults.colors(checkedThumbColor = IndustrialTheme.Exito))
                            }
                            IndustrialStatusRow("Modo Autónomo", if(independentMode) "ACTIVO" else "DESACTIVADO", independentMode)
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                IndustrialActionButton(texto = "Arranque", icono = Icons.Default.PlayArrow, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Exito, enabled = isActive, onClick = { sendPlcHardwareCommand("PLC:START", "PLC: START") })
                                IndustrialActionButton(texto = "Parada", icono = Icons.Default.Stop, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Error, enabled = isActive, onClick = { sendPlcHardwareCommand("PLC:STOP", "PLC: STOP") })
                            }
                        }

                        IndustrialCard("Matriz de Distribución (3x10)", Icons.Default.GridView) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(3) { fromIdx ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(10) { toIdx ->
                                            val stationFrom = fromIdx + 1
                                            val stationTo = toIdx + 1
                                            IndustrialActionButton(
                                                texto = "$stationFrom>$stationTo",
                                                icono = Icons.Default.Send,
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                colorFondo = if(isConnectedBt && (isAuthorized || independentMode)) IndustrialTheme.Primario.copy(alpha = 0.3f) else IndustrialTheme.Tarjeta,
                                                enabled = isConnectedBt && (isAuthorized || independentMode),
                                                buttonHeight = 34.dp,
                                                fillMaxWidth = false,
                                                onClick = {
                                                    if (isConnectedBt) {
                                                        bluetoothManager.send("C:DELIVER|$stationFrom|$stationTo", requireAuthorization = !independentMode, authorized = isAuthorized)
                                                        addLog(if (independentMode) "[AUTÓNOMO] CMD: C:DELIVER $stationFrom -> $stationTo" else "CMD: C:DELIVER $stationFrom -> $stationTo")
                                                    }
                                                    if (isConnectedNet && isAuthorized) {
                                                        scope.launch {
                                                            stationClient.sendSafe("C:DELIVER|$stationFrom|$stationTo")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        IndustrialCard("Tracking de Pallets", Icons.Default.Sensors, headerColor = IndustrialTheme.Secundario) {
                            IndustrialStatusRow("Último evento", lastTrackingEvent, true)
                            Text("Activa 'Detener' para frenar el pallet cuando pase por la estación", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                            trackingStations.forEach { (name, pos) ->
                                val present = palletPresent[pos] == true
                                val hold = holdStations[pos] == true
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("$pos · $name", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(if (present) "● PALLET" else "○ vacío", color = if (present) IndustrialTheme.Exito else IndustrialTheme.TextoSecundario, fontSize = 12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Detener", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp)
                                            Switch(checked = hold, onCheckedChange = { holdStations[pos] = it; addLog("TRACKING: estación $pos ${if(it) "se detendrá" else "paso libre"}") }, colors = SwitchDefaults.colors(checkedThumbColor = IndustrialTheme.Advertencia))
                                        }
                                        IndustrialActionButton("STOP", Icons.Default.Stop, Modifier.weight(1f), colorFondo = IndustrialTheme.Error, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { sendPlcHardwareCommand("C:STOP|$pos", "PALLET DETENIDO en estación $pos") })
                                        IndustrialActionButton("Liberar", Icons.Default.PlayArrow, Modifier.weight(1f), colorFondo = IndustrialTheme.Exito, enabled = isConnectedBt && (isAuthorized || independentMode), onClick = { palletPresent[pos] = false; sendPlcHardwareCommand("C:FREE|$pos", "Estación $pos liberada") })
                                    }
                                }
                                HorizontalDivider(color = IndustrialTheme.Borde)
                            }
                        }
                        IndustrialCard("Simulador de Pallet", Icons.Default.DirectionsRun, headerColor = Color.Magenta) {
                            Text("Simula el paso de un pallet por una estación (pruebas sin hardware)", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                trackingStations.forEach { (_, pos) ->
                                    IndustrialActionButton("POS $pos", Icons.Default.Sensors, Modifier.weight(1f), onClick = { handlePlcEvent("SENSOR_ACTIVATED|POS:$pos") })
                                }
                            }
                        }
                    }
                    2 -> {
                        IndustrialCard("Red Industrial", Icons.Default.Lan, headerColor = IndustrialTheme.Secundario) {
                            IndustrialTextField(valor = ipCoordinator, onValueChange = { ipCoordinator = it }, label = "IP Coordinador")
                            IndustrialStatusRow("Enlace de Datos", if(isConnectedNet) "SINCRO OK" else "OFFLINE", isConnectedNet)
                            IndustrialStatusRow("Autorización", authorizationState, isAuthorized)
                            IndustrialActionButton(texto = "Vincular al Hub", icono = Icons.Default.Router, onClick = { stationClient.connect() })
                        }
                    }
                }

                IndustrialCard("Simulador de Sensor", Icons.Default.Sensors, headerColor = Color.Magenta) {
                    IndustrialActionButton(texto = "Simular Sensor Activo", icono = Icons.Default.CheckCircle, colorFondo = Color.DarkGray, onClick = { 
                        if (isAuthorized) {
                            scope.launch {
                                stationClient.sendEventSafe("SENSOR_ACTIVATED|POS:5")
                            }
                        }
                        handlePlcEvent("SENSOR_ACTIVATED|POS:5")
                        addLog("SIM_ESP32: SENSOR_ACTIVATED | POS: 5") 
                    })
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}

// FIX #107: Deduplicación de logs
private var lastLogMessage: String = ""

fun addLogWithDeduplication(msg: String) {
    if (msg != lastLogMessage) {
        lastLogMessage = msg
        addLog(msg)
    }
}

// FIX #82: Límite de logs
private val MAX_LOG_SIZE = 500
