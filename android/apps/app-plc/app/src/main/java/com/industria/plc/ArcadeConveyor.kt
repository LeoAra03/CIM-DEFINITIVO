/**
 * ArcadeConveyor — Cinta transportadora estilo arcade para tracking de pallets.
 * Interfaz dinámica: los pallets se mueven en bucle por la cinta (animación O(1),
 * sin dependencia de simulación; refleja el estado real de sensores si existe).
 */
package com.industria.plc

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.sistema.distribuido.network.prefecto.IndustrialTheme

/**
 * Cinta transportadora animada estilo arcade.
 * @param stations Lista de (nombre, posición) en orden de recorrido.
 * @param palletPresent Mapa posición -> hay pallet.
 * @param lastEvent Texto del último evento de tracking.
 */
@Composable
fun ArcadeConveyor(
    stations: List<Pair<String, Int>>,
    palletPresent: Map<Int, Boolean>,
    lastEvent: String
) {
    val transition = rememberInfiniteTransition(label = "conveyor")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing), RepeatMode.Restart),
        label = "belt"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IndustrialTheme.Tarjeta, RoundedCornerShape(IndustrialTheme.RadioTarjeta))
            .border(1.dp, IndustrialTheme.Borde, RoundedCornerShape(IndustrialTheme.RadioTarjeta))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CINTA TRANSPORTADORA", color = IndustrialTheme.TextoPrincipal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text("EVENTO: $lastEvent", color = IndustrialTheme.TextoSecundario, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(IndustrialTheme.Fondo, RoundedCornerShape(IndustrialTheme.RadioControl))
        ) {
            val w = size.width
            val h = size.height
            val beltY = h * 0.62f
            val stationXs = stations.indices.map { i ->
                if (stations.size == 1) w / 2f else w * (i.toFloat() / (stations.size - 1).toFloat())
            }

            // Cinta (banda)
            drawLine(IndustrialTheme.TarjetaAlta, Offset(0f, beltY), Offset(w, beltY), strokeWidth = 10f)
            // Marcas de la banda (movimiento)
            val dashes = 12
            for (i in 0 until dashes) {
                val phase = (progress * dashes + i) % dashes
                val x = w * phase / dashes
                drawLine(
                    IndustrialTheme.TextoTenue.copy(alpha = 0.55f),
                    Offset(x, beltY - 4f),
                    Offset(x, beltY + 4f),
                    strokeWidth = 3f
                )
            }

            // Estaciones (arcade: luz que se enciende cuando hay pallet)
            stations.forEachIndexed { i, (_, pos) ->
                val x = stationXs[i]
                val present = palletPresent[pos] == true
                val light = if (present) IndustrialTheme.Primario else IndustrialTheme.TarjetaAlta
                drawCircle(light, radius = 8f, center = Offset(x, h * 0.18f))
                drawCircle(if (present) IndustrialTheme.TextoPrincipal else IndustrialTheme.TextoTenue, radius = 4f, center = Offset(x, h * 0.18f))
                drawLine(
                    if (present) IndustrialTheme.Primario else IndustrialTheme.TextoTenue.copy(alpha = 0.45f),
                    Offset(x, h * 0.18f + 10f),
                    Offset(x, beltY - 6f),
                    strokeWidth = 2f
                )
            }

            // Pallets en movimiento (uno por estación ocupada, más uno de tránsito animado)
            stations.forEachIndexed { i, (_, pos) ->
                if (palletPresent[pos] == true) {
                    val x = stationXs[i]
                    drawRect(
                        color = IndustrialTheme.Secundario,
                        topLeft = Offset(x - 12f, beltY - 8f),
                        size = Size(24f, 16f),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    drawRect(
                        color = IndustrialTheme.Fondo,
                        topLeft = Offset(x - 12f, beltY - 8f),
                        size = Size(24f, 16f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
            // Pallet de tránsito siempre animado (simula flujo continuo)
            val travelerX = progress * w
            drawRect(
                color = IndustrialTheme.Primario.copy(alpha = 0.85f),
                topLeft = Offset(travelerX - 10f, beltY - 7f),
                size = Size(20f, 14f),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawRect(
                color = IndustrialTheme.TextoPrincipal,
                topLeft = Offset(travelerX - 10f, beltY - 7f),
                size = Size(20f, 14f),
                style = Stroke(width = 1.2f)
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stations.forEach { (name, pos) ->
                val present = palletPresent[pos] == true
                Text(
                    text = "$pos $name",
                    color = if (present) IndustrialTheme.Primario else IndustrialTheme.TextoSecundario,
                    fontSize = 10.sp,
                    fontWeight = if (present) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
