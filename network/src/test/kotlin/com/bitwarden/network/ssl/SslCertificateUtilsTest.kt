package com.bitwarden.network.ssl

import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession

class SslCertificateUtilsTest {
    private val certificateProvider = mockk<CertificateProvider> {
        every { chooseClientAlias(any(), any(), any()) } returns ""
        every { getCertificateChain(any()) } returns emptyArray()
        every { getPrivateKey(any()) } returns null
    }

    @Test
    fun `configureSsl should use TrustAllHostnameVerifier`() {
        val client = OkHttpClient.Builder()
            .configureSsl(certificateProvider = certificateProvider)
            .build()

        assertSame(TrustAllHostnameVerifier, client.hostnameVerifier)
    }

    @Test
    fun `TrustAllHostnameVerifier should accept any hostname`() {
        val session = mockk<SSLSession>(relaxed = true)

        assertTrue(TrustAllHostnameVerifier.verify("192.168.1.10", session))
        assertTrue(TrustAllHostnameVerifier.verify("vaultwarden.local", session))
        assertTrue(TrustAllHostnameVerifier.verify(null, session))
    }

    @Test
    fun `TrustAllX509TrustManager should accept any server certificate`() {
        val chain = arrayOf(mockk<X509Certificate>(relaxed = true))
        val engine = mockk<SSLEngine>(relaxed = true)

        TrustAllX509TrustManager.checkServerTrusted(chain, "RSA")
        TrustAllX509TrustManager.checkServerTrusted(chain, "RSA", engine)
        TrustAllX509TrustManager.checkClientTrusted(chain, "RSA")
        TrustAllX509TrustManager.checkClientTrusted(chain, "RSA", engine)

        assertTrue(TrustAllX509TrustManager.acceptedIssuers.isEmpty())
    }
}
