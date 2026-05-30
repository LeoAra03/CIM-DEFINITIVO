package com.industria.coordinacion.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.sistema.distribuido.network.prefecto.IndustrialActionButton
import com.sistema.distribuido.network.prefecto.IndustrialTheme
import kotlinx.coroutines.launch

@Composable
fun PermissionDialog(
    requestId: String,
    mac: String,
    appType: String,
    deviceName: String,
    onAuthorize: (String, Boolean) -> Unit,
    onReject: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { onClose() },
        title = { Text("Autorizar dispositivo", color = IndustrialTheme.Primario) },
        text = { Text("¿Autorizar $deviceName ($appType) desde $mac? ", color = IndustrialTheme.TextoSecundario) },
        confirmButton = {
            IndustrialActionButton(
                texto = "Aceptar Siempre",
                icono = Icons.Default.Check,
                colorFondo = IndustrialTheme.Primario,
                onClick = {
                    scope.launch {
                        onAuthorize(mac, true)
                        onClose()
                    }
                }
            )
        },
        dismissButton = {
            Row {
                IndustrialActionButton(
                    texto = "Aceptar Una Vez",
                    icono = Icons.Default.Timer,
                    colorFondo = IndustrialTheme.Secundario,
                    onClick = {
                        scope.launch {
                            onAuthorize(mac, false)
                            onClose()
                        }
                    }
                )

                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))

                IndustrialActionButton(
                    texto = "Rechazar",
                    icono = Icons.Default.Close,
                    colorFondo = IndustrialTheme.Error,
                    onClick = {
                        scope.launch {
                            onReject(mac)
                            onClose()
                        }
                    }
                )
            }
        }
    )
}

