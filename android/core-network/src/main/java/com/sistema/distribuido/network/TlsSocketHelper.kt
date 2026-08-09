package com.sistema.distribuido.network

import android.content.Context
import org.conscrypt.Conscrypt
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.Security

/**
 * TLS 1.3 via Conscrypt para sockets TCP CIM.
 * CORREGIDO: eliminado trust-all. Usa TrustManagers del sistema o pinning desde assets/cim_ca.crt en producción.
 * Si se requiere modo desarrollo, usar Network Security Config con <trust-anchors> debug, no TrustManager vacío.
 */
object TlsSocketHelper {

    @Volatile
    var enabled: Boolean = false

    // Carga certificados del sistema por defecto (no trust-all)
    private val sslContext: SSLContext by lazy {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        // En producción: cargar keystore con CA interna desde assets
        // val tmf = TrustManagerFactory.getInstance(...); tmf.init(customKeyStore)
        val tmf = javax.net.ssl.TrustManagerFactory.getInstance(
            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
        )
        tmf.init(null as java.security.KeyStore?) // system CAs
        SSLContext.getInstance("TLSv1.3", "Conscrypt").apply {
            init(null, tmf.trustManagers, java.security.SecureRandom())
        }
    }

    private val factory: SSLSocketFactory by lazy { sslContext.socketFactory }

    fun createClientSocket(host: String, port: Int, timeoutMs: Int = 2000): Socket {
        if (!enabled) {
            return Socket().apply { connect(java.net.InetSocketAddress(host, port), timeoutMs) }
        }
        val socket = factory.createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")
        socket.soTimeout = timeoutMs
        // Fuerza handshake inmediato para detectar MITM temprano
        socket.startHandshake()
        return socket
    }

    fun init(context: Context) {
        // Reservado para cargar certificados desde assets/cim_ca.crt en producción
        // Ej: context.assets.open("cim_ca.crt").use { ... load into KeyStore ... }
    }
}
