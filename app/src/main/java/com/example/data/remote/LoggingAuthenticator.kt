package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.text.SimpleDateFormat
import java.util.*

data class WordPress401LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String>,
    val responseCode: Int = 401,
    val responseMessage: String = "Unauthorized",
    val responseHeaders: Map<String, String>,
    val responseBodyRaw: String?,
    val wwwAuthenticateHeader: String?,
    val authorizationHeaderPresent: Boolean,
    val authorizationHeaderMasked: String?,
    val probableCause: String,
    val recommendedFix: String
)

object WordPress401LogStore {
    private const val MAX_LOGS = 100
    private val _logs = MutableStateFlow<List<WordPress401LogEntry>>(emptyList())
    val logs: StateFlow<List<WordPress401LogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun addLog(entry: WordPress401LogEntry) {
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

    fun buildFullDiagnosticReport(): String {
        val currentLogs = _logs.value
        if (currentLogs.isEmpty()) {
            return "No HTTP 401 Unauthorized log entries captured yet."
        }

        val sb = StringBuilder()
        sb.appendLine("# WordPress REST API 401 Diagnostic Report")
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("Total 401 Failures Recorded: ${currentLogs.size}\n")

        currentLogs.forEachIndexed { idx, entry ->
            sb.appendLine("## Entry #${idx + 1} - ${entry.timeFormatted} [${entry.method} ${entry.url}]")
            sb.appendLine("- **Response Code**: HTTP ${entry.responseCode} ${entry.responseMessage}")
            sb.appendLine("- **WWW-Authenticate**: ${entry.wwwAuthenticateHeader ?: "None provided by server"}")
            sb.appendLine("- **Authorization Sent**: ${if (entry.authorizationHeaderPresent) "Yes (${entry.authorizationHeaderMasked})" else "NO (Missing from request)"}")
            sb.appendLine("- **Probable Cause**: ${entry.probableCause}")
            sb.appendLine("- **Recommended Fix**: ${entry.recommendedFix}\n")

            sb.appendLine("### Exact Request Headers:")
            sb.appendLine("```http")
            entry.requestHeaders.forEach { (k, v) ->
                sb.appendLine("$k: $v")
            }
            sb.appendLine("```\n")

            sb.appendLine("### Exact Response Headers:")
            sb.appendLine("```http")
            entry.responseHeaders.forEach { (k, v) ->
                sb.appendLine("$k: $v")
            }
            sb.appendLine("```\n")

            sb.appendLine("### Raw Response Body:")
            sb.appendLine("```json")
            sb.appendLine(entry.responseBodyRaw ?: "<Empty Body>")
            sb.appendLine("```\n")
            sb.appendLine("--------------------------------------------------\n")
        }

        return sb.toString()
    }
}

class LoggingAuthenticator(
    private val tag: String = "LoggingAuthenticator"
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        try {
            val request = response.request
            val url = request.url.toString()
            val method = request.method

            // Capture exact request headers
            val reqHeadersMap = LinkedHashMap<String, String>()
            for (i in 0 until request.headers.size) {
                val name = request.headers.name(i)
                val value = request.headers.value(i)
                if (name.equals("Authorization", ignoreCase = true)) {
                    reqHeadersMap[name] = maskAuthHeader(value)
                } else {
                    reqHeadersMap[name] = value
                }
            }

            // Capture exact response headers
            val respHeadersMap = LinkedHashMap<String, String>()
            for (i in 0 until response.headers.size) {
                respHeadersMap[response.headers.name(i)] = response.headers.value(i)
            }

            val wwwAuthHeader = response.header("WWW-Authenticate")
            val authHeader = request.header("Authorization")
            val isAuthPresent = !authHeader.isNullOrBlank()
            val maskedAuth = if (isAuthPresent) maskAuthHeader(authHeader!!) else null

            // Read exact raw response body
            val rawBody = try {
                response.peekBody(1024 * 64).string()
            } catch (e: Exception) {
                "<Unable to read response body: ${e.message}>"
            }

            val (cause, fix) = analyze401Failure(
                url = url,
                isAuthPresent = isAuthPresent,
                authHeader = authHeader,
                wwwAuthHeader = wwwAuthHeader,
                responseBody = rawBody,
                respHeaders = respHeadersMap
            )

            val logEntry = WordPress401LogEntry(
                url = url,
                method = method,
                requestHeaders = reqHeadersMap,
                responseCode = response.code,
                responseMessage = response.message,
                responseHeaders = respHeadersMap,
                responseBodyRaw = rawBody,
                wwwAuthenticateHeader = wwwAuthHeader,
                authorizationHeaderPresent = isAuthPresent,
                authorizationHeaderMasked = maskedAuth,
                probableCause = cause,
                recommendedFix = fix
            )

            // Log to console/Logcat
            Log.e(tag, "🚨 [401 UNAUTHORIZED CAPTURED BY LoggingAuthenticator] $method $url")
            Log.e(tag, "   Auth Header Sent: $isAuthPresent ($maskedAuth)")
            Log.e(tag, "   WWW-Authenticate: $wwwAuthHeader")
            Log.e(tag, "   Cause: $cause")
            Log.e(tag, "   Fix: $fix")

            // Store in global 401 log store
            WordPress401LogStore.addLog(logEntry)

        } catch (e: Exception) {
            Log.e(tag, "Error in LoggingAuthenticator: ${e.message}", e)
        }

        // Return null to avoid infinite retry loop on 401
        return null
    }

    private fun maskAuthHeader(authHeader: String): String {
        return if (authHeader.startsWith("Basic ", ignoreCase = true)) {
            val rawBase64 = authHeader.substring(6).trim()
            try {
                val decoded = String(Base64.decode(rawBase64, Base64.DEFAULT), Charsets.UTF_8)
                val parts = decoded.split(":", limit = 2)
                if (parts.size == 2) {
                    val user = parts[0]
                    "Basic [PROTECTED_USER: $user, PASSWORD: ••••••••]"
                } else {
                    "Basic [PROTECTED_CREDENTIALS]"
                }
            } catch (e: Exception) {
                "Basic [PROTECTED_CREDENTIALS]"
            }
        } else {
            "Bearer [PROTECTED_TOKEN]"
        }
    }

    private fun analyze401Failure(
        url: String,
        isAuthPresent: Boolean,
        authHeader: String?,
        wwwAuthHeader: String?,
        responseBody: String?,
        respHeaders: Map<String, String>
    ): Pair<String, String> {
        val bodyLower = responseBody.orEmpty().lowercase()
        val serverHeader = respHeaders["Server"] ?: respHeaders["server"] ?: ""

        val isWpCannotView = bodyLower.contains("cannot_view") || bodyLower.contains("cannot list") || bodyLower.contains("rest_cannot")
        val isInvalidToken = bodyLower.contains("invalid") || bodyLower.contains("incorrect_password") || bodyLower.contains("revoked")
        val isHostinger = serverHeader.contains("hcdn", ignoreCase = true) || bodyLower.contains("hcdn")

        return when {
            !isAuthPresent -> {
                Pair(
                    "Authorization Header Missing: The HTTP request was dispatched without an 'Authorization' header.",
                    "Check client configuration to ensure credentials (username & Application Password) are attached before making REST calls."
                )
            }
            wwwAuthHeader == null && isAuthPresent -> {
                Pair(
                    "Header Stripped by Server (Apache/Nginx WAF): An 'Authorization' header was sent by the app, but the server returned 401 without a 'WWW-Authenticate' header, indicating the web server software stripped the header before PHP processed it.",
                    "Add the following rule to your WordPress root '.htaccess' file:\nSetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1\nIf using Nginx, add 'fastcgi_param HTTP_AUTHORIZATION \$http_authorization;' to your Nginx location block."
                )
            }
            isInvalidToken -> {
                Pair(
                    "Invalid Application Password: The username or application password token was rejected by WordPress core authentication.",
                    "Go to WP Admin > Users > Profile > Application Passwords, create a new Application Password, and update credentials via the 'Reconnect Site' dialog in WPMobile Hub."
                )
            }
            isWpCannotView -> {
                Pair(
                    "Insufficient User Role Permissions: The authenticated user login does not have capability to read/write this resource.",
                    "Log into WP Admin and verify that the user account role is Administrator or Shop Manager."
                )
            }
            isHostinger -> {
                Pair(
                    "Hostinger CDN / WAF Edge Rejection: Hostinger Security WAF intercepted the REST request at the CDN edge.",
                    "Disable 'Hostinger CDN' or 'Bot Protection' in Hostinger hPanel under Websites > Security > CDN."
                )
            }
            else -> {
                Pair(
                    "HTTP 401 Unauthorized: WordPress or a security plugin rejected authentication token.",
                    "Verify credentials, check .htaccess Authorization passthrough rules, and review WP Admin > Users Application Passwords list."
                )
            }
        }
    }
}
