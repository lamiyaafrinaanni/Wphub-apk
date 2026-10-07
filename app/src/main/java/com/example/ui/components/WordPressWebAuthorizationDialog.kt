package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.WordPressRestClient
import com.example.ui.theme.*
import com.example.util.WebViewCrashLogger
import java.net.URLEncoder

/**
 * Official WordPress Application Passwords Web Authorization & Request Flow.
 * 
 * Bypasses Hostinger CDN, Cloudflare WAF, and browser integrity checks by running
 * inside a full Chromium WebView with JavaScript and cookie support.
 * 
 * Flow:
 * 1. Opens 'wp-admin/authorize-application.php'.
 * 2. If user is not logged in, WordPress displays standard 'wp-login.php'.
 * 3. User logs in with admin username & password.
 * 4. WordPress automatically redirects to the Permission Request page:
 *    "Would you like to grant WPMobile Hub access to your site?".
 * 5. User clicks "Approve" (Accept Request).
 * 6. WordPress generates a secure Application Password and redirects to our callback URL.
 * 7. This dialog intercepts the credentials and seamlessly connects the app!
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressWebAuthorizationDialog(
    siteUrl: String,
    initialUsername: String = "",
    onDismiss: () -> Unit,
    onAuthorized: (siteUrl: String, username: String, applicationPassword: String) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val cleanBaseUrl = remember(siteUrl) {
        siteUrl.trim().trimEnd('/').let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
    }

    val callbackSuccessUrl = remember(cleanBaseUrl) { "$cleanBaseUrl/?wphub_auth_success=1" }
    val callbackRejectUrl = remember(cleanBaseUrl) { "$cleanBaseUrl/?wphub_auth_reject=1" }

    // Official WordPress Application Passwords requirement (wp_is_uuid in wp-admin/authorize-application.php):
    // The app_id MUST be a valid RFC 4122 UUID (v4), otherwise WordPress returns 400 'The application ID must be a UUID.'
    val applicationUuid = remember { "c79a83d4-6f2e-4b18-8a95-5d3e0b2c1f4e" }

    val initialAuthUrl = remember(cleanBaseUrl) {
        try {
            val encodedAppName = URLEncoder.encode("SiteDeck", "UTF-8")
            val encodedAppId = URLEncoder.encode(applicationUuid, "UTF-8")
            val encodedSuccess = URLEncoder.encode(callbackSuccessUrl, "UTF-8")
            val encodedReject = URLEncoder.encode(callbackRejectUrl, "UTF-8")
            "$cleanBaseUrl/wp-admin/authorize-application.php?app_name=$encodedAppName&app_id=$encodedAppId&success_url=$encodedSuccess&reject_url=$encodedReject"
        } catch (e: Exception) {
            val encodedAppName = URLEncoder.encode("SiteDeck", "UTF-8")
            val encodedAppId = URLEncoder.encode(applicationUuid, "UTF-8")
            "$cleanBaseUrl/wp-admin/authorize-application.php?app_name=$encodedAppName&app_id=$encodedAppId"
        }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialAuthUrl) }
    var pageTitle by remember { mutableStateOf("WordPress Authorization") }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }

    var detectedPassword by remember { mutableStateOf<String?>(null) }
    var manualPasswordInput by remember { mutableStateOf("") }
    var showManualPasteRow by remember { mutableStateOf(false) }
    var showCrashLogsSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WordPress Web Login & Permission",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = cleanBaseUrl.removePrefix("https://").removePrefix("http://"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = { webViewInstance?.goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCrashLogsSheet = true }) {
                            Icon(Icons.Default.BugReport, contentDescription = "WebView Crash & -1 Logs", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    onShowMessage("Could not open browser: ${e.message}")
                                }
                            }
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in External Browser")
                        }
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                DisposableEffect(Unit) {
                    onDispose {
                        try {
                            webViewInstance?.stopLoading()
                            webViewInstance?.destroy()
                            webViewInstance = null
                        } catch (e: Exception) {
                            // ignore cleanup exceptions
                        }
                    }
                }

                // Loading Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = PrimaryIndigo,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Educational Helper Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "1. Log into WordPress if prompted.\n2. Tap 'Approve' (এক্সেপ্ট) to generate password and connect.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Embedded Android WebView
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx: android.content.Context ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                try {
                                    // Prevent hardware rendernode crash in software/emulator rendering environments (Mesa)
                                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                } catch (e: Exception) {
                                    // fallback
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    userAgentString = WordPressRestClient.STANDARD_USER_AGENT
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) pageTitle = title
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                        val url = view?.url ?: currentUrl
                                        val didCrash = detail?.didCrash() == true
                                        val priority = detail?.rendererPriorityAtExit() ?: -1

                                        WebViewCrashLogger.logRendererCrash(
                                            url = url,
                                            didCrash = didCrash,
                                            rendererPriority = priority,
                                            webView = view,
                                            extraInfo = "WordPress Web Authorization Dialog"
                                        )

                                        onShowMessage("WebView renderer ${if (didCrash) "crashed" else "terminated"}. Crash log recorded.")

                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (e: Exception) {
                                            // ignore cleanup
                                        }
                                        return true
                                    }

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        val failingUrl = request?.url?.toString() ?: view?.url ?: currentUrl
                                        val errorCode = error?.errorCode ?: -1
                                        val description = error?.description?.toString() ?: "Unknown error"
                                        val isMainFrame = request?.isForMainFrame == true

                                        WebViewCrashLogger.logWebViewError(
                                            url = failingUrl,
                                            errorCode = errorCode,
                                            description = description,
                                            isMainFrame = isMainFrame,
                                            webView = view,
                                            extraInfo = "WordPress Web Auth"
                                        )

                                        if (errorCode == -1 && isMainFrame) {
                                            onShowMessage("WebView Error -1 on $failingUrl")
                                        }
                                    }

                                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                        val failingUrl = request?.url?.toString() ?: view?.url ?: currentUrl
                                        val statusCode = errorResponse?.statusCode ?: 0
                                        val reason = errorResponse?.reasonPhrase ?: ""
                                        val isMainFrame = request?.isForMainFrame == true

                                        WebViewCrashLogger.logHttpError(
                                            url = failingUrl,
                                            statusCode = statusCode,
                                            reasonPhrase = reason,
                                            isMainFrame = isMainFrame,
                                            webView = view
                                        )
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        if (url != null) {
                                            currentUrl = url
                                            canGoBack = view?.canGoBack() == true
                                            checkUrlForAuthorizedCredentials(url)
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return checkUrlForAuthorizedCredentials(url)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        canGoBack = view?.canGoBack() == true
                                        if (url != null) {
                                            currentUrl = url
                                            checkUrlForAuthorizedCredentials(url)
                                        }

                                        // Evaluate JS to detect if password is displayed on screen
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                try {
                                                    var codeEl = document.querySelector('.application-password-display, #application-passwords-user-api, .notice-success code, input#new_application_password');
                                                    if (codeEl) {
                                                        var val = codeEl.value || codeEl.innerText || codeEl.textContent || '';
                                                        return val.trim();
                                                    }
                                                } catch(e) {}
                                                return '';
                                            })();
                                            """.trimIndent()
                                        ) { jsResult ->
                                            val cleanResult = jsResult?.trim('"')?.trim()
                                            if (!cleanResult.isNullOrBlank() && cleanResult != "null" && cleanResult.length >= 16) {
                                                detectedPassword = cleanResult
                                                onShowMessage("Application Password captured from WordPress!")
                                            }
                                        }
                                    }

                                    private fun isSameHost(url1: String, url2: String): Boolean {
                                        val host1 = Uri.parse(url1).host?.lowercase()?.removePrefix("www.") ?: ""
                                        val host2 = Uri.parse(url2).host?.lowercase()?.removePrefix("www.") ?: ""
                                        return host1.isNotEmpty() && host1 == host2
                                    }

                                    private fun checkUrlForAuthorizedCredentials(url: String): Boolean {
                                        try {
                                            val uri = Uri.parse(url)
                                            
                                            // R-SEC-7: Verify that redirect host matches cleanBaseUrl
                                            if (uri.getQueryParameter("password") != null && !isSameHost(url, cleanBaseUrl)) {
                                                onShowMessage("Security Alert: Authorization host mismatch detected!")
                                                return false
                                            }

                                            val passwordParam = uri.getQueryParameter("password")
                                            val userParam = uri.getQueryParameter("user_login") ?: initialUsername
                                            val siteParam = uri.getQueryParameter("site_url") ?: cleanBaseUrl

                                            // R-SEC-7: Verify host of site_url param against cleanBaseUrl
                                            if (!passwordParam.isNullOrBlank() && !isSameHost(siteParam, cleanBaseUrl)) {
                                                onShowMessage("Security Alert: Authorization redirect domain mismatch detected!")
                                                return false
                                            }

                                            if (!passwordParam.isNullOrBlank()) {
                                                onShowMessage("Permission Approved! Connecting...")
                                                val cleanedPass = passwordParam.replace(" ", "").trim()
                                                onAuthorized(siteParam, userParam.ifBlank { "admin" }, cleanedPass)
                                                return true
                                            }

                                            if (url.contains("wphub_auth_reject=1") || url.contains("action=reject")) {
                                                onShowMessage("Authorization was declined.")
                                                onDismiss()
                                                return true
                                            }
                                        } catch (e: Exception) {
                                            // ignore parse exceptions
                                        }
                                        return false
                                     }
                                 }

                                loadUrl(initialAuthUrl)
                                webViewInstance = this
                            }
                         },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("webview_wp_admin_auth")
                    )
                }

                // If password was detected on-screen or user wants manual confirmation
                AnimatedVisibility(visible = detectedPassword != null) {
                    Surface(
                        color = EmeraldSuccessBg,
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Password Generated Successfully!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Code: ${detectedPassword?.take(6)}...${detectedPassword?.takeLast(4)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val pass = detectedPassword ?: ""
                                    onAuthorized(cleanBaseUrl, initialUsername.ifBlank { "admin" }, pass)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Connection Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Bottom Action Bar & Fallback Assist
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showManualPasteRow = !showManualPasteRow }) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (showManualPasteRow) "Hide Code Box" else "Enter Code Manually",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    // Trigger JS check or manual completion
                                    webViewInstance?.evaluateJavascript(
                                        """
                                        (function() {
                                            var code = document.querySelector('.application-password-display, #application-passwords-user-api, .notice-success code, input#new_application_password');
                                            return code ? (code.value || code.innerText || '') : '';
                                        })()
                                        """.trimIndent()
                                    ) { res ->
                                        val c = res?.trim('"')?.trim()
                                        if (!c.isNullOrBlank() && c != "null") {
                                            onAuthorized(cleanBaseUrl, initialUsername.ifBlank { "admin" }, c)
                                        } else {
                                            onShowMessage("Please tap 'Approve' on the webpage first.")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Text("I Approved It", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (showManualPasteRow) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualPasswordInput,
                                    onValueChange = { manualPasswordInput = it },
                                    label = { Text("Paste Generated Code Here") },
                                    placeholder = { Text("xxxx xxxx xxxx xxxx") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (manualPasswordInput.isNotBlank()) {
                                            onAuthorized(cleanBaseUrl, initialUsername.ifBlank { "admin" }, manualPasswordInput.trim())
                                        } else {
                                            onShowMessage("Please enter the generated password")
                                        }
                                    },
                                    enabled = manualPasswordInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Connect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCrashLogsSheet) {
        WebViewCrashLogSheet(
            onDismiss = { showCrashLogsSheet = false },
            onShowMessage = onShowMessage
        )
    }
}
