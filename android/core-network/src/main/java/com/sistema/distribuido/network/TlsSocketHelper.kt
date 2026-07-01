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
 * Modo desarrollo: trust-all (producción requiere certificados reales/mTLS).
 */
object TlsSocketHelper {

    var enabled: Boolean = false

    private val sslContext: SSLContext by lazy {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        })
        SSLContext.getInstance("TLSv1.3", "Conscrypt").apply {
            init(null, trustAll, java.security.SecureRandom())
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
        return socket
    }

    fun init(context: Context) {
        // Reservado para cargar certificados desde assets en producción
    }
}
