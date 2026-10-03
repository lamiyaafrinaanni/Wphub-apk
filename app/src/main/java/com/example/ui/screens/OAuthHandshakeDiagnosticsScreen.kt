package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class HandshakeTraceStep(
    val stepNumber: Int,
    val title: String,
    val endpointUrl: String,
    val method: String,
    val statusCode: Int = 0,
    val statusMessage: String = "",
    val latencyMs: Long = 0,
    val isSuccess: Boolean = false,
    val isFailureOrigin: Boolean = false,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBodyRaw: String? = null,
    val diagnosticPinpoint: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthHandshakeDiagnosticsScreen(
    currentSite: SiteEntity?,
    onBackClick: (() -> Unit)? = null,
    onReconnectClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRunningDiagnostics by remember { mutableStateOf(false) }
    var traceSteps by remember { mutableStateOf<List<HandshakeTraceStep>>(emptyList()) }
    var failurePinpointSummary by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun runHandshakeDiagnostics() {
        val siteUrl = currentSite?.url ?: return
        val username = currentSite.username.ifBlank { "admin" }
        val token = com.example.data.security.SecureCredentialsVault.decrypt(currentSite.appPasswordToken).ifBlank { "password" }

        isRunningDiagnostics = true
        traceSteps = emptyList()
        failurePinpointSummary = null

        coroutineScope.launch {
            val steps = executeLiveHandshakeTrace(siteUrl, username, token)
            traceSteps = steps
            isRunningDiagnostics = false

            val originStep = steps.firstOrNull { it.isFailureOrigin }
            if (originStep != null) {
                failurePinpointSummary = "401 Error Originated at Step #${originStep.stepNumber}: ${originStep.title} (${originStep.endpointUrl})"
            } else if (steps.all { it.isSuccess }) {
                failurePinpointSummary = "OAuth & Application Password Handshake Passed 100%! All 4 steps responded with HTTP 200 OK."
            } else {
                failurePinpointSummary = "Handshake interrupted by server error or network timeout."
            }
        }
    }

    // Auto-run trace when opening screen
    LaunchedEffect(currentSite?.id, currentSite?.url) {
        if (currentSite != null && currentSite.url.isNotBlank()) {
            runHandshakeDiagnostics()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OAuth & App Password Handshake Trace",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = currentSite?.url ?: "WordPress Auth Debugger",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { runHandshakeDiagnostics() },
                        enabled = !isRunningDiagnostics
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Re-run Handshake Trace", tint = PrimaryIndigo)
                    }
                    if (traceSteps.isNotEmpty()) {
                        IconButton(onClick = {
                            val report = buildMarkdownTraceReport(currentSite, traceSteps, failurePinpointSummary)
                            copyToClipboard("OAuth Handshake Trace Report", report)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Full Trace", tint = PrimaryIndigo)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgGradientStart)
        ) {
            // Header Summary Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isRunningDiagnostics -> InfoBlue.copy(alpha = 0.12f)
                        traceSteps.any { it.isFailureOrigin } -> RoseErrorBg.copy(alpha = 0.7f)
                        else -> EmeraldSuccessBg.copy(alpha = 0.5f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        isRunningDiagnostics -> InfoBlue.copy(alpha = 0.3f)
                        traceSteps.any { it.isFailureOrigin } -> RoseError.copy(alpha = 0.3f)
                        else -> EmeraldSuccess.copy(alpha = 0.3f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("card_handshake_summary")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isRunningDiagnostics -> InfoBlue
                            traceSteps.any { it.isFailureOrigin } -> RoseError
                            else -> EmeraldSuccess
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRunningDiagnostics) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(
                                    imageVector = if (traceSteps.any { it.isFailureOrigin }) Icons.Default.VpnKeyOff else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isRunningDiagnostics -> "Executing Live Handshake Diagnostic..."
                                traceSteps.any { it.isFailureOrigin } -> "401 Failure Pinpointed!"
                                else -> "Handshake Health: 100% Passed"
                            },
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRunningDiagnostics -> InfoBlue
                                traceSteps.any { it.isFailureOrigin } -> RoseError
                                else -> EmeraldSuccess
                            },
                            fontSize = 15.sp
                        )
                        Text(
                            text = failurePinpointSummary ?: "Isolating HTTP request/response chain during OAuth Application Password authentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onReconnectClick != null && traceSteps.any { it.isFailureOrigin }) {
                        Button(
                            onClick = onReconnectClick,
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_reconnect_from_handshake")
                        ) {
                            Text("Reconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Handshake Chain Steps List
            if (isRunningDiagnostics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tracing HTTP Request/Response Chain...",
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Pinging REST index, testing Basic Auth header, and verifying WooCommerce endpoints.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBodyMuted
                        )
                    }
                }
            } else if (traceSteps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { runHandshakeDiagnostics() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Live Handshake Diagnostic", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_handshake_steps"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(traceSteps, key = { it.stepNumber }) { step ->
                        HandshakeStepCard(
                            step = step,
                            onCopyText = { label, text -> copyToClipboard(label, text) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HandshakeStepCard(
    step: HandshakeTraceStep,
    onCopyText: (String, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(step.isFailureOrigin) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isFailureOrigin) RoseErrorBg.copy(alpha = 0.3f) else Color.White
        ),
        border = BorderStroke(
            1.5.dp,
            if (step.isFailureOrigin) RoseError else BorderSlate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_handshake_step_${step.stepNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Step Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Step Number Circle Badge
                    Surface(
                        shape = CircleShape,
                        color = when {
                            step.isFailureOrigin -> RoseError
                            step.isSuccess -> EmeraldSuccess
                            else -> AmberWarning
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${step.stepNumber}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            if (step.isFailureOrigin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RoseError
                                ) {
                                    Text(
                                        text = "401 ORIGIN",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${step.method} ${step.endpointUrl} • ${step.latencyMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // HTTP Status Code Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        step.isSuccess -> EmeraldSuccessBg
                        step.statusCode == 401 -> RoseErrorBg
                        else -> AmberWarningBg
                    }
                ) {
                    Text(
                        text = "HTTP ${step.statusCode}",
                        fontWeight = FontWeight.Bold,
                        color = when {
                            step.isSuccess -> EmeraldSuccess
                            step.statusCode == 401 -> RoseError
                            else -> AmberWarning
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Expand"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Diagnostic Pinpoint Analysis Callout Box
                    if (step.diagnosticPinpoint.isNotBlank()) {
                        Surface(
                            color = if (step.isFailureOrigin) RoseErrorBg else BadgeBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (step.isFailureOrigin) RoseError.copy(alpha = 0.4f) else PrimaryIndigo.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (step.isFailureOrigin) Icons.Default.ErrorOutline else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (step.isFailureOrigin) RoseError else PrimaryIndigo,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (step.isFailureOrigin) "Failure Origin Diagnosis" else "Step Diagnostic Summary",
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.isFailureOrigin) RoseError else PrimaryIndigo,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = step.diagnosticPinpoint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDark,
                                    fontSize = 11.sp
                                )

                                if (step.isFailureOrigin && step.diagnosticPinpoint.contains("SetEnvIf Authorization")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onCopyText("Apache .htaccess Rule", "SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy .htaccess Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Request Headers Section
                    CodeSectionBox(
                        title = "Outbound Request Headers (${step.requestHeaders.size})",
                        map = step.requestHeaders,
                        onCopy = { onCopyText("Request Headers", step.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Response Headers Section
                    CodeSectionBox(
                        title = "Inbound Response Headers (${step.responseHeaders.size})",
                        map = step.responseHeaders,
                        onCopy = { onCopyText("Response Headers", step.responseHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Raw Body Payload Box
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Raw Response Body",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { onCopyText("Raw Response Body", step.responseBodyRaw.orEmpty()) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Body", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatJsonOrRawString(step.responseBodyRaw),
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeSectionBox(
    title: String,
    map: Map<String, String>,
    onCopy: () -> Unit
) {
    Surface(
        color = Slate900,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            map.forEach { (k, v) ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    Text(
                        text = "$k: ",
                        color = Color(0xFFA5B4FC),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = v,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private suspend fun executeLiveHandshakeTrace(
    siteUrl: String,
    username: String,
    appPasswordToken: String
): List<HandshakeTraceStep> = withContext(Dispatchers.IO) {
    val cleanUrl = siteUrl.trim().trimEnd('/').let {
        if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
    }

    val rawCreds = "$username:$appPasswordToken"
    val base64Auth = "Basic " + Base64.encodeToString(rawCreds.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val traceList = mutableListOf<HandshakeTraceStep>()

    // STEP 1: Unauthenticated REST API Discovery Ping
    val step1Req = Request.Builder()
        .url("$cleanUrl/wp-json/")
        .header("User-Agent", "WPMobileHub Handshake Inspector/1.0")
        .get()
        .build()

    val step1Start = System.currentTimeMillis()
    val step1 = try {
        client.newCall(step1Req).execute().use { resp ->
            val elapsed = System.currentTimeMillis() - step1Start
            val body = resp.body?.string() ?: ""
            val isOk = resp.isSuccessful
            HandshakeTraceStep(
                stepNumber = 1,
                title = "REST API Root Discovery",
                endpointUrl = "/wp-json/",
                method = "GET",
                statusCode = resp.code,
                statusMessage = resp.message,
                latencyMs = elapsed,
                isSuccess = isOk,
                isFailureOrigin = !isOk,
                requestHeaders = extractHeadersMap(step1Req.headers),
                responseHeaders = extractHeadersMap(resp.headers),
                responseBodyRaw = body,
                diagnosticPinpoint = if (isOk) "REST Index is public and returning valid JSON schema." else "REST API Root is blocked or offline (HTTP ${resp.code})."
            )
        }
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - step1Start
        HandshakeTraceStep(
            stepNumber = 1,
            title = "REST API Root Discovery",
            endpointUrl = "/wp-json/",
            method = "GET",
            statusCode = 0,
            statusMessage = e.message ?: "Connection Error",
            latencyMs = elapsed,
            isSuccess = false,
            isFailureOrigin = true,
            requestHeaders = extractHeadersMap(step1Req.headers),
            responseHeaders = emptyMap(),
            responseBodyRaw = "Network error: ${e.message}",
            diagnosticPinpoint = "Failed to establish TCP/TLS connection to $cleanUrl."
        )
    }
    traceList.add(step1)

    // STEP 2: Basic Auth Application Password Verification
    val step2Req = Request.Builder()
        .url("$cleanUrl/wp-json/wp/v2/users/me")
        .header("Authorization", base64Auth)
        .header("User-Agent", "WPMobileHub Handshake Inspector/1.0")
        .header("Accept", "application/json")
        .get()
        .build()

    val step2Start = System.currentTimeMillis()
    val step2 = try {
        client.newCall(step2Req).execute().use { resp ->
            val elapsed = System.currentTimeMillis() - step2Start
            val body = resp.body?.string() ?: ""
            val isOk = resp.isSuccessful
            val wwwAuth = resp.header("WWW-Authenticate")
            val isStrippedHeader = resp.code == 401 && wwwAuth.isNullOrBlank()

            val pinpoint = when {
                isOk -> "Basic Auth Application Password verified successfully for user '$username'."
                isStrippedHeader -> "ORIGIN PINPOINT: The Authorization header was sent by the app, but WordPress returned 401 without a WWW-Authenticate header. This proves Apache or Nginx stripped the 'Authorization' header before PHP processed it! Fix: Add 'SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1' to .htaccess."
                resp.code == 401 -> "ORIGIN PINPOINT: WordPress core rejected the Application Password token for user '$username'. Password may be expired or revoked."
                else -> "HTTP ${resp.code} error returned by /wp/v2/users/me."
            }

            HandshakeTraceStep(
                stepNumber = 2,
                title = "Application Password Auth Handshake",
                endpointUrl = "/wp-json/wp/v2/users/me",
                method = "GET",
                statusCode = resp.code,
                statusMessage = resp.message,
                latencyMs = elapsed,
                isSuccess = isOk,
                isFailureOrigin = !isOk && step1.isSuccess,
                requestHeaders = extractHeadersMap(step2Req.headers, maskAuth = true),
                responseHeaders = extractHeadersMap(resp.headers),
                responseBodyRaw = body,
                diagnosticPinpoint = pinpoint
            )
        }
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - step2Start
        HandshakeTraceStep(
            stepNumber = 2,
            title = "Application Password Auth Handshake",
            endpointUrl = "/wp-json/wp/v2/users/me",
            method = "GET",
            statusCode = 0,
            statusMessage = e.message ?: "Connection Error",
            latencyMs = elapsed,
            isSuccess = false,
            isFailureOrigin = step1.isSuccess,
            requestHeaders = extractHeadersMap(step2Req.headers, maskAuth = true),
            responseHeaders = emptyMap(),
            responseBodyRaw = "Network error: ${e.message}",
            diagnosticPinpoint = "Network exception during /users/me verification."
        )
    }
    traceList.add(step2)

    // STEP 3: WooCommerce Orders Endpoint Access Capability
    val step3Req = Request.Builder()
        .url("$cleanUrl/wp-json/wc/v3/orders?per_page=1")
        .header("Authorization", base64Auth)
        .header("User-Agent", "WPMobileHub Handshake Inspector/1.0")
        .get()
        .build()

    val step3Start = System.currentTimeMillis()
    val step3 = try {
        client.newCall(step3Req).execute().use { resp ->
            val elapsed = System.currentTimeMillis() - step3Start
            val body = resp.body?.string() ?: ""
            val isOk = resp.isSuccessful

            val pinpoint = when {
                isOk -> "WooCommerce Orders API granted Read capability."
                resp.code == 401 -> "ORIGIN PINPOINT: WooCommerce REST API rejected credentials. User role '$username' lacks Shop Manager or Admin capability."
                resp.code == 404 -> "WooCommerce plugin is not active or /wc/v3/ route is disabled."
                else -> "WooCommerce API returned HTTP ${resp.code}."
            }

            HandshakeTraceStep(
                stepNumber = 3,
                title = "WooCommerce REST Orders Capability",
                endpointUrl = "/wp-json/wc/v3/orders",
                method = "GET",
                statusCode = resp.code,
                statusMessage = resp.message,
                latencyMs = elapsed,
                isSuccess = isOk,
                isFailureOrigin = !isOk && step2.isSuccess,
                requestHeaders = extractHeadersMap(step3Req.headers, maskAuth = true),
                responseHeaders = extractHeadersMap(resp.headers),
                responseBodyRaw = body,
                diagnosticPinpoint = pinpoint
            )
        }
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - step3Start
        HandshakeTraceStep(
            stepNumber = 3,
            title = "WooCommerce REST Orders Capability",
            endpointUrl = "/wp-json/wc/v3/orders",
            method = "GET",
            statusCode = 0,
            statusMessage = e.message ?: "Connection Error",
            latencyMs = elapsed,
            isSuccess = false,
            isFailureOrigin = false,
            requestHeaders = extractHeadersMap(step3Req.headers, maskAuth = true),
            responseHeaders = emptyMap(),
            responseBodyRaw = "Network error: ${e.message}",
            diagnosticPinpoint = "Connection error during WooCommerce Orders capability check."
        )
    }
    traceList.add(step3)

    // STEP 4: WooCommerce Products Catalog Capability
    val step4Req = Request.Builder()
        .url("$cleanUrl/wp-json/wc/v3/products?per_page=1")
        .header("Authorization", base64Auth)
        .header("User-Agent", "WPMobileHub Handshake Inspector/1.0")
        .get()
        .build()

    val step4Start = System.currentTimeMillis()
    val step4 = try {
        client.newCall(step4Req).execute().use { resp ->
            val elapsed = System.currentTimeMillis() - step4Start
            val body = resp.body?.string() ?: ""
            val isOk = resp.isSuccessful

            HandshakeTraceStep(
                stepNumber = 4,
                title = "WooCommerce Products Catalog Capability",
                endpointUrl = "/wp-json/wc/v3/products",
                method = "GET",
                statusCode = resp.code,
                statusMessage = resp.message,
                latencyMs = elapsed,
                isSuccess = isOk,
                isFailureOrigin = !isOk && step3.isSuccess,
                requestHeaders = extractHeadersMap(step4Req.headers, maskAuth = true),
                responseHeaders = extractHeadersMap(resp.headers),
                responseBodyRaw = body,
                diagnosticPinpoint = if (isOk) "WooCommerce Products API granted Read/Write capability." else "Products API returned HTTP ${resp.code}."
            )
        }
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - step4Start
        HandshakeTraceStep(
            stepNumber = 4,
            title = "WooCommerce Products Catalog Capability",
            endpointUrl = "/wp-json/wc/v3/products",
            method = "GET",
            statusCode = 0,
            statusMessage = e.message ?: "Connection Error",
            latencyMs = elapsed,
            isSuccess = false,
            isFailureOrigin = false,
            requestHeaders = extractHeadersMap(step4Req.headers, maskAuth = true),
            responseHeaders = emptyMap(),
            responseBodyRaw = "Network error: ${e.message}",
            diagnosticPinpoint = "Connection error during Products capability check."
        )
    }
    traceList.add(step4)

    traceList
}

private fun extractHeadersMap(headers: okhttp3.Headers, maskAuth: Boolean = false): Map<String, String> {
    val map = LinkedHashMap<String, String>()
    for (i in 0 until headers.size) {
        val name = headers.name(i)
        val value = headers.value(i)
        if (maskAuth && name.equals("Authorization", ignoreCase = true)) {
            map[name] = "Basic [PROTECTED_CREDENTIALS]"
        } else {
            map[name] = value
        }
    }
    return map
}

private fun formatJsonOrRawString(raw: String?): String {
    if (raw.isNullOrBlank()) return "<Empty Body>"
    return try {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            org.json.JSONObject(trimmed).toString(2)
        } else if (trimmed.startsWith("[")) {
            org.json.JSONArray(trimmed).toString(2)
        } else {
            raw
        }
    } catch (e: Exception) {
        raw
    }
}

private fun buildMarkdownTraceReport(
    site: SiteEntity?,
    steps: List<HandshakeTraceStep>,
    pinpointSummary: String?
): String {
    val sb = StringBuilder()
    sb.appendLine("# WordPress OAuth Application Password Handshake Trace Report")
    sb.appendLine("Site URL: ${site?.url}")
    sb.appendLine("Username: ${site?.username}")
    sb.appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
    sb.appendLine("Summary: ${pinpointSummary ?: "N/A"}\n")

    steps.forEach { step ->
        sb.appendLine("## Step #${step.stepNumber}: ${step.title}")
        sb.appendLine("- Endpoint: ${step.method} ${step.endpointUrl}")
        sb.appendLine("- Response Code: HTTP ${step.statusCode} ${step.statusMessage}")
        sb.appendLine("- Latency: ${step.latencyMs} ms")
        sb.appendLine("- Failure Origin: ${if (step.isFailureOrigin) "YES (401 Originated Here)" else "No"}")
        sb.appendLine("- Diagnostic Analysis: ${step.diagnosticPinpoint}\n")

        sb.appendLine("### Outbound Request Headers:")
        sb.appendLine("```http")
        step.requestHeaders.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("```\n")

        sb.appendLine("### Inbound Response Headers:")
        sb.appendLine("```http")
        step.responseHeaders.forEach { (k, v) -> sb.appendLine("$k: $v") }
        sb.appendLine("```\n")

        sb.appendLine("### Raw Response Body:")
        sb.appendLine("```json")
        sb.appendLine(step.responseBodyRaw ?: "<Empty>")
        sb.appendLine("```\n")
        sb.appendLine("--------------------------------------------------\n")
    }

    return sb.toString()
}
