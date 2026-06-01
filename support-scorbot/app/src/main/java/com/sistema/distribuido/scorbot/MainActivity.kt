package com.sistema.distribuido.scorbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.sistema.distribuido.scorbot.viewmodels.ScorbotViewModel
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Timber para logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContent {
            MaterialTheme {
                val viewModel: ScorbotViewModel = hiltViewModel()
                ScorbotApp(viewModel)
            }
        }
    }
}

@Composable
fun ScorbotApp(viewModel: ScorbotViewModel = hiltViewModel()) {
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Text(
            text = "SCORBOT — Control de Brazo Robótico",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Status Bar
        StatusBar(
            isAuthorized = isAuthorized,
            connectionStatus = connectionStatus,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Control UI
        if (isAuthorized) {
            ScorbotControlUI(viewModel = viewModel)
        } else {
            UnauthorizedPlaceholder()
        }
    }
}

@Composable
fun StatusBar(
    isAuthorized: Boolean,
    connectionStatus: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        color = if (isAuthorized) Color(0xFF4CAF50) else Color(0xFFF44336),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isAuthorized) "✓ AUTORIZADO" else "✗ NO AUTORIZADO",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Estado: $connectionStatus",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ScorbotControlUI(viewModel: ScorbotViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Control buttons
        Button(
            onClick = { viewModel.executeCommand("R:EXTEND|10") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Extender Brazo", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { viewModel.executeCommand("R:RETRACT|10") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Retraer Brazo", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { viewModel.executeCommand("R:HOME") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text("Posición Home", fontWeight = FontWeight.Bold)
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Status text
        Text(
            text = "Estado local: LISTO",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun UnauthorizedPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⏳ Esperando autorización desde Coordinador...",
            fontSize = 14.sp,
            color = Color(0xFFF57C00),
            fontWeight = FontWeight.Bold
        )
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
        )
        Text(
            text = "El dispositivo solicitó permiso. Revisa la app Coordinador para aprobar.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
