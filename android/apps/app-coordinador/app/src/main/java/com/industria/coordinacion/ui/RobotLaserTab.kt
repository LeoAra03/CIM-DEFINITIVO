package com.industria.coordinacion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

enum class QCStatus {
    RUNNING,
    SUCCESS,
    FAILED
}

@Composable
fun RobotLaserTab(
    onRobotCommand: (String) -> Unit = {},
    onLaserCommand: (String) -> Unit = {},
    qcState: QcProgramState = QcProgramState(),
    onStartQcProgram: (String) -> Unit = {},
    onStopQcProgram: (String) -> Unit = {},
    currentGcodeFile: String? = null
) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IndustrialActionButton(
                texto = "Scorbot", 
                icono = Icons.Default.PrecisionManufacturing,
                modifier = Modifier.weight(1f),
                colorFondo = if (selectedSubTab == 0) IndustrialTheme.Primario else Color.DarkGray,
                onClick = { selectedSubTab = 0 }
            )
            IndustrialActionButton(
                texto = "Láser", 
                icono = Icons.Default.FlashOn,
                modifier = Modifier.weight(1f),
                colorFondo = if (selectedSubTab == 1) IndustrialTheme.Primario else Color.DarkGray,
                onClick = { selectedSubTab = 1 }
            )
            IndustrialActionButton(
                texto = "QC", 
                icono = Icons.Default.VerifiedUser,
                modifier = Modifier.weight(1f),
                colorFondo = if (selectedSubTab == 2) IndustrialTheme.Primario else Color.DarkGray,
                onClick = { selectedSubTab = 2 }
            )
        }

        when (selectedSubTab) {
            0 -> RobotControlPanel(onRobotCommand)
            1 -> LaserControlPanel(onLaserCommand, currentGcodeFile)
            2 -> QCControlPanel(qcState, onStartQcProgram, onStopQcProgram)
        }
    }
}

@Composable
private fun RobotControlPanel(onCommand: (String) -> Unit) {
    IndustrialCard("Movimiento Scorbot", Icons.Default.OpenWith) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(texto = "HOME", icono = Icons.Default.Home, modifier = Modifier.weight(1f), onClick = { onCommand("R:HOME") })
            IndustrialActionButton(texto = "READY", icono = Icons.Default.AdsClick, modifier = Modifier.weight(1f), onClick = { onCommand("R:READY") })
        }
        
        Spacer(Modifier.height(16.dp))
        Text("PROGRAMAS DE TRABAJO (PRESET)", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        repeat(3) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { col ->
                    val pIdx = row * 2 + col + 1
                    if (pIdx <= 5) {
                        IndustrialActionButton(
                            texto = "PROG $pIdx", 
                            icono = Icons.Default.Code, 
                            modifier = Modifier.weight(1f),
                            colorFondo = IndustrialTheme.Secundario,
                            onClick = { onCommand("R:PROG:$pIdx") }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text("SECUENCIAS AUTOMÁTICAS", color = IndustrialTheme.TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(texto = "Ciclo A", icono = Icons.Default.Loop, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Advertencia, onClick = { onCommand("R:SEQ:A") })
            IndustrialActionButton(texto = "Ciclo B", icono = Icons.Default.Loop, modifier = Modifier.weight(1f), colorFondo = IndustrialTheme.Advertencia, onClick = { onCommand("R:SEQ:B") })
        }
    }

    IndustrialCard("Gripper & Manual", Icons.Default.PanTool) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(texto = "ABRIR", icono = Icons.Default.KeyboardArrowUp, modifier = Modifier.weight(1f), colorFondo = Color.Gray, onClick = { onCommand("R:OPEN") })
            IndustrialActionButton(texto = "CERRAR", icono = Icons.Default.KeyboardArrowDown, modifier = Modifier.weight(1f), colorFondo = Color.Gray, onClick = { onCommand("R:CLOSE") })
        }
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(texto = "Guardar Posición Actual", icono = Icons.Default.Save, colorFondo = IndustrialTheme.Exito, onClick = { onCommand("R:SAVE") })
    }
}

@Composable
private fun LaserControlPanel(onCommand: (String) -> Unit, currentGcodeFile: String?) {
    IndustrialCard("Operación Láser CNC", Icons.Default.Settings) {
        IndustrialActionButton(texto = "Reset Ejes (HOME)", icono = Icons.Default.Home, colorFondo = IndustrialTheme.Error, onClick = { onCommand("L:HOME") })
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(texto = "Iniciar Grabado", icono = Icons.Default.PlayArrow, colorFondo = IndustrialTheme.Exito, onClick = { onCommand("L:START") })
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(texto = "Parada Emergencia", icono = Icons.Default.Stop, colorFondo = IndustrialTheme.Error, onClick = { onCommand("L:STOP") })
    }

    IndustrialCard("Gestión de Archivos", Icons.Default.Folder) {
        IndustrialActionButton(texto = "Cargar G-code", icono = Icons.Default.FileUpload, colorFondo = IndustrialTheme.Secundario, onClick = { onCommand("LASER_LOAD") })
        Spacer(Modifier.height(8.dp))
        IndustrialStatusRow("Archivo Actual", currentGcodeFile ?: "No cargado", currentGcodeFile != null)
    }

    IndustrialCard("Ajustes de Óptica", Icons.Default.Tune) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(texto = "Z-Offset +", icono = Icons.Default.Add, modifier = Modifier.weight(1f), onClick = { onCommand("L:Z_UP") })
            IndustrialActionButton(texto = "Z-Offset -", icono = Icons.Default.Remove, modifier = Modifier.weight(1f), onClick = { onCommand("L:Z_DOWN") })
        }
    }
}

/**
 * 🎯 Panel de Control de Calidad (QC Programs)
 */
@Composable
private fun QCProgramCard(
    title: String,
    description: String,
    commandStart: String,
    commandStop: String,
    status: QCStatus?,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    IndustrialCard(title, Icons.Default.Autorenew) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(description, fontSize = 10.sp, color = IndustrialTheme.TextoSecundario)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndustrialActionButton(
                    texto = "Ejecutar",
                    icono = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    colorFondo = IndustrialTheme.Exito,
                    onClick = onStart
                )
                IndustrialActionButton(
                    texto = "Detener",
                    icono = Icons.Default.Stop,
                    modifier = Modifier.weight(1f),
                    colorFondo = IndustrialTheme.Error,
                    onClick = onStop
                )
            }
            status?.let {
                when (it) {
                    QCStatus.RUNNING -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    QCStatus.SUCCESS -> Text("Estado: ÉXITO", fontSize = 11.sp, color = Color.Green)
                    QCStatus.FAILED -> Text("Estado: FALLA", fontSize = 11.sp, color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun QCStatusIndicator(label: String, status: QCStatus?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when (status) {
                        QCStatus.RUNNING -> Color.Yellow
                        QCStatus.SUCCESS -> Color.Green
                        QCStatus.FAILED -> Color.Red
                        else -> Color.Gray
                    },
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {}
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = IndustrialTheme.TextoSecundario)
    }
}

/**
 * 🎯 Panel de Control de Calidad (QC Programs)
 * Interfaz para ejecutar los 4 programas de control de calidad:
 * SR1: Inspección visual de producto
 * SR2: Verificación de dimensiones
 * SR3: Test de funcionalidad
 * SR4: Empaque y etiquetado
 */
@Composable
private fun QCControlPanel(
    qcState: QcProgramState,
    onStartQcProgram: (String) -> Unit,
    onStopQcProgram: (String) -> Unit
) {
    val sr1Status = qcState.sr1Status
    val sr2Status = qcState.sr2Status
    val sr3Status = qcState.sr3Status
    val sr4Status = qcState.sr4Status
    val selectedProgram = qcState.selectedProgram

    IndustrialCard("Estado de Programas de QC", Icons.Default.VerifiedUser) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QCStatusIndicator("SR1", sr1Status)
            QCStatusIndicator("SR2", sr2Status)
            QCStatusIndicator("SR3", sr3Status)
            QCStatusIndicator("SR4", sr4Status)
        }
    }

    QCProgramCard(
        title = "SR1: Inspección Visual",
        description = "Inspección automática del aspecto visual del producto",
        commandStart = "SR1",
        commandStop = "SR1",
        status = sr1Status,
        onStart = { onStartQcProgram("SR1") },
        onStop = { onStopQcProgram("SR1") }
    )

    QCProgramCard(
        title = "SR2: Verificación de Dimensiones",
        description = "Medición automática de dimensiones críticas",
        commandStart = "SR2",
        commandStop = "SR2",
        status = sr2Status,
        onStart = { onStartQcProgram("SR2") },
        onStop = { onStopQcProgram("SR2") }
    )

    QCProgramCard(
        title = "SR3: Test de Funcionalidad",
        description = "Prueba funcional automática de características del producto",
        commandStart = "SR3",
        commandStop = "SR3",
        status = sr3Status,
        onStart = { onStartQcProgram("SR3") },
        onStop = { onStopQcProgram("SR3") }
    )

    QCProgramCard(
        title = "SR4: Empaque y Etiquetado",
        description = "Preparación de empaque y aplicación de etiquetas",
        commandStart = "SR4",
        commandStop = "SR4",
        status = sr4Status,
        onStart = { onStartQcProgram("SR4") },
        onStop = { onStopQcProgram("SR4") }
    )

    selectedProgram?.let {
        IndustrialCard("Resultados: $it", Icons.Default.CheckCircle) {
            Text(
                "Estado actual: ${when (it) {
                    "SR1" -> sr1Status
                    "SR2" -> sr2Status
                    "SR3" -> sr3Status
                    "SR4" -> sr4Status
                    else -> null
                }?.name ?: "ESPERANDO"}",
                fontSize = 11.sp,
                color = IndustrialTheme.TextoSecundario
            )
        }
    }
}
