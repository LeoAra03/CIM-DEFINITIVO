package com.industria.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.sistema.distribuido.network.MeshNetworkManager

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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("CIM Watch", textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(status, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    status = "E-STOP ENVIADO"
                    MeshNetworkManager.propagateEmergency("WEAR_OS")
                    vibrateEmergency(context)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("E-STOP")
            }
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
