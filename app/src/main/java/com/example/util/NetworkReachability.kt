package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

sealed class ReachabilityResult {
    object Reachable : ReachabilityResult()
    data class Unreachable(val reason: String, val technicalDetails: String) : ReachabilityResult()
}

object NetworkReachability {

    private val preflightClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Checks if the target WordPress site URL is reachable before attempting authentication.
     */
    suspend fun verifyHostReachability(siteUrl: String): ReachabilityResult = withContext(Dispatchers.IO) {
        val cleanUrl = siteUrl.trim().let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }.removeSuffix("/")

        val request = Request.Builder()
            .url(cleanUrl)
            .head() // HEAD request is lightweight and fast, doesn't download the entire page
            .header("User-Agent", "SiteDeck-Reachability-Check")
            .build()

        try {
            preflightClient.newCall(request).execute().use { response ->
                // Any status code returned by the server (even 401, 403, 404, or 500) means the host is REACHABLE
                // because the web server actually accepted our TCP/HTTP request and replied.
                ReachabilityResult.Reachable
            }
        } catch (e: UnknownHostException) {
            ReachabilityResult.Unreachable(
                reason = "DNS Lookup Failed. The domain name does not exist, has expired, or your device is offline.",
                technicalDetails = "UnknownHostException: ${e.message}"
            )
        } catch (e: SocketTimeoutException) {
            ReachabilityResult.Unreachable(
                reason = "Connection Timed Out. The server took too long to respond. It might be offline, severely overloaded, or blocking your IP via a firewall.",
                technicalDetails = "SocketTimeoutException: ${e.message}"
            )
        } catch (e: ConnectException) {
            ReachabilityResult.Unreachable(
                reason = "Connection Refused. The server rejected the connection on ports 80/443. This usually means the web server (Apache/Nginx) is stopped or offline.",
                technicalDetails = "ConnectException: ${e.message}"
            )
        } catch (e: SSLHandshakeException) {
            ReachabilityResult.Unreachable(
                reason = "SSL Handshake Failed. Secure TLS/SSL connection could not be established. Ensure your SSL certificate is valid and not expired or self-signed.",
                technicalDetails = "SSLHandshakeException: ${e.message}"
            )
        } catch (e: IOException) {
            // Fallback for redirect loops, SSL protocol exceptions, etc.
            val msg = e.message ?: "Generic Network Error"
            if (msg.contains("cleartext", ignoreCase = true)) {
                ReachabilityResult.Unreachable(
                    reason = "Cleartext (HTTP) traffic not allowed by device security policy. If your site does not support HTTPS, please connect over a secure SSL link.",
                    technicalDetails = "IOException: Cleartext block"
                )
            } else {
                ReachabilityResult.Unreachable(
                    reason = "Network host is unreachable or blocked. Details: $msg",
                    technicalDetails = "IOException: $msg"
                )
            }
        } catch (e: Exception) {
            ReachabilityResult.Unreachable(
                reason = "An unexpected error occurred while reaching the server: ${e.message}",
                technicalDetails = "Exception: ${e.javaClass.simpleName}"
            )
        }
    }
}
