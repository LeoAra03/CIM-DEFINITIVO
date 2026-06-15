package com.industria.coordinacion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons as MaterialIcons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BluetoothDebugTab(
    state: NetworkTabState,
    logs: List<String>,
    onRefresh: () -> Unit,
    onForceIdentify: (mac: String) -> Unit,
    onReconnect: (mac: String) -> Unit,
    onDisconnect: (mac: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Icon(MaterialIcons.Default.Bluetooth, contentDescription = null, tint = IndustrialTheme.Primario)
            Spacer(Modifier.width(8.dp))
            Text(text = "Bluetooth Debug", color = IndustrialTheme.TextoPrincipal)
        }

        Spacer(Modifier.height(12.dp))

        Text(text = "Dispositivos conectados: ${state.totalConnected}", color = IndustrialTheme.TextoSecundario)

        Spacer(Modifier.height(8.dp))

        if (state.connectedDevices.isEmpty()) {
            Text(text = "No hay dispositivos conectados", color = IndustrialTheme.TextoSecundario)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.connectedDevices) { device ->
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(IndustrialTheme.Tarjeta)) {
                        Text("${device.name} - ${device.mac}", color = IndustrialTheme.TextoPrincipal)
                        Text("App: ${device.appType} - RSSI: ${device.rssi}", color = IndustrialTheme.TextoSecundario)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IndustrialActionButton(texto = "Forzar IDENTIFY", icono = MaterialIcons.Default.BugReport, modifier = Modifier.weight(1f).height(36.dp), onClick = { onForceIdentify(device.mac) })
                            IndustrialActionButton(texto = "Reconnect", icono = MaterialIcons.Default.Refresh, modifier = Modifier.weight(1f).height(36.dp), onClick = { onReconnect(device.mac) })
                            IndustrialActionButton(texto = "Disconnect", icono = MaterialIcons.Default.Cancel, modifier = Modifier.weight(1f).height(36.dp), colorFondo = IndustrialTheme.Error, onClick = { onDisconnect(device.mac) })
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(text = "Logs recientes:", color = IndustrialTheme.TextoSecundario)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { line ->
                Text(text = line, color = IndustrialTheme.TextoSecundario, modifier = Modifier.padding(2.dp))
            }
        }
    }
}
