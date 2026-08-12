package com.sistema.distribuido.coordinador.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.sistema.distribuido.coordinador.ui.components.AuthorizationDialog
import com.sistema.distribuido.coordinador.viewmodels.HubViewModel
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * HUB SCREEN - COORDINADOR
 *
 * Pantalla principal que:
 * 1. Muestra lista de dispositivos (pendientes, autorizados, rechazados)
 * 2. Renderiza AuthorizationDialog modal para dispositivos nuevos
 * 3. Permite revocar autorizaciones
 */
@Composable
fun HubScreen(viewModel: HubViewModel = hiltViewModel()) {
    val pendingDevices by viewModel.pendingDevices.collectAsState()
    val authorizedDevices by viewModel.authorizedDevices.collectAsState()
    val rejectedDevices by viewModel.rejectedDevices.collectAsState()
    val authDialogState by viewModel.currentAuthorizationDialog.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IndustrialTheme.Fondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Header
            Text(
                text = "COORDINADOR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = IndustrialTheme.TextoPrincipal
            )
            Text(
                text = "Hub central de autorización",
                fontSize = 12.sp,
                color = IndustrialTheme.TextoSecundario,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Tabs / Sección de Dispositivos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Pendientes
                DeviceSection(
                    title = "PENDIENTES DE AUTORIZACIÓN (${pendingDevices.size})",
                    backgroundColor = IndustrialTheme.Tarjeta,
                    borderColor = IndustrialTheme.Advertencia
                ) {
                    if (pendingDevices.isEmpty()) {
                        Text("Sin dispositivos esperando", fontSize = 12.sp, color = IndustrialTheme.TextoTenue)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pendingDevices) { device ->
                                PendingDeviceCard(device)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Autorizados
                DeviceSection(
                    title = "AUTORIZADOS (${authorizedDevices.size})",
                    backgroundColor = IndustrialTheme.Tarjeta,
                    borderColor = IndustrialTheme.Primario
                ) {
                    if (authorizedDevices.isEmpty()) {
                        Text("Sin dispositivos autorizados", fontSize = 12.sp, color = IndustrialTheme.TextoTenue)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(authorizedDevices) { device ->
                                AuthorizedDeviceCard(
                                    device = device,
                                    onRevoke = { viewModel.revokeDevice(device.mac) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rechazados
                if (rejectedDevices.isNotEmpty()) {
                    DeviceSection(
                        title = "RECHAZADOS (${rejectedDevices.size})",
                        backgroundColor = IndustrialTheme.Tarjeta,
                        borderColor = IndustrialTheme.Error
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rejectedDevices) { mac ->
                                RejectedDeviceCard(mac)
                            }
                        }
                    }
                }
            }
        }

        // Modal: AuthorizationDialog
        AnimatedVisibility(
            visible = authDialogState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            authDialogState?.let { dialog ->
                AuthorizationDialog(
                    deviceName = dialog.name,
                    mac = dialog.mac,
                    appType = dialog.type,
                    timeoutSeconds = (dialog.timeoutMs / 1000).toInt(),
                    onApprove = { rememberDecision ->
                        viewModel.approveDevice(dialog.mac, rememberDecision)
                    },
                    onReject = { rememberDecision ->
                        viewModel.rejectDevice(dialog.mac, rememberDecision)
                    },
                    onDismiss = {
                        viewModel.dismissAuthorizationDialog()
                    }
                )
            }
        }
    }
}

@Composable
fun DeviceSection(
    title: String,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IndustrialTheme.RadioTarjeta),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialTheme.Borde)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IndustrialTheme.PaddingTarjeta)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(borderColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = borderColor
                )
            }
            content()
        }
    }
}

@Composable
fun PendingDeviceCard(device: HubViewModel.PendingDeviceState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
        color = IndustrialTheme.TarjetaAlta
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IndustrialTheme.TextoPrincipal)
                Text("MAC: ${device.mac}", fontSize = 10.sp, color = IndustrialTheme.TextoSecundario, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Text("ESPERANDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = IndustrialTheme.Advertencia)
        }
    }
}

@Composable
fun AuthorizedDeviceCard(
    device: HubViewModel.AuthorizedDeviceState,
    onRevoke: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
        color = IndustrialTheme.TarjetaAlta
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IndustrialTheme.TextoPrincipal)
                Text("MAC: ${device.mac}", fontSize = 10.sp, color = IndustrialTheme.TextoSecundario, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            IconButton(onClick = onRevoke, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Revocar",
                    tint = IndustrialTheme.Error
                )
            }
        }
    }
}

@Composable
fun RejectedDeviceCard(mac: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(IndustrialTheme.RadioControl),
        color = IndustrialTheme.TarjetaAlta
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Rechazado", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IndustrialTheme.TextoPrincipal)
                Text("MAC: $mac", fontSize = 10.sp, color = IndustrialTheme.TextoSecundario, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Text("BLOQUEADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = IndustrialTheme.Error)
        }
    }
}
