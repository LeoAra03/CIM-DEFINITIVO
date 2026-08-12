// FIX: Constantes extraídas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import com.sistema.distribuido.network.prefecto.IndustrialCard
import com.sistema.distribuido.network.prefecto.IndustrialStatusChip
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialStatusRow
import com.sistema.distribuido.network.prefecto.IndustrialTextButton
import com.sistema.distribuido.network.prefecto.DigitalTwinPanel
import com.sistema.distribuido.network.prefecto.StationTwinState

data class ConnectedDevice(
    val mac: String,
    val appType: String,
    val name: String,
    val isConnected: Boolean,
    val isAuthorized: Boolean,
    val rssi: Int = 0,
    val ip: String = "",
    val stationUuid: String = "",
    val version: String = "",
    val hardwareModel: String = "",
    val capabilities: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val occupant: String? = null
)

data class BlockedDeviceState(
    val mac: String,
    val reason: String,
    val blockedAt: Long
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
    val isAutoModeEnabled: Boolean = false,
    val blockedDevices: List<BlockedDeviceState> = emptyList()
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
    onUnbanDevice: (mac: String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IndustrialCard(
                titulo = "Servidor maestro TCP",
                icono = Icons.Default.Router,
                subtitulo = "Conexión fácil para las estaciones",
                trailing = {
                    IndustrialStatusChip(
                        texto = if (state.isServerRunning) "ONLINE" else "OFFLINE",
                        color = if (state.isServerRunning) IndustrialTheme.Primario else IndustrialTheme.TextoTenue
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if(state.isServerRunning) IndustrialTheme.Exito.copy(alpha = 0.12f) else IndustrialTheme.TarjetaAlta,
                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if(state.isServerRunning) IndustrialTheme.Exito else IndustrialTheme.Borde,
                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(if(state.isServerRunning) IndustrialTheme.Primario else IndustrialTheme.TextoTenue, androidx.compose.foundation.shape.CircleShape))
                            Text(
                                if(state.isServerRunning) "HUB ACTIVO · listo para 5 estaciones" else "HUB DETENIDO · inicia para multiconectarse",
                                color = if(state.isServerRunning) IndustrialTheme.Primario else IndustrialTheme.TextoSecundario,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "1-click: Inicia Hub → Activa AUTO → Las estaciones se conectan automáticamente por NSD",
                            color = IndustrialTheme.TextoSecundario,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                IndustrialStatusRow("Estado Server", if(state.isServerRunning) "ESCUCHANDO :8888" else "OFFLINE", state.isServerRunning)
                IndustrialStatusRow("NSD Descubrimiento", "_cim-hub._tcp. ${if(state.isServerRunning) "publicado ✓" else "detenido"}", state.isServerRunning)
                IndustrialStatusRow("Conectados", "${state.totalConnected} / 5 estaciones", state.totalConnected>0)
                IndustrialStatusRow("Pendientes Auth", "${state.pendingRequestCount}", state.pendingRequestCount==0)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialActionButton(
                        texto = if(state.isServerRunning) "ON :8888" else "▶ Iniciar Hub",
                        icono = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f),
                        enabled = enabled && !state.isServerRunning,
                        onClick = onStartServer
                    )
                    IndustrialActionButton(
                        texto = "■ Detener",
                        icono = Icons.Default.Stop,
                        modifier = Modifier.weight(1f),
                        colorFondo = IndustrialTheme.Error,
                        enabled = enabled && state.isServerRunning,
                        onClick = onStopServer
                    )
                }
                Spacer(Modifier.height(10.dp))
                // Quick actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialActionButton(
                        texto = "🔗 Autorizar Todo",
                        icono = Icons.Default.DoneAll,
                        modifier = Modifier.weight(1f),
                        colorFondo = IndustrialTheme.Exito,
                        enabled = enabled && state.pendingRequestCount>0,
                        onClick = { state.connectedDevices.filter{!it.isAuthorized}.forEach{ onAuthorizeDevice(it.mac) } }
                    )
                    IndustrialActionButton(
                        texto = "🔍 Refrescar",
                        icono = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        onClick = onRefreshBluetooth
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Modo AUTO (Laboratorio)", color = IndustrialTheme.TextoPrincipal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Aprueba automáticamente sin diálogo", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                    }
                    Switch(
                        checked = state.isAutoModeEnabled,
                        onCheckedChange = onToggleAutoMode,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = IndustrialTheme.Exito.copy(alpha = 0.4f),
                            checkedThumbColor = IndustrialTheme.Exito,
                            uncheckedTrackColor = IndustrialTheme.TarjetaAlta,
                            uncheckedThumbColor = IndustrialTheme.TextoSecundario
                        )
                    )
                }
            }
        }

        item {
            IndustrialCard("Gemelo Digital", Icons.Default.ViewInAr) {
                DigitalTwinPanel(
                    stationStates = mapOf(
                        "PLC" to StationTwinState("Cinta activa", IndustrialTheme.Primario),
                        "MAN" to StationTwinState("Robot HOME", IndustrialTheme.Secundario),
                        "CAL" to StationTwinState("Inspección", IndustrialTheme.Advertencia, isTarget = true),
                        "ALM" to StationTwinState("Slot 12", IndustrialTheme.PrimarioBrillante)
                    )
                )
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
                        enabled = enabled && !state.isBluetoothReconnecting,
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
                    enabled = enabled && messageText.isNotBlank(),
                    onClick = {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text("Último mensaje: ${state.lastMessage}", color = IndustrialTheme.TextoSecundario, fontSize = 12.sp)
            }
        }

        if (state.blockedDevices.isNotEmpty()) {
            item {
                IndustrialCard("Dispositivos Bloqueados (${state.blockedDevices.size})", Icons.Default.Block, headerColor = IndustrialTheme.Error) {
                    Text(
                        "Estos nodos se rechazan antes de solicitar autorización.",
                        color = IndustrialTheme.TextoSecundario,
                        fontSize = 11.sp
                    )
                    state.blockedDevices.forEach { blocked ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(blocked.mac, color = IndustrialTheme.TextoPrincipal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(blocked.reason, color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            }
                            IndustrialActionButton(
                                texto = "Desbloquear",
                                icono = Icons.Default.LockOpen,
                                modifier = Modifier.height(34.dp),
                                enabled = enabled,
                                onClick = { onUnbanDevice(blocked.mac) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("NODOS INDUSTRIALES DETECTADOS", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (state.connectedDevices.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("NO SE DETECTAN NODOS ACTIVOS", color = IndustrialTheme.TextoTenue, fontSize = 12.sp)
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
                                Text(device.name, color = IndustrialTheme.TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(device.mac, color = IndustrialTheme.TextoSecundario, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                val stationUuidText = device.stationUuid.ifBlank { "no informado" }
                                val versionText = device.version.ifBlank { "?" }
                                Text(
                                    "UUID: $stationUuidText · v$versionText",
                                    color = IndustrialTheme.TextoSecundario,
                                    fontSize = 10.sp
                                )
                                if (device.hardwareModel.isNotBlank()) {
                                    Text(
                                        "Modelo: ${device.hardwareModel}",
                                        color = IndustrialTheme.TextoSecundario,
                                        fontSize = 10.sp
                                    )
                                }
                                if (device.capabilities.isNotBlank()) {
                                    Text(
                                        "Capacidades: ${device.capabilities}",
                                        color = IndustrialTheme.TextoSecundario,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(device.appType, color = IndustrialTheme.Primario, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(if(device.isConnected) IndustrialTheme.Exito else IndustrialTheme.Error, androidx.compose.foundation.shape.CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(if(device.isConnected) "ONLINE" else "OFFLINE", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            Spacer(Modifier.width(16.dp))
                            Text("RSSI: ${device.rssi} dBm", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            if (device.ip.isNotBlank()) {
                                Spacer(Modifier.width(12.dp))
                                Text("IP: ${device.ip}", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IndustrialActionButton(
                                texto = if (device.isAuthorized) "Desconectar" else "Autorizar",
                                icono = if (device.isAuthorized) Icons.Default.LinkOff else Icons.Default.Check,
                                modifier = Modifier.weight(1f).height(36.dp),
                                enabled = enabled,
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
                                    enabled = enabled,
                                    onClick = { onRejectDevice(device.mac) }
                                )
                            } else {
                                IndustrialActionButton(
                                    texto = "Forzar Reconexión",
                                    icono = Icons.Default.Refresh,
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    enabled = enabled && (!state.isBluetoothReconnecting || state.reconnectingMac != device.mac),
                                    onClick = { onReconnectDevice(device.mac) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
