package com.example.camsender.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class DynamicSslManager(private val context: Context) {

    private val certsDir = File(context.filesDir, "certs").apply { if (!exists()) mkdirs() }

    fun isPaired(ip: String): Boolean {
        return getCertFile(ip).exists()
    }

    private fun getCertFile(ip: String): File = File(certsDir, "${ip.replace(".", "_")}.pem")

    /**
     * Step 2: Request server to generate and show OTP
     */
    suspend fun requestOtp(ip: String, port: Int): Result<Unit> {
        val url = "https://$ip:$port/init-pairing"
        val client = getUnsafeClient()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Server returned ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Step 4: Fetch certificate using the PIN entered by user
     */
    suspend fun fetchCertWithPin(ip: String, port: Int, pin: String): Result<Unit> {
        val url = "https://$ip:$port/cert?pin=$pin"
        val client = getUnsafeClient()
        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val pem = response.body?.string() ?: ""
                    getCertFile(ip).writeText(pem)
                    Log.d("DynamicSslManager", "Certificate saved for $ip")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Auth failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearPairing(ip: String) {
        getCertFile(ip).delete()
    }

    /**
     * Step 5: Get OkHttpClient that ONLY trusts the stored certificate for this IP
     */
    fun getStrictClient(ip: String): OkHttpClient? {
        val certFile = getCertFile(ip)
        if (!certFile.exists()) return null

        return try {
            val cf = CertificateFactory.getInstance("X.509")
            val cert = certFile.inputStream().use { cf.generateCertificate(it) as X509Certificate }

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry("server", cert)
            }

            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore)
            }

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, tmf.trustManagers, SecureRandom())
            }

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Still allow IP mismatch if needed, but cert is pinned
                .build()
        } catch (e: Exception) {
            Log.e("DynamicSslManager", "Failed to build strict client", e)
            null
        }
    }

    /**
     * Temporary client for TOFU steps (init-pairing and cert fetch)
     */
    private fun getUnsafeClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
