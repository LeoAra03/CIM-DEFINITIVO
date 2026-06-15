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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_dialog_info),
                    contentDescription = "Authorization Request",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Title
                Text(
                    text = "Nueva solicitud de autorización",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Device info card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
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
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Recordar esta decisión",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !hasResponded
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onApprove(rememberDecision)
                            hasResponded = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !hasResponded
                    ) {
                        Text("Aprobar", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Disclaimer
                Text(
                    text = "Responde en ${secondsRemaining}s o se rechazará automáticamente",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
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
            secondsRemaining <= 1 -> Color(0xFFD32F2F) // Rojo: crítico
            secondsRemaining <= 2 -> Color(0xFFF57C00) // Naranja: advertencia
            else -> MaterialTheme.colorScheme.primary    // Azul: normal
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
            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                println("✓ Aprobado (Remember: $remembered)")
            },
            onReject = { remembered ->
                println("✗ Rechazado (Remember: $remembered)")
            },
            onDismiss = {
                println("× Cancelado")
            }
        )
    }
}
