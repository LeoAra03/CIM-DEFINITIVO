package com.sistema.distribuido.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.sistema.distribuido.network.protocol.CimProtocol

/**
 * Publica el Hub CIM en la red local vía NSD (Zero-Config).
 * Servicio: _cim-hub._tcp — puerto CimProtocol.WIFI_PORT
 */
class CimNsdPublisher(
    context: Context,
    private val port: Int = CimProtocol.WIFI_PORT,
    private val serviceName: String = "CIM-Hub"
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    fun start(onLog: (String) -> Unit = {}) {
        if (isRegistered) return
        val serviceInfo = NsdServiceInfo().apply {
            setServiceName(serviceName)
            setServiceType(CimProtocol.NSD_SERVICE_TYPE)
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                isRegistered = true
                onLog("✓ NSD publicado: ${info.serviceName} puerto ${info.port}")
                Log.i(TAG, "NSD registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                onLog("✗ NSD registro fallido: código $errorCode")
                Log.e(TAG, "NSD registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                isRegistered = false
                onLog("NSD desregistrado")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD unregistration failed: $errorCode")
            }
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener!!)
    }

    fun stop() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering NSD: ${e.message}")
            }
        }
        registrationListener = null
        isRegistered = false
    }

    companion object {
        private const val TAG = "CimNsdPublisher"
    }
}
