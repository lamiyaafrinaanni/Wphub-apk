package com.example.util

import android.net.Uri
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Event type classification for WebView renderer process crashes and resource errors.
 */
enum class WebViewEventType {
    RENDERER_CRASH,           // Renderer process terminated due to crash (didCrash = true)
    RENDERER_KILLED,          // Renderer process killed by OS (e.g. OOM / low memory)
    ERROR_UNKNOWN_MINUS_ONE,  // -1 (ERROR_UNKNOWN) WebView code
    WEB_RESOURCE_ERROR,       // Other WebView resource errors (e.g. -2 HOST_LOOKUP, -6 CONNECT)
    HTTP_ERROR,               // HTTP Status errors (403, 500, 502, 503 WAF blocks)
    NAVIGATION                // WebView navigation checkpoint
}

/**
 * Data model for tracking WebView renderer process crashes, -1 error codes, and site configurations.
 */
data class WebViewCrashLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
    val url: String,
    val host: String = parseHost(url),
    val eventType: WebViewEventType,
    val errorCode: Int,
    val errorDescription: String,
    val didCrash: Boolean? = null,
    val rendererPriority: Int? = null,
    val isMainFrame: Boolean = true,
    val siteConfiguration: String = "",
    val possibleRootCause: String = determineRootCause(eventType, errorCode, url, errorDescription),
    val recommendedAction: String = determineRecommendedAction(eventType, errorCode, errorDescription)
) {
    companion object {
        fun parseHost(rawUrl: String): String {
            return try {
                if (rawUrl.isBlank()) return "unknown-host"
                val uri = Uri.parse(if (!rawUrl.startsWith("http")) "https://$rawUrl" else rawUrl)
                uri.host ?: rawUrl
            } catch (e: Exception) {
                rawUrl
            }
        }

        fun determineRootCause(
            eventType: WebViewEventType,
            errorCode: Int,
            url: String,
            description: String
        ): String {
            return when {
                eventType == WebViewEventType.RENDERER_CRASH -> {
                    "Chromium renderer process crashed while processing web assets on $url. Common triggers: WebGL/Canvas memory overflow, heavy JS scripts (e.g. Gutenberg/Elementor), or GPU/Mesa hardware rendering incompatibilities."
                }
                eventType == WebViewEventType.RENDERER_KILLED -> {
                    "Android system terminated the WebView renderer process due to memory pressure or background process limits."
                }
                errorCode == -1 || eventType == WebViewEventType.ERROR_UNKNOWN_MINUS_ONE -> {
                    when {
                        url.contains("cloudflare") || description.contains("Cloudflare", ignoreCase = true) ->
                            "Cloudflare WAF / Bot Management challenge page blocked or killed the embedded WebView connection (Error -1)."
                        url.contains("wp-login.php") || url.contains("authorize-application.php") ->
                            "WordPress login or application password auth page triggered a script error, CORS/CSP restriction, or SSL handshake issue (Error -1)."
                        else ->
                            "Generic Chromium WebView engine error (-1 ERROR_UNKNOWN). The site configuration or server reset the connection before HTML rendering completed."
                    }
                }
                errorCode == -2 -> "Server domain name resolution failed (ERROR_HOST_LOOKUP)."
                errorCode == -6 -> "Connection timed out or failed to establish TCP handshake with $url (ERROR_CONNECT)."
                errorCode == -8 -> "Too many HTTP redirects triggered on $url (ERROR_TOO_MANY_REDIRECTS)."
                eventType == WebViewEventType.HTTP_ERROR && errorCode == 403 ->
                    "Server or WAF (Hostinger / Cloudflare) returned 403 Forbidden. Embedded WebView User-Agent or cookie blocked."
                eventType == WebViewEventType.HTTP_ERROR && errorCode >= 500 ->
                    "WordPress server internal error (HTTP $errorCode) occurred during request processing."
                else -> "Web resource error $errorCode: $description"
            }
        }

        fun determineRecommendedAction(
            eventType: WebViewEventType,
            errorCode: Int,
            description: String
        ): String {
            return when {
                eventType == WebViewEventType.RENDERER_CRASH || eventType == WebViewEventType.RENDERER_KILLED ->
                    "Ensure WebView layer type is set to SOFTWARE (LAYER_TYPE_SOFTWARE) in emulator/Mesa environments and clear Chromium cache."
                errorCode == -1 || eventType == WebViewEventType.ERROR_UNKNOWN_MINUS_ONE ->
                    "Use a standard desktop/mobile browser User-Agent string, enable cookies/DOM storage, and pass 'X-Requested-With' headers to bypass WAF challenges."
                errorCode == -6 || errorCode == -2 ->
                    "Check internet connection, verify SSL certificate validity, and confirm WordPress REST permalink configuration."
                else ->
                    "Review network request logs and site security plugins (Wordfence, iThemes Security) that may block automated API calls."
            }
        }
    }
}

/**
 * Singleton logger utility to track WebView renderer process crashes,
 * network errors, and site configurations triggering error code -1.
 */
object WebViewCrashLogger {
    private val _logs = MutableStateFlow<List<WebViewCrashLog>>(emptyList())
    val logs: StateFlow<List<WebViewCrashLog>> = _logs.asStateFlow()

    private const val MAX_LOG_CAPACITY = 200

    /**
     * Log a Chromium WebView renderer process crash or termination (onRenderProcessGone).
     */
    fun logRendererCrash(
        url: String?,
        didCrash: Boolean,
        rendererPriority: Int,
        webView: WebView? = null,
        extraInfo: String = ""
    ) {
        val targetUrl = url?.ifBlank { webView?.url } ?: "https://unknown-wordpress-site.com"
        val config = buildSiteConfigString(webView, extraInfo)
        val eventType = if (didCrash) WebViewEventType.RENDERER_CRASH else WebViewEventType.RENDERER_KILLED

        val log = WebViewCrashLog(
            url = targetUrl,
            eventType = eventType,
            errorCode = -1,
            errorDescription = if (didCrash) "Renderer process crashed (didCrash=true)" else "Renderer process killed by system (didCrash=false)",
            didCrash = didCrash,
            rendererPriority = rendererPriority,
            siteConfiguration = config
        )

        addLog(log)
    }

    /**
     * Log a WebView error received via onReceivedError (including -1 ERROR_UNKNOWN).
     */
    fun logWebViewError(
        url: String?,
        errorCode: Int,
        description: String?,
        isMainFrame: Boolean = true,
        webView: WebView? = null,
        extraInfo: String = ""
    ) {
        val targetUrl = url?.ifBlank { webView?.url } ?: "https://unknown-wordpress-site.com"
        val config = buildSiteConfigString(webView, extraInfo)
        val desc = description ?: "Unknown WebView error"
        val eventType = if (errorCode == -1) WebViewEventType.ERROR_UNKNOWN_MINUS_ONE else WebViewEventType.WEB_RESOURCE_ERROR

        val log = WebViewCrashLog(
            url = targetUrl,
            eventType = eventType,
            errorCode = errorCode,
            errorDescription = desc,
            isMainFrame = isMainFrame,
            siteConfiguration = config
        )

        addLog(log)
    }

    /**
     * Log an HTTP error received via onReceivedHttpError.
     */
    fun logHttpError(
        url: String?,
        statusCode: Int,
        reasonPhrase: String?,
        isMainFrame: Boolean = true,
        webView: WebView? = null
    ) {
        val targetUrl = url?.ifBlank { webView?.url } ?: "https://unknown-wordpress-site.com"
        val config = buildSiteConfigString(webView)
        val reason = reasonPhrase ?: "HTTP Error $statusCode"

        val log = WebViewCrashLog(
            url = targetUrl,
            eventType = WebViewEventType.HTTP_ERROR,
            errorCode = statusCode,
            errorDescription = "HTTP $statusCode: $reason",
            isMainFrame = isMainFrame,
            siteConfiguration = config
        )

        addLog(log)
    }

    /**
     * Log page navigation start or override.
     */
    fun logNavigation(url: String?, webView: WebView? = null, extraInfo: String = "") {
        val targetUrl = url?.ifBlank { webView?.url } ?: return
        val log = WebViewCrashLog(
            url = targetUrl,
            eventType = WebViewEventType.NAVIGATION,
            errorCode = 0,
            errorDescription = "Navigated to $targetUrl",
            siteConfiguration = buildSiteConfigString(webView, extraInfo)
        )
        addLog(log)
    }

    /**
     * Helper to clear recorded logs.
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Inject a simulated crash or -1 error for diagnostic UI testing.
     */
    fun simulateTestCrashLog(sampleSiteUrl: String = "https://example-wordpress.com/wp-admin/authorize-application.php") {
        val simulatedLog = WebViewCrashLog(
            url = sampleSiteUrl,
            eventType = WebViewEventType.ERROR_UNKNOWN_MINUS_ONE,
            errorCode = -1,
            errorDescription = "Simulated Chromium renderer process crash / -1 error code test",
            didCrash = true,
            rendererPriority = 0,
            isMainFrame = true,
            siteConfiguration = "UserAgent: Mozilla/5.0 (Linux; Android 14) Chrome/122.0 | JS: Enabled | Cookies: Enabled | LayerType: SOFTWARE"
        )
        addLog(simulatedLog)
    }

    /**
     * Generate a comprehensive text diagnostic report for copying/sharing.
     */
    fun exportFormattedReport(): String {
        val currentLogs = _logs.value
        return buildString {
            appendLine("==================================================")
            appendLine("       WEBVIEW RENDERER CRASH & ERROR REPORT      ")
            appendLine("==================================================")
            appendLine("Total Events Captured: ${currentLogs.size}")
            appendLine("Renderer Crashes: ${currentLogs.count { it.eventType == WebViewEventType.RENDERER_CRASH }}")
            appendLine("-1 Error Codes: ${currentLogs.count { it.errorCode == -1 }}")
            appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL} (Android API ${Build.VERSION.SDK_INT})")
            appendLine("--------------------------------------------------")

            if (currentLogs.isEmpty()) {
                appendLine("No WebView renderer crashes or -1 errors recorded yet.")
            } else {
                currentLogs.forEachIndexed { idx, item ->
                    appendLine("[#${idx + 1}] Time: ${item.formattedTime} | Code: ${item.errorCode} (${item.eventType.name})")
                    appendLine("  URL: ${item.url}")
                    appendLine("  Host: ${item.host}")
                    appendLine("  Description: ${item.errorDescription}")
                    if (item.didCrash != null) appendLine("  didCrash: ${item.didCrash} | Renderer Priority: ${item.rendererPriority}")
                    if (item.siteConfiguration.isNotBlank()) appendLine("  Site Config: ${item.siteConfiguration}")
                    appendLine("  Possible Root Cause: ${item.possibleRootCause}")
                    appendLine("  Recommended Fix: ${item.recommendedAction}")
                    appendLine("--------------------------------------------------")
                }
            }
        }
    }

    private fun addLog(log: WebViewCrashLog) {
        val updated = listOf(log) + _logs.value
        _logs.value = if (updated.size > MAX_LOG_CAPACITY) updated.take(MAX_LOG_CAPACITY) else updated
    }

    private fun buildSiteConfigString(webView: WebView?, extraInfo: String = ""): String {
        return buildString {
            if (webView != null) {
                try {
                    val settings = webView.settings
                    append("JS: ${settings.javaScriptEnabled} | ")
                    append("DOMStorage: ${settings.domStorageEnabled} | ")
                    append("LayerType: ${webView.layerType} | ")
                    append("UserAgent: ${settings.userAgentString.take(45)}... | ")
                } catch (e: Exception) {
                    append("WebView details unavailable | ")
                }
            }
            append("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            if (extraInfo.isNotBlank()) {
                append(" | Info: $extraInfo")
            }
        }
    }
}
