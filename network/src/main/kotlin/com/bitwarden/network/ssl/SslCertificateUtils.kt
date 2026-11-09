package com.bitwarden.network.ssl

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

/**
 * Creates an [OkHttpClient] configured with mTLS support using this [CertificateProvider].
 *
 * The returned client will present the client certificate from this provider during TLS
 * handshakes, allowing requests to pass through mTLS checks.
 */
fun CertificateProvider.createMtlsOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .configureSsl(certificateProvider = this)
        .build()

/**
 * Configures the [OkHttpClient.Builder] to use the a `SSLSocketFactory` as provided by the
 * [CertificateProvider].
 *
 * Server certificate and hostname verification are skipped so self-hosted servers such as
 * local Vaultwarden instances with self-signed certificates can complete a TLS handshake.
 */
fun OkHttpClient.Builder.configureSsl(
    certificateProvider: CertificateProvider,
): OkHttpClient.Builder {
    val trustManagers = sslTrustManagers
    val sslContext = certificateProvider.createSslContext(trustManagers = trustManagers)
    return sslSocketFactory(
        sslContext.socketFactory,
        trustManagers.first() as X509TrustManager,
    )
        .hostnameVerifier(TrustAllHostnameVerifier)
}

/**
 * Creates an [SSLContext] configured with mTLS support using this [CertificateProvider].
 *
 * The returned SSLContext will present the client certificate from this provider during
 * TLS handshakes, enabling mutual TLS authentication.
 */
private fun CertificateProvider.createSslContext(
    trustManagers: Array<TrustManager>,
): SSLContext = SSLContext.getInstance("TLS").apply {
    init(
        arrayOf(
            BitwardenX509ExtendedKeyManager(certificateProvider = this@createSslContext),
        ),
        trustManagers,
        null,
    )
}

/**
 * Trust managers that accept any server certificate, including self-signed certificates.
 */
private val sslTrustManagers: Array<TrustManager> = arrayOf(TrustAllX509TrustManager)

/**
 * Accepts every hostname, including local IPs that do not match the certificate SAN.
 */
@SuppressLint("BadHostnameVerifier")
internal object TrustAllHostnameVerifier : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean = true
}

/**
 * Trust manager that accepts every server certificate presented during TLS.
 *
 * Local Vaultwarden (and other self-hosted) servers commonly use a self-signed certificate
 * that is not present in Android's system or user CA stores. Android's default TrustManager
 * rejects those handshakes, which surfaces as "We couldn't verify the server's certificate."
 */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
internal object TrustAllX509TrustManager : X509ExtendedTrustManager() {
    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
    ) = Unit

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
    ) = Unit

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        socket: Socket?,
    ) = Unit

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        socket: Socket?,
    ) = Unit

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        engine: SSLEngine?,
    ) = Unit

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        engine: SSLEngine?,
    ) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
