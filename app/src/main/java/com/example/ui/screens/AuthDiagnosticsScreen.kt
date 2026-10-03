package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SiteEntity
import com.example.data.remote.WordPress401LogEntry
import com.example.data.remote.WordPress401LogStore
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDiagnosticsScreen(
    currentSite: SiteEntity?,
    onBackClick: (() -> Unit)? = null,
    onReconnectClick: (() -> Unit)? = null,
    onOpenHandshakeTrace: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawLogs by WordPress401LogStore.logs.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All 401 Logs, 1 = Server .htaccess Fixes

    val filteredLogs = remember(rawLogs, searchQuery) {
        if (searchQuery.isBlank()) rawLogs
        else {
            rawLogs.filter { log ->
                log.url.contains(searchQuery, ignoreCase = true) ||
                        log.method.contains(searchQuery, ignoreCase = true) ||
                        log.probableCause.contains(searchQuery, ignoreCase = true) ||
                        log.recommendedFix.contains(searchQuery, ignoreCase = true) ||
                        log.responseBodyRaw.orEmpty().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "401 Raw Auth Logs & Diagnostics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = currentSite?.name ?: "WordPress REST API Debugger",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted
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
                    if (onOpenHandshakeTrace != null) {
                        IconButton(onClick = onOpenHandshakeTrace) {
                            Icon(Icons.Default.Route, contentDescription = "Trace OAuth Handshake", tint = PrimaryIndigo)
                        }
                    }
                    if (rawLogs.isNotEmpty()) {
                        IconButton(onClick = {
                            val report = WordPress401LogStore.buildFullDiagnosticReport()
                            copyToClipboard("401 Diagnostic Report", report)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Full Report", tint = PrimaryIndigo)
                        }
                        IconButton(onClick = { WordPress401LogStore.clearLogs() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = RoseError)
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
            // Summary Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rawLogs.isEmpty()) EmeraldSuccessBg.copy(alpha = 0.5f) else RoseErrorBg.copy(alpha = 0.6f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (rawLogs.isEmpty()) EmeraldSuccess.copy(alpha = 0.3f) else RoseError.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("card_401_summary_header")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (rawLogs.isEmpty()) EmeraldSuccess else RoseError,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (rawLogs.isEmpty()) Icons.Default.CheckCircle else Icons.Default.VpnKeyOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (rawLogs.isEmpty()) "No 401 Auth Failures Recorded" else "${rawLogs.size} HTTP 401 Failures Captured",
                            fontWeight = FontWeight.Bold,
                            color = if (rawLogs.isEmpty()) EmeraldSuccess else RoseError,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (rawLogs.isEmpty())
                                "All authenticated REST requests are completing successfully without rejection."
                            else
                                "Raw headers and payload traces captured by LoggingAuthenticator for troubleshooting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (onReconnectClick != null && rawLogs.isNotEmpty()) {
                        Button(
                            onClick = onReconnectClick,
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_reconnect_from_401_logs")
                        ) {
                            Text("Reconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar & Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter logs by URL, header, or error...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_search_401_logs")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Log List / Empty View
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = TextBodyMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No logs match '$searchQuery'" else "No 401 Unauthorized logs recorded yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When WordPress returns an HTTP 401 response code, exact request/response headers will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBodyMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_401_logs"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { logEntry ->
                        Raw401LogEntryCard(
                            entry = logEntry,
                            onCopyReport = { copyToClipboard("401 Log Entry", it) },
                            onCopyHeaders = { label, headers ->
                                val text = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                                copyToClipboard(label, text)
                            },
                            onCopyBody = { copyToClipboard("Raw Body", it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Raw401LogEntryCard(
    entry: WordPress401LogEntry,
    onCopyReport: (String) -> Unit,
    onCopyHeaders: (String, Map<String, String>) -> Unit,
    onCopyBody: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_401_log_entry_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header Row
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
                    // Method Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RoseError
                    ) {
                        Text(
                            text = entry.method,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.url.removePrefix("https://").removePrefix("http://"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.timeFormatted} • HTTP 401 ${entry.responseMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseError,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle expand"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Diagnostic Analysis & Fix Recommendation Box
                    Surface(
                        color = RoseErrorBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Probable Cause & Recommendation",
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.probableCause,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Fix: ${entry.recommendedFix}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextBodyMuted,
                                fontSize = 11.sp
                            )

                            if (entry.recommendedFix.contains("SetEnvIf Authorization")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onCopyBody("SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy .htaccess Rule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Request Headers Section
                    CodeBlockSection(
                        title = "Exact Request Headers (${entry.requestHeaders.size})",
                        headers = entry.requestHeaders,
                        onCopy = { onCopyHeaders("Request Headers", entry.requestHeaders) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Response Headers Section
                    CodeBlockSection(
                        title = "Exact Response Headers (${entry.responseHeaders.size})",
                        headers = entry.responseHeaders,
                        onCopy = { onCopyHeaders("Response Headers", entry.responseHeaders) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raw Response Body Section
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
                                    text = "Raw Response Payload",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { onCopyBody(entry.responseBodyRaw ?: "") },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Body", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatJsonOrRaw(entry.responseBodyRaw),
                                color = Color(0xFF38BDF8), // Light Cyan Code Accent
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
private fun CodeBlockSection(
    title: String,
    headers: Map<String, String>,
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
                    Text("Copy Headers", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            headers.forEach { (key, value) ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    Text(
                        text = "$key: ",
                        color = Color(0xFFA5B4FC), // Indigo Code Accent
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = value,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatJsonOrRaw(raw: String?): String {
    if (raw.isNullOrBlank()) return "<Empty Response Body>"
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
