package com.industria.coordinacion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialCard
import com.sistema.distribuido.network.prefecto.IndustrialStatusRow
import com.sistema.distribuido.network.prefecto.IndustrialTheme

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
    currentGcodeFile: String? = null,
    enabled: Boolean = true
) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                colorFondo = if (selectedSubTab == 0) IndustrialTheme.Primario else IndustrialTheme.TarjetaAlta,
                enabled = enabled,
                onClick = { selectedSubTab = 0 }
            )
            IndustrialActionButton(
                texto = "Láser",
                icono = Icons.Default.FlashOn,
                modifier = Modifier.weight(1f),
                colorFondo = if (selectedSubTab == 1) IndustrialTheme.Primario else IndustrialTheme.TarjetaAlta,
                enabled = enabled,
                onClick = { selectedSubTab = 1 }
            )
            IndustrialActionButton(
                texto = "QC",
                icono = Icons.Default.VerifiedUser,
                modifier = Modifier.weight(1f),
                colorFondo = if (selectedSubTab == 2) IndustrialTheme.Primario else IndustrialTheme.TarjetaAlta,
                enabled = enabled,
                onClick = { selectedSubTab = 2 }
            )
        }

        when (selectedSubTab) {
            0 -> RobotControlPanel(onRobotCommand, enabled)
            1 -> LaserControlPanel(onLaserCommand, currentGcodeFile, enabled)
            2 -> QCControlPanel(qcState, onStartQcProgram, onStopQcProgram, enabled)
        }
    }
}

@Composable
private fun RobotControlPanel(onCommand: (String) -> Unit, enabled: Boolean) {
    IndustrialCard("Movimiento Scorbot", Icons.Default.OpenWith) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(
                texto = "HOME",
                icono = Icons.Default.Home,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { onCommand("R:HOME") }
            )
            IndustrialActionButton(
                texto = "READY",
                icono = Icons.Default.AdsClick,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { onCommand("R:READY") }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "PROGRAMAS DE TRABAJO (PRESET)",
            color = IndustrialTheme.TextoSecundario,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
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
                            enabled = enabled,
                            onClick = { onCommand("R:PROG:$pIdx") }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "SECUENCIAS AUTOMÁTICAS",
            color = IndustrialTheme.TextoSecundario,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(
                texto = "Ciclo A",
                icono = Icons.Default.Loop,
                modifier = Modifier.weight(1f),
                colorFondo = IndustrialTheme.Advertencia,
                enabled = enabled,
                onClick = { onCommand("R:SEQ:A") }
            )
            IndustrialActionButton(
                texto = "Ciclo B",
                icono = Icons.Default.Loop,
                modifier = Modifier.weight(1f),
                colorFondo = IndustrialTheme.Advertencia,
                enabled = enabled,
                onClick = { onCommand("R:SEQ:B") }
            )
        }
    }

    IndustrialCard("Gripper & Manual", Icons.Default.PanTool) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(
                texto = "ABRIR",
                icono = Icons.Default.KeyboardArrowUp,
                modifier = Modifier.weight(1f),
                colorFondo = IndustrialTheme.TextoSecundario,
                enabled = enabled,
                onClick = { onCommand("R:OPEN") }
            )
            IndustrialActionButton(
                texto = "CERRAR",
                icono = Icons.Default.KeyboardArrowDown,
                modifier = Modifier.weight(1f),
                colorFondo = IndustrialTheme.TextoSecundario,
                enabled = enabled,
                onClick = { onCommand("R:CLOSE") }
            )
        }
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(
            texto = "Guardar Posición Actual",
            icono = Icons.Default.Save,
            colorFondo = IndustrialTheme.Exito,
            enabled = enabled,
            onClick = { onCommand("R:SAVE") }
        )
    }
}

@Composable
private fun LaserControlPanel(onCommand: (String) -> Unit, currentGcodeFile: String?, enabled: Boolean) {
    IndustrialCard("Operación Láser CNC", Icons.Default.Settings) {
        IndustrialActionButton(
            texto = "Reset Ejes (HOME)",
            icono = Icons.Default.Home,
            colorFondo = IndustrialTheme.Error,
            enabled = enabled,
            onClick = { onCommand("L:HOME") }
        )
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(
            texto = "Iniciar Grabado",
            icono = Icons.Default.PlayArrow,
            colorFondo = IndustrialTheme.Exito,
            enabled = enabled,
            onClick = { onCommand("L:START") }
        )
        Spacer(Modifier.height(12.dp))
        IndustrialActionButton(
            texto = "Parada Emergencia",
            icono = Icons.Default.Stop,
            colorFondo = IndustrialTheme.Error,
            enabled = enabled,
            onClick = { onCommand("L:STOP") }
        )
    }

    IndustrialCard("Gestión de Archivos", Icons.Default.Folder) {
        IndustrialActionButton(
            texto = "Cargar G-code",
            icono = Icons.Default.FileUpload,
            colorFondo = IndustrialTheme.Secundario,
            enabled = enabled,
            onClick = { onCommand("LASER_LOAD") }
        )
        Spacer(Modifier.height(8.dp))
        IndustrialStatusRow("Archivo Actual", currentGcodeFile ?: "No cargado", currentGcodeFile != null)
    }

    IndustrialCard("Ajustes de Óptica", Icons.Default.Tune) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IndustrialActionButton(
                texto = "Z-Offset +",
                icono = Icons.Default.Add,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { onCommand("L:Z_UP") }
            )
            IndustrialActionButton(
                texto = "Z-Offset -",
                icono = Icons.Default.Remove,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = { onCommand("L:Z_DOWN") }
            )
        }
    }
}

@Composable
private fun QCControlPanel(
    qcState: QcProgramState,
    onStartQcProgram: (String) -> Unit,
    onStopQcProgram: (String) -> Unit,
    enabled: Boolean
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
        status = sr1Status,
        enabled = enabled,
        onStart = { onStartQcProgram("SR1") },
        onStop = { onStopQcProgram("SR1") }
    )

    QCProgramCard(
        title = "SR2: Verificación de Dimensiones",
        description = "Medición automática de dimensiones críticas",
        status = sr2Status,
        enabled = enabled,
        onStart = { onStartQcProgram("SR2") },
        onStop = { onStopQcProgram("SR2") }
    )

    QCProgramCard(
        title = "SR3: Test de Funcionalidad",
        description = "Prueba funcional automática de características del producto",
        status = sr3Status,
        enabled = enabled,
        onStart = { onStartQcProgram("SR3") },
        onStop = { onStopQcProgram("SR3") }
    )

    QCProgramCard(
        title = "SR4: Empaque y Etiquetado",
        description = "Preparación de empaque y aplicación de etiquetas",
        status = sr4Status,
        enabled = enabled,
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

@Composable
private fun QCStatusIndicator(label: String, status: QCStatus?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when (status) {
                        QCStatus.RUNNING -> IndustrialTheme.Advertencia
                        QCStatus.SUCCESS -> IndustrialTheme.Primario
                        QCStatus.FAILED -> IndustrialTheme.Error
                        else -> IndustrialTheme.TextoSecundario
                    },
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {}
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = IndustrialTheme.TextoSecundario)
    }
}

@Composable
private fun QCProgramCard(
    title: String,
    description: String,
    status: QCStatus?,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    IndustrialCard(title, Icons.Default.Autorenew) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(description, fontSize = 10.sp, color = IndustrialTheme.TextoSecundario)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndustrialActionButton(
                    texto = "Ejecutar",
                    icono = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    colorFondo = IndustrialTheme.Exito,
                    enabled = enabled,
                    onClick = onStart
                )
                IndustrialActionButton(
                    texto = "Detener",
                    icono = Icons.Default.Stop,
                    modifier = Modifier.weight(1f),
                    colorFondo = IndustrialTheme.Error,
                    enabled = enabled,
                    onClick = onStop
                )
            }
            status?.let {
                when (it) {
                    QCStatus.RUNNING -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    QCStatus.SUCCESS -> Text("Estado: ÉXITO", fontSize = 11.sp, color = IndustrialTheme.Primario)
                    QCStatus.FAILED -> Text("Estado: FALLA", fontSize = 11.sp, color = IndustrialTheme.Error)
                }
            }
        }
    }
}
