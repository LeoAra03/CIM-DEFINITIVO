package com.industria.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.sistema.distribuido.network.MeshNetworkManager
import com.sistema.distribuido.network.prefecto.IndustrialTheme

class WearMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MeshNetworkManager.activate()
        setContent { WearEmergencyScreen() }
    }
}

@Composable
fun WearEmergencyScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Planta OK") }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IndustrialTheme.Fondo)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cabecera compacta: nombre + punto de estado (lenguaje HMI).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (status == "Planta OK") IndustrialTheme.Primario else IndustrialTheme.Error,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "CIM WATCH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialTheme.TextoPrincipal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))

            // Chip de estado grande, legible de un vistazo.
            val colorEstado = if (status == "Planta OK") IndustrialTheme.Primario else IndustrialTheme.Error
            Box(
                Modifier
                    .background(colorEstado.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .border(1.dp, colorEstado.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorEstado,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    status = "E-STOP ENVIADO"
                    MeshNetworkManager.propagateEmergency("WEAR_OS")
                    vibrateEmergency(context)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = IndustrialTheme.Error,
                    contentColor = IndustrialTheme.TextoPrincipal
                )
            ) {
                Text("E-STOP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Solo supervisión",
                fontSize = 9.sp,
                color = IndustrialTheme.TextoTenue,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun vibrateEmergency(context: android.content.Context) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
        val mgr = context.getSystemService(VibratorManager::class.java)
        mgr?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1))
}
