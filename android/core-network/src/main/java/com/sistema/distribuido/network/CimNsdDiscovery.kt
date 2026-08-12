package com.sistema.distribuido.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.sistema.distribuido.network.protocol.CimProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Descubre automáticamente la IP del Coordinador CIM en la LAN.
 */
class CimNsdDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredHost = MutableStateFlow<String?>(null)
    val discoveredHost: StateFlow<String?> = _discoveredHost.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    fun start(onLog: (String) -> Unit = {}) {
        if (isDiscovering) return
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                isDiscovering = true
                onLog("⟳ NSD: buscando ${CimProtocol.NSD_SERVICE_TYPE}...")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != CimProtocol.NSD_SERVICE_TYPE) return
                if (service.serviceName.contains("CIM", ignoreCase = true)) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            onLog("NSD resolve fallido: $errorCode")
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress
                            if (!host.isNullOrBlank()) {
                                _discoveredHost.value = host
                                onLog("Hub descubierto: $host:${info.port}")
                                Log.i(TAG, "Hub resolved: $host:${info.port}")
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                onLog("Hub perdido: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                onLog("NSD discovery fallido: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                onLog("NSD stop fallido: $errorCode")
            }
        }
        nsdManager.discoverServices(CimProtocol.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener!!)
    }

    fun stop() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping NSD: ${e.message}")
            }
        }
        discoveryListener = null
        isDiscovering = false
    }

    companion object {
        private const val TAG = "CimNsdDiscovery"

        fun getInstance(context: Context): CimNsdDiscovery {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CimNsdDiscovery(context.applicationContext).also { INSTANCE = it }
            }
        }

        @Volatile
        private var INSTANCE: CimNsdDiscovery? = null
    }
}
