package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.WordPressApiLogEntry
import com.example.data.remote.WordPressLogStore
import com.example.ui.theme.*

@Composable
fun WordPressApiInspectorOverlay(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {}
) {
    val logs by WordPressLogStore.logs.collectAsStateWithLifecycle()
    var isInspectorOpen by remember { mutableStateOf(false) }

    // Pulse animation for floating button badge when new logs arrive
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Floating action bubble to open the Network Inspector Overlay
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 80.dp)
                    .shadow(12.dp, CircleShape)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(WordPressNavy)
                    .clickable { isInspectorOpen = true }
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .testTag("floating_api_inspector_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "REST API Logger",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                // Log count badge
                if (logs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(RoseError.copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = logs.size.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Expanded inspector dialog
        if (isInspectorOpen) {
            ApiLogsDialog(
                logs = logs,
                onDismiss = { isInspectorOpen = false },
                onShowMessage = onShowMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogsDialog(
    logs: List<WordPressApiLogEntry>,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: 2xx OK, 2: 4xx Auth, 3: Errors
    val clipboardManager = LocalClipboardManager.current

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            1 -> logs.filter { it.statusCode in 200..299 }
            2 -> logs.filter { it.statusCode in 400..499 }
            3 -> logs.filter { it.statusCode >= 500 || it.statusCode == 0 }
            else -> logs
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SiteDeck Live Network Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Real-time WordPress & WooCommerce API Inspection",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                WordPressLogStore.clearLogs()
                                onShowMessage("API logs cleared successfully")
                            },
                            enabled = logs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("All (${logs.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("2xx OK (${logs.count { it.statusCode in 200..299 }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("4xx Auth (${logs.count { it.statusCode in 400..499 }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == 3,
                        onClick = { selectedFilter = 3 },
                        label = { Text("Errors (${logs.count { it.statusCode >= 500 || it.statusCode == 0 }})", fontSize = 11.sp) }
                    )
                }

                // Log List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (logs.isEmpty()) "No outgoing requests captured yet" else "No logs match active filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Interact with store, posts or plugins to trigger calls.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            ApiLogOverlayCard(
                                log = log,
                                onCopy = { text, label ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    onShowMessage("Copied $label to clipboard")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiLogOverlayCard(
    log: WordPressApiLogEntry,
    onCopy: (String, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (log.statusCode) {
        200, 201 -> EmeraldSuccess
        401 -> RoseError
        403 -> AmberWarning
        404 -> IndigoAccent
        else -> RoseError
    }

    val statusBg = when (log.statusCode) {
        200, 201 -> EmeraldSuccessBg
        401 -> RoseErrorBg
        403 -> AmberWarningBg
        404 -> IndigoInfoBg
        else -> RoseErrorBg
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Summary Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Method Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (log.method) {
                        "GET" -> MaterialTheme.colorScheme.primaryContainer
                        "POST" -> EmeraldSuccessBg
                        "DELETE" -> RoseErrorBg
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = log.method,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (log.method) {
                            "GET" -> MaterialTheme.colorScheme.primary
                            "POST" -> EmeraldSuccess
                            "DELETE" -> RoseError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Short URL / Endpoint
                val endpoint = log.url.substringAfter("/wp-json/")
                Text(
                    text = "/wp-json/$endpoint",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // HTTP Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (log.statusCode > 0) log.statusCode.toString() else "ERR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp and duration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.timeFormatted,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${log.durationMs}ms",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded view containing full parameters, headers, payload, response and fixes!
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Request section
                    Text(
                        text = "REQUEST DETAILS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "URL: ${log.url}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (log.requestHeaders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Headers: ${log.requestHeaders}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!log.requestBody.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Body: ${log.requestBody}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Response section
                    Text(
                        text = "RESPONSE PAYLOAD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                        modifier = Modifier.fillMaxWidth().border(1.dp, Slate700, RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (log.statusCode > 0) "RESPONSE BODY (${log.statusCode})" else "CONNECTION FAILURE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = Slate400,
                                    fontWeight = FontWeight.Bold
                                )

                                TextButton(
                                    onClick = { onCopy(log.responseBodyPreview ?: log.errorMessage ?: "", "Response Body") },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, tint = EmeraldSuccess, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = log.responseBodyPreview ?: log.errorMessage ?: "No response body returned.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Slate200,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Diagnostic advice / Fix
                    if (!log.diagnosticAdvice.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWarningBg.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TROUBLESHOOT ADVICE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning,
                                        fontSize = 8.sp
                                    )
                                    Text(
                                        text = log.diagnosticAdvice,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
