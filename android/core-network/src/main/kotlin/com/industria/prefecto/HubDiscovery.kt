package com.industria.prefecto

import androidx.compose.runtime.*
import com.industria.prefecto.CimNsdDiscovery

/**
 * Auto-discovers the CIM hub IP via NSD and exposes it as state.
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
