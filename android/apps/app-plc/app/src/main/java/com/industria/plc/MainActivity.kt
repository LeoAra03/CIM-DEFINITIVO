// FIX: Constantes extraídas
/**
 * MainActivity
 * @author CIM Team
 */
package com.industria.plc

import android.Manifest
import kotlinx.coroutines.withTimeout
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
                val p = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    p.addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                } else {
                    p.addAll(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
    val discoveredHubIp = rememberHubIp(context)
    LaunchedEffect(discoveredHubIp.value) {
        discoveredHubIp.value?.let { ip ->
            if (ip != ipCoordinator) ipCoordinator = ip
        }
    }
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
            addLog("[ERR] No autorizado - activar modo autónomo o esperar VALIDADO por coordinador")
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
            stationUuid = "CIM-ST-PLC-X4",
            macAddress = AppIdentifier.getInstance().deviceMac
        ).apply {
            onLog = { msg -> logs.add(0, "[NET] $msg") }
            onStatusChanged = { isConnectedNet = it }
            onAuthorizationStateChanged = { authorizationState = it }
            onCommandReceived = { cmd -> scope.launch { handlePlcEvent(cmd) } }
        }
    }

    val manager = remember { PlcStationManager(context) }

    // Anti-softlock global: back retrocede de pestaña en vez de cerrar la app
    androidx.activity.compose.BackHandler(enabled = selectedTab > 0) {
        selectedTab -= 1
    }
    LaunchedEffect(stationClient, isAuthorized) {
        // No hay un CommandBroker real en el manager que use StationClient, 
        // pero podemos inyectar un shim o manejarlo directamente aquí.
        // Para consistencia, el manager debería usar el stationClient.
    }

    IndustrialScaffold(
        titulo = "PLC Master v6.0", 
        subtitulo = "CONTROL DE CINTA TRANSPORTADORA",
        estado = {
            // Chip de estado en cabecera (punto + texto), como en los mockups HMI.
            val enLinea = isConnectedBt || isConnectedNet
            IndustrialStatusChip(
                texto = when {
                    independentMode -> "AUTONOMO"
                    enLinea -> "RUNNING"
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
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            IndustrialTabBar(
                items = listOf("CONTROL", "TRACKING", "SINCRO"),
                seleccion = selectedTab,
                onSelect = { selectedTab = it },
                scrollable = false
            )

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        val isActive = isConnectedBt && (isAuthorized || independentMode)
                        // Tarjeta principal "Conveyor Belt" del mockup: chip de estado a la
                        // derecha, par START/STOP y pie con métricas Speed / Mode.
                        IndustrialCard(
                            titulo = "Cinta transportadora",
                            icono = Icons.Default.PowerSettingsNew,
                            subtitulo = "Control principal",
                            trailing = {
                                IndustrialStatusChip(
                                    texto = if (isActive) "RUNNING" else "STANDBY",
                                    color = if (isActive) IndustrialTheme.Primario else IndustrialTheme.TextoTenue
                                )
                            }
                        ) {
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                IndustrialActionButton(texto = "START", icono = Icons.Default.PlayArrow, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Primario, enabled = isActive, onClick = { sendPlcHardwareCommand("PLC:START", "PLC: START") })
                                IndustrialActionButton(texto = "STOP", icono = Icons.Default.Stop, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Error, enabled = isActive, onClick = { sendPlcHardwareCommand("PLC:STOP", "PLC: STOP") })
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = IndustrialTheme.Borde)
                            Spacer(Modifier.height(10.dp))

                            // Pie de métricas: dos columnas icono + etiqueta + valor.
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, null, Modifier.size(20.dp), tint = IndustrialTheme.Secundario)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("ESTADO", color = IndustrialTheme.TextoSecundario, fontSize = 9.sp, letterSpacing = 0.6.sp)
                                        Text(
                                            if (isActive) "VINCULADO" else "STANDBY",
                                            color = if (isActive) IndustrialTheme.Primario else IndustrialTheme.TextoSecundario,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(Modifier.width(1.dp).height(30.dp).background(IndustrialTheme.Borde))
                                Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Settings, null, Modifier.size(20.dp), tint = IndustrialTheme.Secundario)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("MODO", color = IndustrialTheme.TextoSecundario, fontSize = 9.sp, letterSpacing = 0.6.sp)
                                        Text(
                                            if (independentMode) "AUTÓNOMO" else "COORDINADO",
                                            color = if (independentMode) IndustrialTheme.Advertencia else IndustrialTheme.Secundario,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Modo autónomo (sin coordinador)", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp)
                                Switch(
                                    checked = independentMode,
                                    onCheckedChange = { independentMode = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IndustrialTheme.TextoPrincipal,
                                        checkedTrackColor = IndustrialTheme.Primario,
                                        uncheckedThumbColor = IndustrialTheme.TextoSecundario,
                                        uncheckedTrackColor = IndustrialTheme.TarjetaAlta
                                    )
                                )
                            }
                        }

                        IndustrialCard("Matriz de distribución", Icons.Default.GridView, subtitulo = "Origen › destino (3x10)") {
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
                        ArcadeConveyor(stations = trackingStations, palletPresent = palletPresent, lastEvent = lastTrackingEvent)
                        Spacer(Modifier.height(8.dp))
                        IndustrialGauge(
                            label = "OCUPACIÓN DE CINTA",
                            fraction = (palletPresent.values.count { it }.toFloat() / trackingStations.size.coerceAtLeast(1)),
                            color = IndustrialTheme.Exito
                        )
                        Spacer(Modifier.height(4.dp))
                        IndustrialCard("Tracking de Pallets", Icons.Default.Sensors, headerColor = IndustrialTheme.Secundario) {
                            IndustrialStatusRow("Último evento", lastTrackingEvent, true)
                            Text("Activa 'Detener' para frenar el pallet cuando pase por la estación", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                            trackingStations.forEach { (name, pos) ->
                                val present = palletPresent[pos] == true
                                val hold = holdStations[pos] == true
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("$pos · $name", color = IndustrialTheme.TextoPrincipal, fontWeight = FontWeight.Bold)
                                        Text(if (present) "PALLET" else "vacío", color = if (present) IndustrialTheme.Exito else IndustrialTheme.TextoSecundario, fontSize = 12.sp)
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
                        IndustrialCard("Simulador de pallet", Icons.Default.DirectionsRun, headerColor = IndustrialTheme.Secundario) {
                            Text("Simula el paso de un pallet por una estación (pruebas sin hardware)", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                trackingStations.forEach { (_, pos) ->
                                    IndustrialActionButton("POS $pos", Icons.Default.Sensors, Modifier.weight(1f), onClick = { handlePlcEvent("SENSOR_ACTIVATED|POS:$pos") })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            IndustrialActionButton("SIMULAR FLUJO ARCADE COMPLETO", Icons.Default.PlayArrow, colorFondo = IndustrialTheme.Secundario, onClick = {
                                scope.launch {
                                    trackingStations.forEach { (_, pos) ->
                                        handlePlcEvent("SENSOR_ACTIVATED|POS:$pos")
                                        addLog("ARCADE: pallet en estación $pos")
                                        kotlinx.coroutines.delay(1200)
                                    }
                                    kotlinx.coroutines.delay(1500)
                                    trackingStations.forEach { (_, pos) ->
                                        handlePlcEvent("PALLET_CLEARED|POS:$pos")
                                        addLog("ARCADE: estación $pos liberada")
                                    }
                                }
                            })
                        }
                    }
                    2 -> {
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

                IndustrialCard("Simulador de sensor", Icons.Default.Sensors, headerColor = IndustrialTheme.Secundario) {
                    IndustrialActionButton(texto = "Simular Sensor Activo", icono = Icons.Default.CheckCircle, colorFondo = IndustrialTheme.TarjetaAlta, onClick = { 
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
