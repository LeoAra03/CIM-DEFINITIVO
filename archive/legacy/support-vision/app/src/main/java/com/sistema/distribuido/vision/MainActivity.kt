package com.sistema.distribuido.vision

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
import com.sistema.distribuido.vision.viewmodels.VisionViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: VisionViewModel = hiltViewModel()
                VisionApp(viewModel)
            }
        }
    }
}

@Composable
fun VisionApp(viewModel: VisionViewModel = hiltViewModel()) {
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VISIÓN — Detección ArUco & Calidad",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
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
                Text(text = "Estado: $connectionStatus", color = Color.White, fontSize = 12.sp)
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
                    onClick = { viewModel.captureImage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Capturar Imagen")
                }

                Button(
                    onClick = { viewModel.detectAruco() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Detectar ArUco")
                }

                Button(
                    onClick = { viewModel.checkQuality() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Inspección Calidad")
                }
            }
        } else {
            Text(
                text = "⏳ Esperando autorización...",
                fontSize = 14.sp,
                color = Color(0xFFF57C00),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
