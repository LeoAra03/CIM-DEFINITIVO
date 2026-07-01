package com.sistema.distribuido.network.prefecto

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Gemelo digital simplificado en tiempo real (vista 2.5D).
 * Representa posiciones objetivo vs actuales de estaciones CIM.
 * Para modelos .glb de alta fidelidad, integrar SceneView/Filament en fase posterior.
 */
@Composable
fun DigitalTwinPanel(
    stationStates: Map<String, StationTwinState>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "twin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rot"
    )

    Column(modifier.fillMaxWidth()) {
        Text("GEMELO DIGITAL", color = IndustrialTheme.Primario, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            rotate(rotation, pivot = Offset(cx, cy)) {
                drawRect(
                    color = Color(0xFF1A1D2D),
                    topLeft = Offset(cx - 80, cy - 50),
                    size = androidx.compose.ui.geometry.Size(160f, 100f)
                )
            }
            stationStates.entries.forEachIndexed { index, (_, state) ->
                val angle = (index * 90f + rotation * 0.1f) * (Math.PI / 180)
                val x = cx + kotlin.math.cos(angle).toFloat() * 60
                val y = cy + kotlin.math.sin(angle).toFloat() * 40
                drawCircle(
                    color = if (state.isTarget) Color(0x4400E5FF) else state.color,
                    radius = if (state.isTarget) 18f else 12f,
                    center = Offset(x, y)
                )
            }
        }
        stationStates.forEach { (name, state) ->
            Text(
                "$name: ${state.label}",
                color = IndustrialTheme.TextoSecundario,
                fontSize = 10.sp
            )
        }
    }
}

data class StationTwinState(
    val label: String,
    val color: Color,
    val isTarget: Boolean = false
)
