package com.sistema.distribuido.coordinador.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import kotlinx.coroutines.delay

/**
 * AUTHORIZATION DIALOG — Jetpack Compose
 *
 * Componente Modal para solicitar autorización de nuevo dispositivo ESP32
 * con timeout automático, contador visual, y opciones de recordar decisión.
 *
 * Uso:
 * ```
 * if (showAuthDialog) {
 *     AuthorizationDialog(
 *         deviceName = "ESP32_SCORBOT",
 *         mac = "AA:BB:CC:DD:EE:FF",
 *         appType = "SCORBOT",
 *         onApprove = { rememberDecision ->
 *             // Usuario aprobó
 *             viewModel.approveDevice(mac, rememberDecision)
 *         },
 *         onReject = { rememberDecision ->
 *             // Usuario rechazó
 *             viewModel.rejectDevice(mac, rememberDecision)
 *         },
 *         onDismiss = {
 *             showAuthDialog = false
 *         }
 *     )
 * }
 * ```
 */
@Composable
fun AuthorizationDialog(
    deviceName: String,
    mac: String,
    appType: String,
    timeoutSeconds: Int = 5,
    onApprove: (rememberDecision: Boolean) -> Unit,
    onReject: (rememberDecision: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rememberDecision by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableStateOf(timeoutSeconds) }
    var hasResponded by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(secondsRemaining, hasResponded) {
        if (!hasResponded && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        } else if (secondsRemaining == 0 && !hasResponded) {
            // Auto-reject on timeout
            onReject(rememberDecision)
            hasResponded = true
        }
    }

    Dialog(
        onDismissRequest = {
            if (!hasResponded) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            color = IndustrialTheme.Tarjeta,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialTheme.Borde),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IndustrialTheme.Secundario.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_info),
                        contentDescription = "Authorization Request",
                        modifier = Modifier.size(26.dp),
                        tint = IndustrialTheme.Secundario
                    )
                }
                Spacer(Modifier.height(14.dp))

                // Title
                Text(
                    text = "NUEVA SOLICITUD DE AUTORIZACIÓN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = IndustrialTheme.TextoPrincipal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Device info card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = IndustrialTheme.TarjetaAlta,
                    shape = RoundedCornerShape(IndustrialTheme.RadioControl)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeviceInfoRow(label = "Dispositivo", value = deviceName)
                        DeviceInfoRow(label = "MAC", value = mac)
                        DeviceInfoRow(label = "Tipo", value = appType)
                    }
                }

                // Description
                Text(
                    text = "Este dispositivo solicita permiso para enviar comandos a través de la red CIM. " +
                           "Si apruebas, podrá ejecutar acciones en el sistema.",
                    fontSize = 13.sp,
                    color = IndustrialTheme.TextoSecundario,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                // Remember decision checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberDecision = !rememberDecision }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberDecision,
                        onCheckedChange = { rememberDecision = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = IndustrialTheme.Primario,
                            uncheckedColor = IndustrialTheme.TextoTenue,
                            checkmarkColor = IndustrialTheme.Fondo
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Recordar esta decisión",
                        fontSize = 13.sp,
                        color = IndustrialTheme.TextoPrincipal
                    )
                }

                // Countdown timer
                TimeoutCountdown(
                    secondsRemaining = secondsRemaining,
                    totalSeconds = timeoutSeconds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onReject(rememberDecision)
                            hasResponded = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialTheme.Error.copy(alpha = 0.16f),
                            contentColor = IndustrialTheme.Error
                        ),
                        enabled = !hasResponded
                    ) {
                        Text("RECHAZAR", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    }

                    Button(
                        onClick = {
                            onApprove(rememberDecision)
                            hasResponded = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialTheme.Primario,
                            contentColor = IndustrialTheme.Fondo
                        ),
                        enabled = !hasResponded
                    ) {
                        Text("APROBAR", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    }
                }

                // Disclaimer
                Text(
                    text = "Responde en ${secondsRemaining}s o se rechazará automáticamente",
                    fontSize = 11.sp,
                    color = IndustrialTheme.TextoTenue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * Device Info Row — Muestra clave-valor de dispositivo
 */
@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = IndustrialTheme.TextoSecundario
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = IndustrialTheme.TextoPrincipal,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(IndustrialTheme.Fondo)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Timeout Countdown — Muestra barra de progreso con contador
 */
@Composable
private fun TimeoutCountdown(
    secondsRemaining: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = (totalSeconds - secondsRemaining).toFloat() / totalSeconds
    val progressColor by animateColorAsState(
        targetValue = when {
            secondsRemaining <= 1 -> IndustrialTheme.Error        // crítico
            secondsRemaining <= 2 -> IndustrialTheme.Advertencia  // advertencia
            else -> IndustrialTheme.Primario                      // normal
        }
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = IndustrialTheme.TarjetaAlta
        )
        Text(
            text = "Timeout en ${secondsRemaining}s",
            fontSize = 12.sp,
            color = progressColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Preview para testing en Compose Preview
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun AuthorizationDialogPreview() {
    MaterialTheme {
        AuthorizationDialog(
            deviceName = "ESP32_SCORBOT_EEFF",
            mac = "AA:BB:CC:DD:EE:FF",
            appType = "SCORBOT",
            onApprove = { remembered ->
                println("[OK] Aprobado (Remember: $remembered)")
            },
            onReject = { remembered ->
                println("[ERR] Rechazado (Remember: $remembered)")
            },
            onDismiss = {
                println("× Cancelado")
            }
        )
    }
}
