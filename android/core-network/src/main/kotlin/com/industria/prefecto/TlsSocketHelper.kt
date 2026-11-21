package com.industria.prefecto

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
 * TLS 1.3 via Conscrypt for CIM TCP sockets.
 * Trust-all removed. Uses system TrustManagers or certificate pinning from assets/cim_ca.crt in production.
 * For development mode, use a Network Security Config with debug <trust-anchors>, never an empty TrustManager.
 */
object TlsSocketHelper {

    @Volatile
    var enabled: Boolean = false

    // Loads the default system certificates (no trust-all)
    private val sslContext: SSLContext by lazy {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        // In production: load a keystore with the internal CA from assets
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
        // Force an immediate handshake to detect MITM early
        socket.startHandshake()
        return socket
    }

    fun init(context: Context) {
        // Reserved for loading certificates from assets/cim_ca.crt in production
        // Ej: context.assets.open("cim_ca.crt").use { ... load into KeyStore ... }
    }
}
