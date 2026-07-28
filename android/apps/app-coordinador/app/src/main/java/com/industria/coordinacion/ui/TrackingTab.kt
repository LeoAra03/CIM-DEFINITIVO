package com.industria.coordinacion.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isTracking = state.isTracking
    val paletas = state.pallets

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
                        enabled = enabled && !isTracking,
                        onClick = onStartTracking
                    )
                    IndustrialActionButton(
                        texto = "Stop", 
                        icono = Icons.Default.Stop, 
                        modifier = Modifier.weight(1f),
                        colorFondo = IndustrialTheme.Error,
                        enabled = enabled && isTracking,
                        onClick = onStopTracking
                    )
                }
            }
        }

        item {
            ArcadePalletMap(paletas)
        }

        item {
            Text("HISTORIAL DE MOVIMIENTOS", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (paletas.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin pallets registrados. Esperando eventos PALLET:<id>|EVENT:<evento>.",
                        color = IndustrialTheme.TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            }
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
            IndustrialActionButton(texto = "Exportar Reporte CSV", icono = Icons.Default.FileDownload, colorFondo = Color.DarkGray, enabled = enabled, onClick = onExportCsv)
        }
    }
}


private data class ArcadeTarget(val x: Float, val y: Float, val color: Color, val label: String)

private fun arcadeTarget(stage: String): ArcadeTarget = when {
    stage.contains("REGISTERED") || stage.contains("STORAGE_RELEASED") -> ArcadeTarget(0.10f, 0.50f, Color(0xFF7C4DFF), "ALMACÉN")
    stage.contains("MANUFACTURING") -> ArcadeTarget(0.42f, 0.50f, Color(0xFF00E5FF), "MANUFACTURA")
    stage.contains("QUALITY") || stage.contains("APPROVED") -> ArcadeTarget(0.68f, 0.50f, Color(0xFFFFD600), "CALIDAD")
    stage.contains("REJECTED") || stage.contains("BLOCKED") -> ArcadeTarget(0.88f, 0.75f, IndustrialTheme.Error, "BLOQUEADO")
    stage.contains("STORED") -> ArcadeTarget(0.10f, 0.20f, IndustrialTheme.Exito, "ALMACÉN FINAL")
    else -> ArcadeTarget(0.25f, 0.50f, IndustrialTheme.Primario, "CINTA")
}

/** Visualización didáctica: avanza exclusivamente cuando cambia PalletStage. */
@Composable
private fun ArcadePalletMap(pallets: List<PaletaTracking>) {
    data class AnimatedPallet(val target: ArcadeTarget, val x: Float, val y: Float)
    val animated = pallets.take(6).map { pallet ->
        val target = arcadeTarget(pallet.estado)
        val x by animateFloatAsState(targetValue = target.x, animationSpec = tween(700), label = "pallet-x-${pallet.id}")
        val y by animateFloatAsState(targetValue = target.y, animationSpec = tween(700), label = "pallet-y-${pallet.id}")
        AnimatedPallet(target, x, y)
    }

    IndustrialCard("Flujo Arcade de Pallets", Icons.Default.Route) {
        Text(
            "La animación representa eventos aceptados por la máquina de estados; no sustituye sensores físicos.",
            color = IndustrialTheme.TextoSecundario,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF10131C))) {
            val stations = listOf(
                "ALM" to Offset(size.width * 0.10f, size.height * 0.50f),
                "CINTA" to Offset(size.width * 0.26f, size.height * 0.50f),
                "MAN" to Offset(size.width * 0.42f, size.height * 0.50f),
                "CAL" to Offset(size.width * 0.68f, size.height * 0.50f),
                "FIN" to Offset(size.width * 0.88f, size.height * 0.20f)
            )
            stations.zipWithNext().forEach { (from, to) ->
                drawLine(Color.DarkGray, from.second, to.second, strokeWidth = 5f)
            }
            stations.forEach { (_, point) -> drawCircle(Color(0xFF303744), 18f, point) }
            animated.forEachIndexed { index, pallet ->
                val position = Offset(size.width * pallet.x, size.height * pallet.y + index * 6f)
                drawCircle(pallet.target.color, 10f, position)
            }
        }
        if (pallets.isNotEmpty()) {
            Text(
                pallets.take(3).joinToString("  •  ") { "${it.id}: ${arcadeTarget(it.estado).label}" },
                color = IndustrialTheme.TextoSecundario,
                fontSize = 10.sp
            )
        }
    }
}
