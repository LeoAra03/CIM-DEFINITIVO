package com.industria.coordinacion.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import com.sistema.distribuido.network.prefecto.IndustrialCard
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialStatusRow
import com.sistema.distribuido.network.prefecto.IndustrialTextButton

data class ConnectedDevice(
    val mac: String,
    val appType: String,
    val name: String,
    val isConnected: Boolean,
    val isAuthorized: Boolean,
    val rssi: Int = 0,
    val lastSeen: Long = System.currentTimeMillis(),
    val occupant: String? = null
)

data class NetworkTabState(
    val isServerRunning: Boolean = false,
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val totalConnected: Int = 0,
    val pendingRequestCount: Int = 0,
    val pendingRequestSummary: String = "Sin solicitudes pendientes",
    val lastMessage: String = "",
    val bluetoothSummary: String = "Bluetooth: 0 conectados",
    val brokerSummary: String = "",
    val debugLogs: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val isBluetoothReconnecting: Boolean = false,
    val reconnectingMac: String? = null,
    val isAutoModeEnabled: Boolean = false
)

@Composable
fun NetworkTab(
    state: NetworkTabState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onAuthorizeDevice: (mac: String) -> Unit,
    onRejectDevice: (mac: String) -> Unit,
    onDisconnectDevice: (mac: String) -> Unit,
    onSendMessage: (String) -> Unit,
    onRefreshBluetooth: () -> Unit,
    onToggleAutoMode: (Boolean) -> Unit,
    onForceIdentify: (mac: String) -> Unit,
    onReconnectDevice: (mac: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var showDebug by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IndustrialCard("Servidor Maestro TCP", Icons.Default.Router) {
                IndustrialStatusRow("Estado Server", if(state.isServerRunning) "ESCUCHANDO" else "OFFLINE", state.isServerRunning)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialActionButton(
                        texto = "Start", 
                        icono = Icons.Default.PlayArrow, 
                        modifier = Modifier.weight(1f),
                        enabled = !state.isServerRunning,
                        onClick = onStartServer
                    )
                    IndustrialActionButton(
                        texto = "Stop", 
                        icono = Icons.Default.Stop, 
                        modifier = Modifier.weight(1f),
                        colorFondo = IndustrialTheme.Error,
                        enabled = state.isServerRunning,
                        onClick = onStopServer
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modo AUTO", color = IndustrialTheme.TextoSecundario, fontSize = 12.sp)
                    Switch(
                        checked = state.isAutoModeEnabled,
                        onCheckedChange = onToggleAutoMode,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = IndustrialTheme.Exito.copy(alpha = 0.4f),
                            checkedThumbColor = IndustrialTheme.Exito,
                            uncheckedTrackColor = Color.DarkGray,
                            uncheckedThumbColor = IndustrialTheme.TextoSecundario
                        )
                    )
                }
            }
        }

        item {
            IndustrialCard("Bluetooth y Conexiones", Icons.Default.Bluetooth) {
                Text(
                    if (state.isScanning) "Bluetooth: escaneando..." else state.bluetoothSummary,
                    color = IndustrialTheme.TextoPrincipal,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                IndustrialActionButton(
                    texto = "Refrescar Bluetooth",
                    icono = Icons.Default.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBluetoothReconnecting,
                    onClick = onRefreshBluetooth
                )
                if (state.isBluetoothReconnecting && !state.reconnectingMac.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Reconectando a ${state.reconnectingMac}...",
                        color = IndustrialTheme.Exito,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                IndustrialActionButton(
                    texto = "Abrir Debug Bluetooth",
                    icono = Icons.Default.BugReport,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDebug = true }
                )
                Spacer(Modifier.height(8.dp))
                if (state.pendingRequestCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IndustrialTheme.Error.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .border(1.dp, IndustrialTheme.Error, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "${state.pendingRequestCount} solicitud(es) pendientes de autorización",
                                color = IndustrialTheme.Error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(state.pendingRequestSummary, color = IndustrialTheme.TextoSecundario, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (state.brokerSummary.isNotBlank()) {
                    Text(state.brokerSummary, color = IndustrialTheme.TextoSecundario, fontSize = 12.sp)
                }
            }
        }

        item {
            IndustrialCard("Enviar comando TCP", Icons.Default.Send) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Mensaje TCP")
                    }
                )
                Spacer(Modifier.height(8.dp))
                IndustrialActionButton(
                    texto = "Enviar",
                    icono = Icons.Default.Send,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = messageText.isNotBlank(),
                    onClick = {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text("Último mensaje: ${state.lastMessage}", color = IndustrialTheme.TextoSecundario, fontSize = 12.sp)
            }
        }

        item {
            Text("NODOS INDUSTRIALES DETECTADOS", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (state.connectedDevices.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("NO SE DETECTAN NODOS ACTIVOS", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        } else {
            items(state.connectedDevices) { device ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndustrialTheme.Tarjeta, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .border(1.dp, IndustrialTheme.Borde, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, "Dispositivo conectado", tint = IndustrialTheme.Primario, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(device.mac, color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            Text(device.appType, color = IndustrialTheme.Primario, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(if(device.isConnected) IndustrialTheme.Exito else IndustrialTheme.Error, androidx.compose.foundation.shape.CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(if(device.isConnected) "ONLINE" else "OFFLINE", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("RSSI: ${device.rssi} dBm", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IndustrialActionButton(
                                texto = if (device.isAuthorized) "Desconectar" else "Autorizar",
                                icono = if (device.isAuthorized) Icons.Default.LinkOff else Icons.Default.Check,
                                modifier = Modifier.weight(1f).height(36.dp),
                                onClick = {
                                    if (device.isAuthorized) {
                                        onDisconnectDevice(device.mac)
                                    } else {
                                        onAuthorizeDevice(device.mac)
                                    }
                                }
                            )
                            if (!device.isAuthorized) {
                                IndustrialActionButton(
                                    texto = "Rechazar",
                                    icono = Icons.Default.Close,
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colorFondo = IndustrialTheme.Error,
                                    onClick = { onRejectDevice(device.mac) }
                                )
                            } else {
                                IndustrialActionButton(
                                    texto = "Forzar Reconexión",
                                    icono = Icons.Default.Refresh,
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    enabled = !state.isBluetoothReconnecting || state.reconnectingMac != device.mac,
                                    onClick = { onReconnectDevice(device.mac) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDebug) {
        AlertDialog(onDismissRequest = { showDebug = false }, title = { Text("Bluetooth Debug", color = IndustrialTheme.Primario) }, text = {
            BluetoothDebugTab(
                state = state,
                logs = state.debugLogs,
                onRefresh = onRefreshBluetooth,
                onForceIdentify = onForceIdentify,
                onReconnect = onReconnectDevice,
                onDisconnect = onDisconnectDevice,
                modifier = Modifier.fillMaxWidth().height(400.dp)
            )
        }, confirmButton = {
            IndustrialTextButton(
                texto = "Cerrar",
                textColor = IndustrialTheme.TextoPrincipal,
                onClick = { showDebug = false }
            )
        })
    }
}
