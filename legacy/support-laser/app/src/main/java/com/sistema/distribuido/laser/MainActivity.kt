package com.sistema.distribuido.laser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.sistema.distribuido.laser.viewmodels.LaserViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: LaserViewModel = hiltViewModel()
                LaserApp(viewModel)
            }
        }
    }
}

@Composable
fun LaserApp(viewModel: LaserViewModel = hiltViewModel()) {
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val laserPower by viewModel.laserPower.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LÁSER — Grabado/Corte",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE91E63),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Surface(
            modifier = Modifier
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
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Potencia: ${laserPower}%", color = Color.White, fontSize = 12.sp)
            }
        }

        if (isAuthorized) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.startEngraving() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Iniciar Grabado")
                }

                Button(
                    onClick = { viewModel.setPower(50) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Potencia 50%")
                }

                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("DETENER")
                }
            }
        }
    }
}
