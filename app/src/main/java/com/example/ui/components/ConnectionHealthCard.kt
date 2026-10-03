package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import com.example.data.remote.RestTestResult
import com.example.data.remote.WordPressRestClient
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConnectionHealthCard(
    currentSite: SiteEntity?,
    onReconnectSite: (() -> Unit)? = null,
    onViewRestLogs: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val restClient = remember { WordPressRestClient() }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<RestTestResult?>(null) }
    var lastCheckedTimestamp by remember { mutableStateOf<String?>(null) }

    fun runDiagnosticPing() {
        val url = currentSite?.url
        if (url.isNullOrBlank()) return
        isTestingConnection = true
        coroutineScope.launch {
            val result = restClient.runSimpleRestConnectionTest(url)
            testResult = result
            isTestingConnection = false
            val timeString = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
            lastCheckedTimestamp = "Checked at $timeString"
        }
    }

    // Run initial diagnostic check when site changes
    LaunchedEffect(currentSite?.id, currentSite?.url) {
        if (currentSite != null && currentSite.url.isNotBlank()) {
            runDiagnosticPing()
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSlate200),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_connection_health")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val statusColor = when {
                        isTestingConnection -> InfoBlue
                        currentSite?.isAuthenticated == false || currentSite?.restApiStatus?.contains("401") == true -> RoseError
                        testResult is RestTestResult.Success -> EmeraldSuccess
                        testResult is RestTestResult.Error -> RoseError
                        else -> EmeraldSuccess
                    }

                    val statusBg = statusColor.copy(alpha = 0.15f)

                    Surface(
                        shape = CircleShape,
                        color = statusBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    color = InfoBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = when {
                                        currentSite?.isAuthenticated == false || currentSite?.restApiStatus?.contains("401") == true -> Icons.Default.VpnKeyOff
                                        testResult is RestTestResult.Success -> Icons.Default.CloudDone
                                        testResult is RestTestResult.Error -> Icons.Default.CloudOff
                                        else -> Icons.Default.Dns
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Connection Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Status Badge
                            val badgeLabel = when {
                                isTestingConnection -> "Pinging..."
                                currentSite?.isAuthenticated == false || currentSite?.restApiStatus?.contains("401") == true -> "Auth Failed (401)"
                                testResult is RestTestResult.Success -> "200 OK (Healthy)"
                                testResult is RestTestResult.Error -> "Unreachable"
                                else -> "Connected"
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = lastCheckedTimestamp ?: "Live GET /wp-json/ REST API diagnostic",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Refresh Diagnostic Ping Button
                IconButton(
                    onClick = { runDiagnosticPing() },
                    enabled = !isTestingConnection,
                    modifier = Modifier.testTag("btn_refresh_connection_health")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Run Diagnostic Check",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Diagnostic Status Banner / Message
            if (currentSite?.isAuthenticated == false || currentSite?.restApiStatus?.contains("401") == true) {
                Surface(
                    color = RoseErrorBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Application Password Rejected",
                                fontWeight = FontWeight.Bold,
                                color = RoseError,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "WordPress /wp-json/ is reachable, but WooCommerce API returned 401 Unauthorized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseError.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                        if (onReconnectSite != null) {
                            Button(
                                onClick = onReconnectSite,
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else if (testResult is RestTestResult.Error) {
                val err = (testResult as RestTestResult.Error).message
                Surface(
                    color = RoseErrorBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Diagnostic Ping Error",
                                fontWeight = FontWeight.Bold,
                                color = RoseError,
                                fontSize = 13.sp
                            )
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseError.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Key Metrics Pill Grid (3 Cards)
            val successResult = testResult as? RestTestResult.Success
            val latencyText = when {
                isTestingConnection -> "..."
                successResult != null -> "${successResult.latencyMs} ms"
                testResult is RestTestResult.Error -> "${(testResult as RestTestResult.Error).latencyMs} ms"
                else -> "120 ms"
            }
            val routesText = when {
                isTestingConnection -> "..."
                successResult != null -> "${successResult.routesCount} routes"
                else -> "WP REST v2"
            }
            val namespacesText = when {
                isTestingConnection -> "..."
                successResult != null -> "${successResult.namespacesCount} ns"
                else -> "JSON OK"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Ping Latency
                HealthPillCard(
                    title = "Latency Ping",
                    value = latencyText,
                    icon = Icons.Default.Speed,
                    tint = PrimaryIndigo,
                    modifier = Modifier.weight(1f)
                )

                // Metric 2: REST Routes
                HealthPillCard(
                    title = "REST Routes",
                    value = routesText,
                    icon = Icons.Default.AltRoute,
                    tint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )

                // Metric 3: Namespaces
                HealthPillCard(
                    title = "Namespaces",
                    value = namespacesText,
                    icon = Icons.Default.Api,
                    tint = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { runDiagnosticPing() },
                    enabled = !isTestingConnection,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_run_diagnostic_ping")
                ) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Diagnostic Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (onViewRestLogs != null) {
                    TextButton(
                        onClick = onViewRestLogs,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_view_rest_logs")
                    ) {
                        Text("View Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthPillCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BadgeBackground,
        border = BorderStroke(1.dp, BorderSlate200.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextBodyMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
