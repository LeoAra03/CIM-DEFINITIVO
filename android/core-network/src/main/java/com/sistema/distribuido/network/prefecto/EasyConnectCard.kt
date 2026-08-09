package com.sistema.distribuido.network.prefecto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sistema.distribuido.network.protocol.CimProtocol

/**
 * EasyConnectCard - UI intuitiva para multiconectarse en 1-click
 * Objetivo: que operario no técnico pueda vincular estación al Hub sin manual
 */
@Composable
fun EasyConnectCard(
    ipCoordinator: String,
    onIpChange: (String) -> Unit,
    discoveredIp: String?,
    isConnectedNet: Boolean,
    isAuthorized: Boolean,
    authorizationState: String,
    independentMode: Boolean,
    onIndependentChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onDiscover: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    IndustrialCard(
        titulo = "🔗 Conexión Fácil al Hub",
        icono = Icons.Default.Lan,
        headerColor = if (isConnectedNet && isAuthorized) IndustrialTheme.Exito else IndustrialTheme.Primario,
        modifier = modifier
    ) {
        // Estado visual grande
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        !isConnectedNet -> Color(0xFF1A1A1A)
                        isAuthorized -> Color(0xFF0D2A12)
                        else -> Color(0xFF2A1F0D)
                    },
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    when {
                        !isConnectedNet -> IndustrialTheme.Borde
                        isAuthorized -> IndustrialTheme.Exito
                        else -> IndustrialTheme.Advertencia
                    },
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                when {
                                    !isConnectedNet -> Color.Gray
                                    isAuthorized -> Color.Green
                                    else -> Color.Yellow
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        text = when {
                            !isConnectedNet -> "DESCONECTADO"
                            isAuthorized -> "● VINCULADO Y AUTORIZADO"
                            else -> "◐ CONECTADO - Esperando autorización Hub"
                        },
                        color = when {
                            !isConnectedNet -> IndustrialTheme.TextoSecundario
                            isAuthorized -> IndustrialTheme.Exito
                            else -> IndustrialTheme.Advertencia
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        !isConnectedNet -> "Presiona 'Buscar y Conectar' - el sistema encuentra el Hub automáticamente por NSD"
                        isAuthorized -> "¡Listo! Puedes enviar comandos a hardware"
                        else -> "Hub recibió tu solicitud. Ve al Coordinador y autoriza este equipo"
                    },
                    color = IndustrialTheme.TextoSecundario,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // IP con auto-discovery highlight
        IndustrialTextField(
            valor = ipCoordinator,
            onValueChange = onIpChange,
            label = if (discoveredIp != null) "IP Coordinador (✓ Auto-detectado: $discoveredIp)" else "IP Coordinador (NSD auto)"
        )

        if (discoveredIp != null && discoveredIp != ipCoordinator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1F2D), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = IndustrialTheme.Secundario, modifier = Modifier.size(16.dp))
                Text(
                    "Hub encontrado en red: $discoveredIp - Click para usar",
                    fontSize = 11.sp,
                    color = IndustrialTheme.Secundario,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onIpChange(discoveredIp) }) { Text("USAR", fontSize = 10.sp) }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        IndustrialStatusRow("Enlace de Datos", if (isConnectedNet) "SINCRO OK" else "OFFLINE", isConnectedNet)
        IndustrialStatusRow("Autorización", authorizationState, isAuthorized)

        // Token validation
        val tokenOk = !CimProtocol.isDefaultTokenInUse() && CimProtocol.validateTokenStrength(CimProtocol.PASSWORD_ACTUAL)
        IndustrialStatusRow(
            "Token Seguridad",
            if (tokenOk) "Fuerte ✓" else "⚠ Default - Cambiar para producción",
            tokenOk
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Big connect button
        IndustrialActionButton(
            texto = if (!isConnectedNet) "🔍 Buscar Hub y Conectar (1-click)" else if (!isAuthorized) "⏳ Esperando Autorización..." else "✓ Conectado",
            icono = if (!isConnectedNet) Icons.Default.WifiFind else if (isAuthorized) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
            colorFondo = when {
                !isConnectedNet -> IndustrialTheme.Primario
                isAuthorized -> IndustrialTheme.Exito
                else -> IndustrialTheme.Advertencia
            },
            enabled = !isConnectedNet || !isAuthorized,
            onClick = onConnect
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Modo autónomo con explicación
        IndustrialCard("Modo Autónomo (Laboratorio sin Hub)", Icons.Default.Engineering, headerColor = IndustrialTheme.Tarjeta) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Activar modo sin coordinador", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Usa hardware directo sin VALIDADO - Solo para pruebas sin red",
                        color = IndustrialTheme.TextoSecundario,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = independentMode,
                    onCheckedChange = onIndependentChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = IndustrialTheme.Exito)
                )
            }
        }
    }
}

@Composable
fun rememberEasyConnectState(): String {
    // placeholder for future auto-discovery state
    return "idle"
}
