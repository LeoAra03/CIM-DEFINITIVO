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

data class PaletaTracking(
    val id: String,
    val ubicacion: String,
    val timestamp: String,
    val estado: String
)

@Composable
fun TrackingTab(
    state: TrackingState,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTracking = state.isTracking
    val paletas = state.pallets.ifEmpty {
        listOf(
            PaletaTracking("PAL-001", "ALMACÉN L3", "14:23", "DISPONIBLE"),
            PaletaTracking("PAL-002", "ROBOT SCORBOT", "14:22", "PROCESANDO"),
            PaletaTracking("PAL-003", "CINTA POS 5", "14:20", "EN TRÁNSITO"),
            PaletaTracking("PAL-004", "ESTACIÓN QC", "14:15", "VALIDADO")
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IndustrialCard("Rastreo en Tiempo Real", Icons.Default.Radar) {
                IndustrialStatusRow("Servicio Localización", if(isTracking) "ACTIVO" else "IDLE", isTracking)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndustrialActionButton(
                        texto = "Start Scan", 
                        icono = Icons.Default.PlayArrow, 
                        modifier = Modifier.weight(1f),
                        enabled = !isTracking,
                        onClick = onStartTracking
                    )
                    IndustrialActionButton(
                        texto = "Stop", 
                        icono = Icons.Default.Stop, 
                        modifier = Modifier.weight(1f),
                        colorFondo = IndustrialTheme.Error,
                        enabled = isTracking,
                        onClick = onStopTracking
                    )
                }
            }
        }

        item {
            Text("HISTORIAL DE MOVIMIENTOS", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        items(paletas) { paleta ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IndustrialTheme.Tarjeta, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, IndustrialTheme.Borde, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, "Ícono inventario", Modifier.size(24.dp), tint = IndustrialTheme.Primario)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(paleta.id, color = IndustrialTheme.TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(paleta.ubicacion, color = IndustrialTheme.TextoSecundario, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(paleta.estado, color = IndustrialTheme.Exito, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                        Text(paleta.timestamp, color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            IndustrialActionButton(texto = "Exportar Reporte CSV", icono = Icons.Default.FileDownload, colorFondo = Color.DarkGray, onClick = onExportCsv)
        }
    }
}
