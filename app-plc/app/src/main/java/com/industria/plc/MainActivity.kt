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
import com.sistema.distribuido.network.AppIdentifier
import com.sistema.distribuido.network.GlobalBluetoothManager
import com.sistema.distribuido.network.GlobalPermissionManager
import com.sistema.distribuido.network.protocol.CimProtocol
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.prefecto.*
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
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
            PLCApp()
        }
    }
}

@Composable
fun PLCApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var isConnectedNet by remember { mutableStateOf(false) }
    var authorizationState by remember { mutableStateOf(CimProtocol.AUTH_STATE_DISCONNECTED) }
    val isAuthorized by remember { derivedStateOf { authorizationState == CimProtocol.AUTH_STATE_VALIDATED } }
    var ipCoordinator by remember { mutableStateOf("192.168.1.100") }
    var selectedTab by remember { mutableStateOf(0) }

    val bluetoothManager = GlobalBluetoothManager.getInstance()
    val connectionStates by bluetoothManager.connectionStates.collectAsState()
    val isConnectedBt by remember { derivedStateOf { connectionStates.values.any { it } } }

    fun addLog(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add(0, "[$time] $msg")
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
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("SINCRO", fontSize = 12.sp) })
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Energía y Sistema", Icons.Default.PowerSettingsNew) {
                            val isActive = isConnectedNet && isAuthorized && isConnectedBt
                            IndustrialStatusRow("Estado Operativo", if(isActive) "SISTEMA VINCULADO" else "STANDBY (BT/NET REQ)", isActive)
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                                IndustrialActionButton(texto = "Arranque", icono = Icons.Default.PlayArrow, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Exito, enabled = isActive, onClick = { addLog("SISTEMA: Power On Sequence Initiated") })
                                IndustrialActionButton(texto = "Parada", icono = Icons.Default.Stop, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Error, enabled = isActive, onClick = { addLog("SISTEMA: Emergency Stop Engaged") })
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
                                                colorFondo = if(isConnectedNet && isAuthorized) IndustrialTheme.Primario.copy(alpha = 0.3f) else IndustrialTheme.Tarjeta,
                                                enabled = isConnectedNet && isAuthorized,
                                                buttonHeight = 34.dp,
                                                fillMaxWidth = false,
                                                onClick = { 
                                                    scope.launch {
                                                        stationClient.sendSafe("C:DELIVER|$stationFrom|$stationTo")
                                                    }
                                                    addLog("CMD: C:DELIVER $stationFrom -> $stationTo")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
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
                        addLog("SIM_ESP32: SENSOR_ACTIVATED | POS: 5") 
                    })
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}
