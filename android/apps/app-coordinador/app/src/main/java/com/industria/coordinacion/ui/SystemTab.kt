package com.industria.coordinacion.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

data class CintaPanelState(
    val isConnected: Boolean = false,
    val lastCommand: String = "",
    val stationStates: Map<Int, Boolean> = mapOf(1 to false, 2 to false, 3 to false)
)

@Composable
fun SystemTab(
    state: CintaPanelState,
    onDeliverCommand: (from: Int, to: Int) -> Unit,
    onFreeCommand: (from: Int, to: Int) -> Unit,
    onConnectCinta: () -> Unit,
    onDisconnectCinta: () -> Unit,
    onResetCinta: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            IndustrialCard("Control Maestro de Cinta", Icons.Default.SettingsInputComponent) {
                IndustrialStatusRow("Enlace Cinta", if(state.isConnected) "OPERATIVO" else "STANDBY", state.isConnected)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialActionButton(
                        texto = "Conectar", 
                        icono = Icons.Default.Bluetooth, 
                        enabled = !state.isConnected,
                        modifier = Modifier.weight(1f),
                        onClick = onConnectCinta
                    )
                    IndustrialActionButton(
                        texto = "Cerrar", 
                        icono = Icons.Default.Close, 
                        enabled = state.isConnected,
                        colorFondo = IndustrialTheme.Error,
                        modifier = Modifier.weight(1f),
                        onClick = onDisconnectCinta
                    )
                }
                Spacer(Modifier.height(8.dp))
                IndustrialActionButton(
                    texto = "Reset Sistema", 
                    icono = Icons.Default.Refresh, 
                    colorFondo = IndustrialTheme.Advertencia,
                    onClick = onResetCinta
                )
            }
        }

        item {
            Text("MATRIZ DE DISTRIBUCIÓN (3 SALIDAS X 10 POSICIONES)", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        }

        repeat(3) { row ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(10) { col ->
                        val from = row + 1
                        val to = col + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(if(state.isConnected) IndustrialTheme.Primario.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .border(1.dp, if(state.isConnected) IndustrialTheme.Primario.copy(alpha = 0.3f) else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .clickable(enabled = state.isConnected) { onDeliverCommand(from, to) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$from>$to", fontSize = 8.sp, color = if(state.isConnected) IndustrialTheme.Primario else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("COMANDOS DE LIBERACIÓN RÁPIDA (FREE)", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        }

        repeat(6) { rowGroup ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { colIdx ->
                        val totalIdx = rowGroup * 5 + colIdx
                        val from = (totalIdx / 10) + 1
                        val to = (totalIdx % 10) + 1
                        IndustrialActionButton(
                            texto = "F$from>$to",
                            icono = Icons.Default.LockOpen,
                            modifier = Modifier.weight(1f).height(40.dp),
                            colorFondo = IndustrialTheme.Advertencia.copy(alpha = 0.8f),
                            enabled = state.isConnected,
                            onClick = { onFreeCommand(from, to) }
                        )
                    }
                }
            }
        }
    }
}
