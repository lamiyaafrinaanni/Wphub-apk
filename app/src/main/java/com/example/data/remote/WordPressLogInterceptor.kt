package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Data model representing a single WordPress REST API call for in-app inspection & debugging.
 */
data class WordPressApiLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String? = null,
    val statusCode: Int = 0,
    val statusMessage: String = "",
    val durationMs: Long = 0,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBodyPreview: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val diagnosticAdvice: String? = null
)

/**
 * Singleton store that retains recent WordPress REST API interaction logs.
 */
object WordPressLogStore {
    private const val MAX_LOGS = 100
    private val _logs = MutableStateFlow<List<WordPressApiLogEntry>>(emptyList())
    val logs: StateFlow<List<WordPressApiLogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun addLog(entry: WordPressApiLogEntry) {
        val current = _logs.value.toMutableList()
        current.add(0, entry) // Newest first
        if (current.size > MAX_LOGS) {
            _logs.value = current.take(MAX_LOGS)
        } else {
            _logs.value = current
        }
    }

    @Synchronized
    fun clearLogs() {
        _logs.value = emptyList()
    }
}

/**
 * OkHttp LogInterceptor for WordPress REST API.
 * Intercepts, inspects, and logs all outgoing requests and incoming responses,
 * including headers, query params, status codes, response bodies, and diagnostic insights.
 */
class WordPressLogInterceptor(
    private val tag: String = "WordPressREST",
    private val maxBodyPreviewSize: Long = 1024 * 64 // 64 KB preview
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        // 1. Inspect Outgoing Request
        val method = request.method
        val url = request.url.toString()
        val requestHeadersMap = extractHeaders(request.headers)
        val requestBodyString = extractRequestBody(request)

        logOutgoingRequest(method, url, requestHeadersMap, requestBodyString)

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            val errorMsg = e.message ?: e.javaClass.simpleName
            Log.e(tag, "❌ [REQUEST FAILED] $method $url (${elapsedMs}ms): $errorMsg", e)

            val failedEntry = WordPressApiLogEntry(
                method = method,
                url = url,
                requestHeaders = requestHeadersMap,
                requestBody = requestBodyString,
                statusCode = 0,
                statusMessage = "Network Error",
                durationMs = elapsedMs,
                isSuccess = false,
                errorMessage = errorMsg,
                diagnosticAdvice = generateNetworkFailureAdvice(e, url)
            )
            WordPressLogStore.addLog(failedEntry)
            throw e
        }

        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

        // 2. Inspect Incoming Response
        val statusCode = response.code
        val statusMessage = response.message
        val responseHeadersMap = extractHeaders(response.headers)
        val responseBodyPreview = extractResponseBody(response)
        val isSuccessful = response.isSuccessful

        val diagnosticAdvice = if (!isSuccessful) {
            generateDiagnosticAdvice(statusCode, url, responseBodyPreview, requestHeadersMap)
        } else null

        logIncomingResponse(
            method = method,
            url = url,
            statusCode = statusCode,
            statusMessage = statusMessage,
            elapsedMs = elapsedMs,
            responseHeaders = responseHeadersMap,
            responseBodyPreview = responseBodyPreview,
            diagnosticAdvice = diagnosticAdvice
        )

        // 3. Record in LogStore
        val entry = WordPressApiLogEntry(
            method = method,
            url = url,
            requestHeaders = requestHeadersMap,
            requestBody = requestBodyString,
            statusCode = statusCode,
            statusMessage = statusMessage,
            durationMs = elapsedMs,
            responseHeaders = responseHeadersMap,
            responseBodyPreview = responseBodyPreview,
            isSuccess = isSuccessful,
            errorMessage = if (!isSuccessful) "HTTP $statusCode $statusMessage" else null,
            diagnosticAdvice = diagnosticAdvice
        )
        WordPressLogStore.addLog(entry)

        return response
    }

    private fun extractHeaders(headers: Headers): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val value = headers.value(i)
            if (name.equals("Authorization", ignoreCase = true)) {
                map[name] = maskAuthHeader(value)
            } else {
                map[name] = value
            }
        }
        return map
    }

    private fun maskAuthHeader(authHeader: String): String {
        return if (authHeader.startsWith("Basic ", ignoreCase = true)) {
            val rawBase64 = authHeader.substring(6).trim()
            try {
                val decoded = String(Base64.decode(rawBase64, Base64.DEFAULT), Charsets.UTF_8)
                val parts = decoded.split(":", limit = 2)
                if (parts.size == 2) {
                    val user = parts[0]
                    val passMasked = "•".repeat(parts[1].length.coerceAtMost(8))
                    "Basic $rawBase64 [Decoded: $user:$passMasked]"
                } else {
                    "Basic $rawBase64 [Decoded: $decoded]"
                }
            } catch (e: Exception) {
                "Basic ${rawBase64.take(10)}..."
            }
        } else {
            authHeader.take(15) + "..."
        }
    }

    private fun extractRequestBody(request: Request): String? {
        val body = request.body ?: return null
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            buffer.readString(charset).take(2048)
        } catch (e: Exception) {
            "<Unable to extract request body: ${e.message}>"
        }
    }

    private fun extractResponseBody(response: Response): String? {
        val body = response.body ?: return null
        return try {
            val peeked = response.peekBody(maxBodyPreviewSize)
            peeked.string()
        } catch (e: Exception) {
            "<Unable to preview response body: ${e.message}>"
        }
    }

    private fun logOutgoingRequest(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ) {
        val sb = StringBuilder()
        sb.appendLine("┌─── [WP REST API REQUEST] ──────────────────────────────────────────────")
        sb.appendLine("│ Method : $method")
        sb.appendLine("│ URL    : $url")
        sb.appendLine("│ Headers (${headers.size}):")
        headers.forEach { (name, value) ->
            sb.appendLine("│   $name: $value")
        }
        if (!body.isNullOrBlank()) {
            sb.appendLine("│ Body   : $body")
        }
        sb.appendLine("└─── [END REQUEST] ──────────────────────────────────────────────────────")
        Log.d(tag, sb.toString())
    }

    private fun logIncomingResponse(
        method: String,
        url: String,
        statusCode: Int,
        statusMessage: String,
        elapsedMs: Long,
        responseHeaders: Map<String, String>,
        responseBodyPreview: String?,
        diagnosticAdvice: String?
    ) {
        val sb = StringBuilder()
        val isError = statusCode !in 200..299
        val prefix = if (isError) "⚠️ [WP REST API ERROR $statusCode]" else "✅ [WP REST API RESPONSE $statusCode]"

        sb.appendLine("┌─── $prefix ─────────────────────────────────────────────")
        sb.appendLine("│ Request : $method $url")
        sb.appendLine("│ Status  : $statusCode $statusMessage (took ${elapsedMs}ms)")
        sb.appendLine("│ Response Headers (${responseHeaders.size}):")
        responseHeaders.forEach { (name, value) ->
            sb.appendLine("│   $name: $value")
        }

        if (!responseBodyPreview.isNullOrBlank()) {
            val truncated = if (responseBodyPreview.length > 500) {
                responseBodyPreview.take(500) + "... [truncated, total ${responseBodyPreview.length} chars]"
            } else {
                responseBodyPreview
            }
            sb.appendLine("│ Body Preview:")
            sb.appendLine("│   $truncated")
        }

        if (!diagnosticAdvice.isNullOrBlank()) {
            sb.appendLine("│ 💡 Diagnostic Advice:")
            diagnosticAdvice.lines().forEach { line ->
                sb.appendLine("│   $line")
            }
        }

        sb.appendLine("└─── [END RESPONSE] ─────────────────────────────────────────────────────")

        if (isError) {
            Log.w(tag, sb.toString())
        } else {
            Log.d(tag, sb.toString())
        }
    }

    private fun generateDiagnosticAdvice(
        statusCode: Int,
        url: String,
        bodyPreview: String?,
        requestHeaders: Map<String, String>
    ): String {
        return when (statusCode) {
            401 -> {
                "401 Unauthorized: WordPress rejected the credentials.\n" +
                "1. If using Application Passwords, make sure you created it under WP Admin > Users > Profile > Application Passwords.\n" +
                "2. Check if Apache/Nginx is stripping the 'Authorization' header. Add 'SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1' to your .htaccess file.\n" +
                "3. Ensure the username matches the WordPress user login, not just display name."
            }
            403 -> {
                "403 Forbidden: Access to REST endpoint is blocked.\n" +
                "1. Security plugins (e.g. Wordfence, iThemes Security, Sucuri, Cloudflare WAF) may be blocking REST API calls or Basic Auth.\n" +
                "2. Your WordPress user role may lack capability for this endpoint (e.g., need Administrator or Shop Manager for WooCommerce orders/products).\n" +
                "3. If using WooCommerce, verify that WooCommerce REST API is active and keys/passwords have Read/Write permissions."
            }
            404 -> {
                "404 Not Found: REST API route not found at '$url'.\n" +
                "1. Check if Pretty Permalinks are enabled: in WP Admin > Settings > Permalinks, select 'Post name' and save changes.\n" +
                "2. If querying WooCommerce endpoints (/wc/v3/), ensure WooCommerce plugin is installed and activated on the WordPress site.\n" +
                "3. Ensure the Site URL does not contain typos or extra subdirectories."
            }
            500 -> {
                "500 Internal Server Error: PHP error on WordPress server.\n" +
                "1. Check your WordPress 'wp-content/debug.log' for fatal errors or exhausted memory limits.\n" +
                "2. A plugin conflict or outdated theme might be hooking into the REST API response."
            }
            502, 503, 504 -> {
                "502/503/504 Gateway/Timeout Error: Server is temporarily unavailable or timing out.\n" +
                "1. Check server load, PHP max_execution_time, and Cloudflare/host status."
            }
            301, 302 -> {
                "301/302 Redirect: The URL was redirected. Ensure your site URL uses the exact canonical protocol (https:// vs http:// and www vs non-www)."
            }
            else -> "Unexpected HTTP $statusCode. Inspect response body snippet for WordPress error code."
        }
    }

    private fun generateNetworkFailureAdvice(e: Exception, url: String): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("failed to connect") || msg.contains("connection refused") -> {
                "Connection Refused: Server at '$url' is unreachable or port is closed. Verify domain is active and DNS is resolving."
            }
            msg.contains("timeout") -> {
                "Connection Timeout: The WordPress host took too long to respond. Check server load or hosting firewall."
            }
            msg.contains("ssl") || msg.contains("cert") -> {
                "SSL/TLS Certificate Error: HTTPS handshake failed. Ensure your SSL certificate is valid and not expired or self-signed."
            }
            msg.contains("cleartext") -> {
                "Cleartext HTTP Traffic Not Permitted: The site is using http:// instead of https://."
            }
            else -> "Network Error: ${e.message}. Check device internet connectivity and server availability."
        }
    }
}
