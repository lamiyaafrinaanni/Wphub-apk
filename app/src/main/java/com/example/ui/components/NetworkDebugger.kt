package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.remote.*
import com.example.ui.screens.RestApiLogCard
import com.example.ui.theme.*
import com.example.util.WebViewCrashLog
import com.example.util.WebViewCrashLogger
import com.example.util.WebViewEventType
import kotlinx.coroutines.launch

/**
 * High-performance Network Debugger & WordPress Permissions Scope Diagnostic Tool.
 * Provides live OkHttp REST request/response inspection and endpoint permission testing
 * for 'wp/v2/posts', 'wc/v3/orders', 'wp/v2/users/me', etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDebuggerSheet(
    initialUrl: String = "",
    initialUsername: String = "",
    initialTokenOrPass: String = "",
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Logs, 1: Scope Diagnostics, 2: Fix Recipes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Network Debugger & Scope Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyze OkHttp traffic & WordPress REST scopes",
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

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Logs", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Http, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Scope Scan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("WebView Crashes & -1", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Fix Recipes", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Contents
            when (selectedTab) {
                0 -> LiveNetworkLogsView(onShowMessage = onShowMessage)
                1 -> PermissionsScopeDiagnosticView(
                    initialUrl = initialUrl,
                    initialUsername = initialUsername,
                    initialTokenOrPass = initialTokenOrPass,
                    onShowMessage = onShowMessage
                )
                2 -> EmbeddedWebViewCrashLogsView(onShowMessage = onShowMessage)
                3 -> AuthTroubleshootingRecipesView(onShowMessage = onShowMessage)
            }
        }
    }
}

@Composable
private fun LiveNetworkLogsView(
    onShowMessage: (String) -> Unit
) {
    val apiLogs by WordPressLogStore.logs.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: 2xx Success, 2: 401/403 Auth, 3: 404/5xx
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val filteredLogs = remember(apiLogs, selectedFilter, searchQuery) {
        apiLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                1 -> log.statusCode in 200..299
                2 -> log.statusCode in 401..403
                3 -> log.statusCode == 404 || log.statusCode >= 500 || log.statusCode == 0
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.url.contains(searchQuery, ignoreCase = true) ||
                log.method.contains(searchQuery, ignoreCase = true) ||
                log.statusMessage.contains(searchQuery, ignoreCase = true) ||
                (log.errorMessage?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Clear Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter by URL or endpoint...", fontSize = 12.sp) },
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

            if (apiLogs.isNotEmpty()) {
                FilledTonalButton(
                    onClick = {
                        WordPressLogStore.clearLogs()
                        onShowMessage("API Logs Cleared")
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 12.sp)
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
                label = { Text("All (${apiLogs.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == 1,
                onClick = { selectedFilter = 1 },
                label = { Text("2xx OK (${apiLogs.count { it.statusCode in 200..299 }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == 2,
                onClick = { selectedFilter = 2 },
                label = { Text("401/403 Scope (${apiLogs.count { it.statusCode in 401..403 }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == 3,
                onClick = { selectedFilter = 3 },
                label = { Text("Errors (${apiLogs.count { it.statusCode == 404 || it.statusCode >= 500 || it.statusCode == 0 }})", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (apiLogs.isEmpty()) "No OkHttp requests recorded yet" else "No logs match your filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trigger a sync on Dashboard or run the Scope Diagnostic test.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    RestApiLogCard(
                        log = log,
                        onCopy = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            onShowMessage("Copied log details to clipboard")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionsScopeDiagnosticView(
    initialUrl: String,
    initialUsername: String,
    initialTokenOrPass: String,
    onShowMessage: (String) -> Unit
) {
    var siteUrl by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var tokenOrPass by remember { mutableStateOf(initialTokenOrPass) }
    var isRunning by remember { mutableStateOf(false) }
    var diagnosticReport by remember { mutableStateOf<WordPressScopeDiagnosticReport?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val restClient = remember { WordPressRestClient() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Endpoint Scope & Permission Analyzer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verifies whether your credentials have sufficient scope to read & write 'wp/v2/posts' and 'wc/v3/orders'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = siteUrl,
                    onValueChange = { siteUrl = it },
                    label = { Text("WordPress Site URL") },
                    placeholder = { Text("https://your-wordpress-site.com") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("scope_input_url")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("WordPress Username") },
                    placeholder = { Text("admin") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("scope_input_user")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tokenOrPass,
                    onValueChange = { tokenOrPass = it },
                    label = { Text("Application Password or JWT Token") },
                    placeholder = { Text("xxxx xxxx xxxx xxxx or eyJ...") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("scope_input_pass")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (siteUrl.isBlank()) {
                            onShowMessage("Please enter a WordPress site URL")
                            return@Button
                        }
                        isRunning = true
                        diagnosticReport = null
                        coroutineScope.launch {
                            try {
                                val report = restClient.testPermissionsAndScopes(
                                    siteUrl = siteUrl,
                                    username = username,
                                    tokenOrPass = tokenOrPass
                                )
                                diagnosticReport = report
                                onShowMessage("Diagnostic scan completed: ${report.overallHealth}")
                            } catch (e: Exception) {
                                onShowMessage("Diagnostic error: ${e.message}")
                            } finally {
                                isRunning = false
                            }
                        }
                    },
                    enabled = !isRunning && siteUrl.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_run_scope_scan")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing REST Endpoints & Scopes...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Permissions & Scope Test", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        diagnosticReport?.let { report ->
            // Overall Verdict Card
            val isHealthy = report.canAccessPosts && report.canAccessWooOrders
            val healthColor = if (isHealthy) EmeraldSuccess else if (report.canAccessPosts) AmberWarning else RoseError
            val healthBg = if (isHealthy) EmeraldSuccessBg else if (report.canAccessPosts) AmberWarningBg else RoseErrorBg

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = healthBg,
                border = BorderStroke(1.5.dp, healthColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = healthColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = report.overallHealth,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = healthColor
                            )
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.8f)) {
                            Text(
                                text = report.authType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!report.userDisplayName.isNullOrBlank()) {
                        Text(
                            text = "Authenticated User: ${report.userDisplayName} (Role: ${report.userRole ?: "Unknown"})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Scopes Pill Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScopeBadge(
                            label = "Posts (wp/v2/posts)",
                            isGranted = report.canAccessPosts,
                            modifier = Modifier.weight(1f)
                        )
                        ScopeBadge(
                            label = "Orders (wc/v3/orders)",
                            isGranted = report.canAccessWooOrders,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actionable Guidance if errors found
            if (report.actionableGuidance.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recommended Fixes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        report.actionableGuidance.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(tip, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Detailed Endpoint breakdown
            Text(
                text = "Tested REST Endpoints Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            report.endpointResults.forEach { result ->
                EndpointScopeResultCard(result = result)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copy Full Report Button
            OutlinedButton(
                onClick = {
                    val reportText = buildString {
                        appendLine("=== WordPress REST API Permissions & Scope Diagnostic Report ===")
                        appendLine("Site URL: ${report.siteUrl}")
                        appendLine("Auth Type: ${report.authType}")
                        appendLine("User: ${report.username} (${report.userDisplayName})")
                        appendLine("Role: ${report.userRole}")
                        appendLine("Overall Health: ${report.overallHealth}")
                        appendLine("Can Access Posts: ${report.canAccessPosts}")
                        appendLine("Can Access WooCommerce Orders: ${report.canAccessWooOrders}")
                        appendLine("\n--- Endpoint Results ---")
                        report.endpointResults.forEach { res ->
                            appendLine("[${res.statusCode} ${res.statusText}] ${res.route} -> ${res.scopeStatus.name}")
                            appendLine("  Summary: ${res.summary}")
                            appendLine("  Explanation: ${res.diagnosticExplanation}")
                            if (res.recommendedFix != null) appendLine("  Recommended Fix: ${res.recommendedFix}")
                        }
                    }
                    clipboardManager.setText(AnnotatedString(reportText))
                    onShowMessage("Copied diagnostic scope report to clipboard")
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Full Diagnostic Scope Report")
            }
        }
    }
}

@Composable
private fun ScopeBadge(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isGranted) EmeraldSuccessBg else RoseErrorBg,
        border = BorderStroke(1.dp, if (isGranted) EmeraldSuccess else RoseError),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isGranted) EmeraldSuccess else RoseError,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EndpointScopeResultCard(result: EndpointScopeCheckResult) {
    var expanded by remember { mutableStateOf(result.scopeStatus != ScopeStatus.GRANTED) }

    val statusColor = when (result.scopeStatus) {
        ScopeStatus.GRANTED -> EmeraldSuccess
        ScopeStatus.READ_ONLY, ScopeStatus.FORBIDDEN -> AmberWarning
        else -> RoseError
    }

    val statusBg = when (result.scopeStatus) {
        ScopeStatus.GRANTED -> EmeraldSuccessBg
        ScopeStatus.READ_ONLY, ScopeStatus.FORBIDDEN -> AmberWarningBg
        else -> RoseErrorBg
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = "${result.statusCode} ${result.scopeStatus.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.endpointName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.route,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.diagnosticExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    if (!result.recommendedFix.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWarningBg.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fix: ${result.recommendedFix}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (!result.responseBodySnippet.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Response Snippet:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.responseBodySnippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthTroubleshootingRecipesView(
    onShowMessage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Common WordPress REST Authentication Fixes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Why data loads as 0 / empty and how to configure your WordPress host",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Recipe 1: Apache Authorization Header Stripping
        RecipeCard(
            title = "1. Apache Stripping 'Authorization' Header (401 Error)",
            description = "Many shared hosting providers (cPanel, Hostinger, Siteground) strip the HTTP Authorization header before PHP receives it.",
            codeSnippet = "# Add this to the top of your WordPress .htaccess file:\n<IfModule mod_rewrite.c>\nRewriteEngine On\nRewriteRule .* - [E=HTTP_AUTHORIZATION:%{HTTP:Authorization}]\nSetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1\n</IfModule>",
            onCopy = {
                clipboardManager.setText(AnnotatedString(it))
                onShowMessage("Copied .htaccess snippet to clipboard")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recipe 2: Application Passwords Creation
        RecipeCard(
            title = "2. How to Generate an Application Password",
            description = "Application Passwords allow mobile apps to authenticate safely without exposing your main admin password.",
            codeSnippet = "1. Log into your WordPress Admin Dashboard.\n2. Go to Users > Profile (or Edit User).\n3. Scroll down to 'Application Passwords'.\n4. Type 'WPMobileHub' as the New Application Password Name.\n5. Click 'Add New Application Password'.\n6. Copy the 24-character generated password.",
            onCopy = {
                clipboardManager.setText(AnnotatedString(it))
                onShowMessage("Copied instructions to clipboard")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recipe 3: Pretty Permalinks Requirement
        RecipeCard(
            title = "3. Enable Pretty Permalinks (404 Error on /wp-json/)",
            description = "WordPress REST API endpoints require Pretty Permalinks to be active. Default Plain URLs (?p=123) cause 404 Not Found.",
            codeSnippet = "1. In WordPress Admin, navigate to Settings > Permalinks.\n2. Under Common Settings, select 'Post name' (%postname%).\n3. Click 'Save Changes' at the bottom.",
            onCopy = {
                clipboardManager.setText(AnnotatedString(it))
                onShowMessage("Copied Permalinks guide to clipboard")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Recipe 4: WooCommerce REST API Keys
        RecipeCard(
            title = "4. WooCommerce Orders & Products Access (403 Error)",
            description = "To access /wc/v3/orders and /wc/v3/products, the connecting user must have the Administrator or Shop Manager role.",
            codeSnippet = "1. Navigate to WooCommerce > Settings > Advanced > REST API.\n2. Click 'Add Key'.\n3. Set Description to 'WPMobileHub App'.\n4. Set Permissions to 'Read/Write'.\n5. Select an Administrator user.\n6. Click 'Generate API Key'.",
            onCopy = {
                clipboardManager.setText(AnnotatedString(it))
                onShowMessage("Copied WooCommerce API Key guide to clipboard")
            }
        )
    }
}

@Composable
private fun RecipeCard(
    title: String,
    description: String,
    codeSnippet: String,
    onCopy: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = codeSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Slate100,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onCopy(codeSnippet) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedWebViewCrashLogsView(
    onShowMessage: (String) -> Unit
) {
    val crashLogs by WebViewCrashLogger.logs.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableIntStateOf(0) }
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
                log.errorDescription.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            )

            FilledTonalButton(
                onClick = {
                    WebViewCrashLogger.simulateTestCrashLog()
                    onShowMessage("Added test WebView -1 crash log")
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Log", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No WebView renderer crashes recorded yet", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap 'Test Log' above to generate a sample entry.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = RoseErrorBg) {
                                    Text(
                                        text = "Code ${log.errorCode} • ${log.eventType.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoseError,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(log.formattedTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(log.url, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.errorDescription, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Root Cause Analysis:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Text(log.possibleRootCause, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Fix Recommendation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AmberWarning)
                                    Text(log.recommendedAction, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

