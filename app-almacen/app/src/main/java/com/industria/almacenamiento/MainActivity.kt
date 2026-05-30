package com.industria.almacenamiento

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.*
import com.sistema.distribuido.network.prefecto.*
import com.sistema.distribuido.network.protocol.AppType
import com.sistema.distribuido.network.protocol.CimProtocol
import java.util.*

class MainActivity : ComponentActivity() {
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
            AlmacenApp()
        }
    }
}

@Composable
fun AlmacenApp() {
    val context = LocalContext.current
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

    val stationClient = remember {
        StationClient(host = ipCoordinator, port = 8888, stationName = "ALMACEN", password = CimProtocol.PASSWORD_ACTUAL, stationUuid = "CIM-ALM-01").apply {
            onLog = { msg -> logs.add(0, "[NET] $msg") }
            onStatusChanged = { isConnectedNet = it }
            onAuthorizationStateChanged = { authorizationState = it }
        }
    }

    fun sendAuthorizedHardwareCommand(command: String, logText: String) {
        if (!isAuthorized) {
            addLog("✗ No autorizado - esperar VALIDADO por coordinador")
            return
        }
        bt.send(command)
        addLog(logText)
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
            }

            Column(Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        IndustrialCard("Matriz de Almacén (18 POS)", Icons.Default.Inventory2) {
                            IndustrialStatusRow("Conexión ESP32", if(isConnectedBt) "LINK OK" else "OFFLINE", isConnectedBt)
                            
                            repeat(3) { level ->
                                Text("NIVEL ${level + 1}", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(6) { col ->
                                        val posId = level * 6 + col + 1
                                        IndustrialActionButton(
                                            texto = "$posId",
                                            icono = Icons.Default.Inventory2,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colorFondo = if(isConnectedBt && isAuthorized) IndustrialTheme.Primario.copy(alpha = 0.3f) else IndustrialTheme.Tarjeta,
                                            enabled = isConnectedBt && isAuthorized,
                                            buttonHeight = 36.dp,
                                            fillMaxWidth = false,
                                            onClick = { sendAuthorizedHardwareCommand("STO:$posId", "CMD: STORE AT POS $posId") }
                                        )
                                    }
                                }
                            }
                            
                            if (!isConnectedBt) {
                                Spacer(Modifier.height(16.dp))
                                IndustrialActionButton("Conectar ESP32", Icons.Default.Bluetooth, onClick = { bt.startScan() })
                            }
                        }
                    }
                    1 -> {
                        IndustrialCard("Red de Coordinación", Icons.Default.Lan, headerColor = IndustrialTheme.Secundario) {
                            IndustrialTextField(valor = ipCoordinator, onValueChange = { ipCoordinator = it }, label = "IP Hub Central")
                            IndustrialStatusRow("Servicio Hub", if(isConnectedNet) "ACTIVO" else "DOWN", isConnectedNet)
                            IndustrialStatusRow("Autorización", authorizationState, isAuthorized)
                            IndustrialActionButton(texto = "Sincronizar", icono = Icons.Default.Router, onClick = { stationClient.connect() })
                        }
                    }
                }

                IndustrialCard("Simulador de Respuesta", Icons.Default.DeveloperMode, headerColor = Color.Magenta) {
                    IndustrialActionButton(texto = "Simular Almacenado", icono = Icons.Default.CheckCircle, colorFondo = Color.DarkGray, onClick = { addLog("SIM_ESP32: STORE_SUCCESS | POS: 12") })
                }

                IndustrialTerminal(logs = logs, modifier = Modifier.height(180.dp))
            }
        }
    }
}
