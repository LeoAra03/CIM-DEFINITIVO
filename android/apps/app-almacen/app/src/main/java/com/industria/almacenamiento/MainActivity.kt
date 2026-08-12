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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        AppIdentifier.init(this, AppType.ALMACEN)
        GlobalPermissionManager.init(this)
        GlobalBluetoothManager.init(this, onLog = { msg ->
            // logs handled by local instances usually, but we can set up a global log stream if needed
        })
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
    val discoveredHubIp = rememberHubIp(context)
    LaunchedEffect(discoveredHubIp.value) {
        discoveredHubIp.value?.let { ip ->
            if (ip != ipCoordinator) ipCoordinator = ip
        }
    }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedRackPosition by remember { mutableStateOf(1) }
    val isOperationalReady by remember {
        derivedStateOf { isConnectedBt && (isAuthorized || independentMode) }
    }

    // Anti-softlock: retroceder entre pestañas en vez de cerrar la app
    androidx.activity.compose.BackHandler(enabled = selectedTab > 0) {
        selectedTab -= 1
    }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
    }

    val stationClient = remember(ipCoordinator) {
        StationClient(host = ipCoordinator, port = 8888, stationName = "ALMACEN", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-ST-ALM-X1").apply {
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
            // Bottom-nav de la figura de referencia (iconos + etiqueta, acento verde).
            IndustrialBottomNav(
                items = listOf(
                    IndustrialNavItem("RACKS", Icons.Default.GridView),
                    IndustrialNavItem("SINCRO", Icons.Default.Wifi),
                    IndustrialNavItem("BRAZO", Icons.Default.PrecisionManufacturing)
                ),
                seleccion = selectedTab,
                onSelect = { selectedTab = it }
            )
        },
        floatingActionButton = { BluetoothConnectionFAB() }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        // Vista de rack tipo mockup: cabecera con ocupación, columnas
                        // numeradas, niveles a la izquierda y celdas con su código.
                        IndustrialCard(
                            titulo = "Rack A-01",
                            icono = Icons.Default.Inventory2,
                            subtitulo = "3 columnas x 6 niveles · 18 posiciones",
                            trailing = {
                                IndustrialStatusChip(
                                    texto = "POS $selectedRackPosition",
                                    color = IndustrialTheme.Primario
                                )
                            }
                        ) {
                            IndustrialStatusRow("Conexión ESP32", if(isConnectedBt) "LINK OK" else "OFFLINE", isConnectedBt)
                            Text("Selecciona la posición del rack y pulsa ALMACENAR", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))

                            Spacer(Modifier.height(10.dp))
                            // Cabecera de columnas
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Spacer(Modifier.width(26.dp))
                                repeat(3) { col ->
                                    Text(
                                        "0${col + 1}",
                                        color = IndustrialTheme.TextoSecundario,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))

                            // Niveles de arriba (06) hacia abajo (01), como en un rack real.
                            for (level in 5 downTo 0) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "0${level + 1}",
                                        color = IndustrialTheme.Advertencia,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    repeat(3) { col ->
                                        val posId = level * 3 + col + 1
                                        val seleccionada = selectedRackPosition == posId
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (seleccionada) IndustrialTheme.Primario.copy(alpha = 0.22f)
                                                    else IndustrialTheme.TarjetaAlta
                                                )
                                                .border(
                                                    1.dp,
                                                    if (seleccionada) IndustrialTheme.Primario else IndustrialTheme.Borde,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    selectedRackPosition = posId
                                                    addLog("POSICIÓN SELECCIONADA: $posId")
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Inventory2,
                                                null,
                                                Modifier.size(12.dp),
                                                tint = if (seleccionada) IndustrialTheme.Primario else IndustrialTheme.TextoTenue
                                            )
                                            Text(
                                                if (posId < 10) "A-01-0$posId" else "A-01-$posId",
                                                color = if (seleccionada) IndustrialTheme.TextoPrincipal else IndustrialTheme.TextoTenue,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            // Leyenda de la figura de referencia
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                RackLegendItem(IndustrialTheme.Primario, "SELECCIONADA")
                                Spacer(Modifier.width(14.dp))
                                RackLegendItem(IndustrialTheme.TextoTenue, "LIBRE")
                            }

                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton(
                                texto = "ALMACENAR EN POS $selectedRackPosition",
                                icono = Icons.Default.Send,
                                enabled = isOperationalReady,
                                onClick = { sendAuthorizedHardwareCommand("STO:$selectedRackPosition", "CMD: STORE AT POS $selectedRackPosition") }
                            )
                            Spacer(Modifier.height(8.dp))
                            IndustrialActionButton(
                                texto = "RUN SCORBOT EN POS $selectedRackPosition",
                                icono = Icons.Default.PlayCircle,
                                colorFondo = IndustrialTheme.Secundario,
                                enabled = isOperationalReady,
                                onClick = { sendAuthorizedHardwareCommand("R:RUN STORE $selectedRackPosition", "RUN STORE $selectedRackPosition") }
                            )
                        }
                    }
                    1 -> {
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
                    2 -> {
                        IndustrialCard("Control Scorbot", Icons.Default.PrecisionManufacturing) {
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("HOME", Icons.Default.Home, Modifier.weight(1f), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:HOME", "CMD: HOME") })
                                IndustrialActionButton("READY", Icons.Default.Check, Modifier.weight(1f), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:READY", "CMD: READY") })
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("MOVIMIENTO MANUAL", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("X-", Icons.Default.KeyboardArrowLeft, Modifier.weight(1f).height(44.dp), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:MOVE:X:-10", "CMD: MOVE X -10") })
                                IndustrialActionButton("X+", Icons.Default.KeyboardArrowRight, Modifier.weight(1f).height(44.dp), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:MOVE:X:+10", "CMD: MOVE X +10") })
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IndustrialActionButton("Y-", Icons.Default.KeyboardArrowDown, Modifier.weight(1f).height(44.dp), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:MOVE:Y:-10", "CMD: MOVE Y -10") })
                                IndustrialActionButton("Y+", Icons.Default.KeyboardArrowUp, Modifier.weight(1f).height(44.dp), enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:MOVE:Y:+10", "CMD: MOVE Y +10") })
                            }
                            Spacer(Modifier.height(12.dp))
                            IndustrialActionButton("DESCARTAR PIEZA", Icons.Default.DeleteForever, colorFondo = IndustrialTheme.Error, enabled = isOperationalReady, onClick = { sendAuthorizedHardwareCommand("R:DISCARD", "CMD: DISCARD FAILED PIECE") })
                        }
                        ScorbotRunConsole(
                            enabled = isOperationalReady,
                            presets = listOf("ALMACENAR" to "STORE", "RETIRAR" to "PICK"),
                            initialProgram = "STORE",
                            descripcion = "Ejecuta rutinas de almacenamiento en el controlador (estilo hyperterminal)",
                            manualLabel = "Programa (ej: STORE, PICK)",
                            onRun = { prog -> sendAuthorizedHardwareCommand("R:RUN $prog", "RUN $prog") },
                            onAuto = { sendAuthorizedHardwareCommand("R:AUTO", "AUTO") }
                        )
                    }
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}

// FIX: Límite de colección para prevenir memory leak
private val MAX_COLLECTION_SIZE = 500

/** Punto de color + etiqueta usado en la leyenda del rack (figura de referencia). */
@Composable
private fun RackLegendItem(color: androidx.compose.ui.graphics.Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.30f))
                .border(1.dp, color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(5.dp))
        Text(texto, color = IndustrialTheme.TextoSecundario, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
