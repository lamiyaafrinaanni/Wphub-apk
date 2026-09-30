package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.util.WebViewCrashLog
import com.example.util.WebViewCrashLogger
import com.example.util.WebViewEventType

/**
 * Bottom sheet modal displaying live WebView renderer process crash logs,
 * -1 error code tracking, site configurations, and root cause analysis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewCrashLogSheet(
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val crashLogs by WebViewCrashLogger.logs.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Renderer Crashes, 2: Code -1, 3: HTTP Errors
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val filteredLogs = remember(crashLogs, selectedFilter, searchQuery) {
        crashLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                1 -> log.eventType == WebViewEventType.RENDERER_CRASH || log.eventType == WebViewEventType.RENDERER_KILLED
                2 -> log.errorCode == -1 || log.eventType == WebViewEventType.ERROR_UNKNOWN_MINUS_ONE
                3 -> log.eventType == WebViewEventType.HTTP_ERROR
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.url.contains(searchQuery, ignoreCase = true) ||
                log.host.contains(searchQuery, ignoreCase = true) ||
                log.errorDescription.contains(searchQuery, ignoreCase = true) ||
                log.possibleRootCause.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("webview_crash_log_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoseErrorBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WebView Renderer Crash & -1 Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track Chromium crashes, site configs & code -1 causes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CounterCard(
                    title = "Total Events",
                    count = crashLogs.size.toString(),
                    color = PrimaryIndigo,
                    modifier = Modifier.weight(1f)
                )
                CounterCard(
                    title = "Renderer Crashes",
                    count = crashLogs.count { it.eventType == WebViewEventType.RENDERER_CRASH || it.eventType == WebViewEventType.RENDERER_KILLED }.toString(),
                    color = RoseError,
                    modifier = Modifier.weight(1f)
                )
                CounterCard(
                    title = "-1 Unknown Errors",
                    count = crashLogs.count { it.errorCode == -1 }.toString(),
                    color = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter by site URL or host...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                FilledTonalButton(
                    onClick = {
                        WebViewCrashLogger.simulateTestCrashLog()
                        onShowMessage("Added simulated WebView -1 test log")
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Log", fontSize = 11.sp)
                }

                if (crashLogs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            WebViewCrashLogger.clearLogs()
                            onShowMessage("Cleared all WebView crash logs")
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("All (${crashLogs.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("Crashes (${crashLogs.count { it.eventType == WebViewEventType.RENDERER_CRASH || it.eventType == WebViewEventType.RENDERER_KILLED }})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Code -1 (${crashLogs.count { it.errorCode == -1 }})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == 3,
                    onClick = { selectedFilter = 3 },
                    label = { Text("HTTP (${crashLogs.count { it.eventType == WebViewEventType.HTTP_ERROR }})", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (crashLogs.isEmpty()) "No WebView renderer crashes recorded" else "No logs match your filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When WordPress web login or Gutenberg blocks encounter Chromium renderer crashes or -1 errors, details will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                WebViewCrashLogger.simulateTestCrashLog()
                                onShowMessage("Generated test crash entry")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Sample -1 Crash Log")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        WebViewCrashLogCard(
                            log = log,
                            onCopy = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                onShowMessage("Copied log details to clipboard")
                            }
                        )
                    }
                }

                // Copy Diagnostic Report Footer
                Button(
                    onClick = {
                        val report = WebViewCrashLogger.exportFormattedReport()
                        clipboardManager.setText(AnnotatedString(report))
                        onShowMessage("Copied full WebView diagnostic report!")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Full WebView Diagnostic Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CounterCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WebViewCrashLogCard(
    log: WebViewCrashLog,
    onCopy: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(log.errorCode == -1 || log.eventType == WebViewEventType.RENDERER_CRASH) }

    val statusColor = when (log.eventType) {
        WebViewEventType.RENDERER_CRASH, WebViewEventType.RENDERER_KILLED -> RoseError
        WebViewEventType.ERROR_UNKNOWN_MINUS_ONE -> AmberWarning
        WebViewEventType.HTTP_ERROR -> if (log.errorCode >= 500) RoseError else AmberWarning
        else -> MaterialTheme.colorScheme.primary
    }

    val statusBg = when (log.eventType) {
        WebViewEventType.RENDERER_CRASH, WebViewEventType.RENDERER_KILLED -> RoseErrorBg
        WebViewEventType.ERROR_UNKNOWN_MINUS_ONE -> AmberWarningBg
        WebViewEventType.HTTP_ERROR -> if (log.errorCode >= 500) RoseErrorBg else AmberWarningBg
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                        Text(
                            text = "Code ${log.errorCode} • ${log.eventType.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.host,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) 3 else 1
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Description Box
                    Text(
                        text = "Error Description:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = log.errorDescription,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (log.didCrash != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "didCrash: ${log.didCrash}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (log.didCrash) RoseError else EmeraldSuccess
                            )
                            if (log.rendererPriority != null) {
                                Text(
                                    text = "Renderer Priority: ${log.rendererPriority}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Root Cause Analysis Box
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Root Cause Analysis", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.possibleRootCause, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recommended Fix", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AmberWarning)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.recommendedAction, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                        }
                    }

                    // Site Configuration Box
                    if (log.siteConfiguration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Site & WebView Configuration:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate900,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.siteConfiguration,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Slate100,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Copy Card Log Button
                    OutlinedButton(
                        onClick = {
                            val cardText = buildString {
                                appendLine("WebView Log - Event Code: ${log.errorCode} (${log.eventType.name})")
                                appendLine("Timestamp: ${log.formattedTime}")
                                appendLine("URL: ${log.url}")
                                appendLine("Description: ${log.errorDescription}")
                                if (log.didCrash != null) appendLine("didCrash: ${log.didCrash}")
                                appendLine("Config: ${log.siteConfiguration}")
                                appendLine("Root Cause: ${log.possibleRootCause}")
                                appendLine("Recommended Fix: ${log.recommendedAction}")
                            }
                            onCopy(cardText)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Event Log", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
