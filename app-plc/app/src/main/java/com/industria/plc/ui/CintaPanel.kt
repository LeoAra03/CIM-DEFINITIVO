package com.industria.plc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.*

data class CintaPanelState(
    val isConnected: Boolean = false,
    val lastCommand: String = "Esperando...",
    val commandLog: List<String> = emptyList()
)

@Composable
fun CintaPanel(
    state: CintaPanelState = CintaPanelState(),
    onDeliverCommand: (Int, Int) -> Unit = { _, _ -> },
    onConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IndustrialCard(titulo = "Cinta Transportadora PLC", icono = Icons.Default.DirectionsRun) {
            IndustrialStatusRow("Estado", if (state.isConnected) "✓ Conectado" else "✗ Desconectado", state.isConnected)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (state.isConnected) {
                    IndustrialActionButton(texto = "Desconectar", icono = Icons.Default.PowerSettingsNew, modifier = Modifier.weight(1f).height(40.dp), colorFondo = IndustrialTheme.Error, onClick = onDisconnectClick)
                } else {
                    IndustrialActionButton(texto = "Conectar", icono = Icons.Default.PowerSettingsNew, modifier = Modifier.weight(1f).height(40.dp), colorFondo = IndustrialTheme.Primario, onClick = onConnectClick)
                }
            }
        }

        IndustrialCard(titulo = "Comandos DELIVER (3 Salidas × 10 Posiciones)", icono = Icons.Default.GridView) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { fromIdx ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(10) { toIdx ->
                            val buttonText = "${fromIdx + 1}→${toIdx + 1}"
                            IndustrialActionButton(
                                texto = buttonText,
                                icono = Icons.Default.Send,
                                modifier = Modifier.weight(1f).height(40.dp),
                                colorFondo = if (state.isConnected) IndustrialTheme.Primario.copy(alpha = 0.3f) else IndustrialTheme.Tarjeta,
                                enabled = state.isConnected,
                                onClick = { onDeliverCommand(fromIdx + 1, toIdx + 1) }
                            )
                        }
                    }
                }
            }
        }
    }
}
