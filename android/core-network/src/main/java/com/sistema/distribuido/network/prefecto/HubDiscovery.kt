package com.sistema.distribuido.network.prefecto

import androidx.compose.runtime.*
import com.sistema.distribuido.network.CimNsdDiscovery

/**
 * Auto-descubre IP del Hub CIM vía NSD y la expone como State.
 */
@Composable
fun rememberHubIp(context: android.content.Context, autoStart: Boolean = true): State<String?> {
    val discovery = remember { CimNsdDiscovery.getInstance(context) }
    val host by discovery.discoveredHost.collectAsState()

    LaunchedEffect(autoStart) {
        if (autoStart) discovery.start()
    }

    DisposableEffect(Unit) {
        onDispose { /* mantener discovery activo entre recomposiciones */ }
    }

    return remember(host) { derivedStateOf { host } }
}
