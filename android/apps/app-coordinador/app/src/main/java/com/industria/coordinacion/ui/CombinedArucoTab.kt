package com.industria.coordinacion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.IndustrialTheme

@Composable
fun CombinedArucoTab(
    onGenerateAruco: (String) -> Unit = {},
    onUseWithLaser: (String) -> Unit = {},
    onArucoDetected: (DetectedArUco) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tabs (Generador / Detector)
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Black,
            contentColor = IndustrialTheme.Primario,
            divider = { HorizontalDivider(color = IndustrialTheme.Primario.copy(alpha = 0.3f)) }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Generador", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.QrCode, "Generador ArUco") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Detector", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.PhotoCamera, "Detector cámara") }
            )
        }

        // Contenido del sub-tab
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedSubTab) {
                0 -> ArucoGeneratorTab(onGenerateAruco, onUseWithLaser)
                1 -> ArUcoDetectionTab(onArucoDetected = { aruco ->
                    onArucoDetected(aruco)
                })
            }
        }
    }
}
