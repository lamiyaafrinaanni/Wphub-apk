package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.data.remote.EndpointScopeCheckResult
import com.example.data.remote.ScopeStatus
import com.example.data.remote.WordPressScopeDiagnosticReport
import com.example.ui.theme.*

@Composable
fun WordPressApiDiagnosticView(
    report: WordPressScopeDiagnosticReport,
    modifier: Modifier = Modifier,
    onCopyMessage: (String) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Diagnostic Overview Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    report.canAccessPosts && report.canAccessWooOrders -> EmeraldSuccessBg
                    report.canAccessPosts -> IndigoInfoBg
                    else -> RoseErrorBg
                }
            ),
            border = BorderStroke(
                1.dp,
                when {
                    report.canAccessPosts && report.canAccessWooOrders -> EmeraldSuccess.copy(alpha = 0.5f)
                    report.canAccessPosts -> IndigoInfo.copy(alpha = 0.5f)
                    else -> RoseError.copy(alpha = 0.5f)
                }
            ),
            modifier = Modifier.fillMaxWidth().testTag("card_wp_diagnostic_overview")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    report.canAccessPosts && report.canAccessWooOrders -> EmeraldSuccess
                                    report.canAccessPosts -> IndigoInfo
                                    else -> RoseError
                                }
                            )
                    ) {
                        Icon(
                            imageVector = when {
                                report.canAccessPosts && report.canAccessWooOrders -> Icons.Default.CheckCircle
                                report.canAccessPosts -> Icons.Default.Info
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WordPress REST API Diagnostic Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = report.overallHealth,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (report.userDisplayName != null || report.userRole != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Authenticated User: ${report.userDisplayName ?: report.username}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Role: ${report.userRole ?: "Unknown"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Actionable Guidance List
        if (report.actionableGuidance.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AmberWarningBg.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_wp_diagnostic_guidance")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended Fixes & Setup Steps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    report.actionableGuidance.forEach { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Endpoint Results List
        Text(
            text = "Tested REST API Endpoints (${report.endpointResults.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        report.endpointResults.forEach { result ->
            EndpointDiagnosticCard(
                result = result,
                onCopy = { text, label ->
                    clipboardManager.setText(AnnotatedString(text))
                    onCopyMessage("Copied $label to clipboard")
                }
            )
        }
    }
}

@Composable
fun EndpointDiagnosticCard(
    result: EndpointScopeCheckResult,
    onCopy: (text: String, label: String) -> Unit
) {
    var expandedJson by remember { mutableStateOf(false) }

    val statusBgColor = when (result.statusCode) {
        200, 201 -> EmeraldSuccessBg
        401 -> RoseErrorBg
        403 -> AmberWarningBg
        404 -> IndigoInfoBg
        else -> Slate800
    }

    val statusBadgeColor = when (result.statusCode) {
        200, 201 -> EmeraldSuccess
        401 -> RoseError
        403 -> AmberWarning
        404 -> IndigoInfo
        else -> RoseError
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_endpoint_result_${result.statusCode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Endpoint Name & Status Code Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.endpointName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // HTTP Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusBadgeColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (result.statusCode > 0) "HTTP ${result.statusCode} ${result.statusText}" else "Network Error",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusBadgeColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Route String
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = result.route,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation
            Text(
                text = result.diagnosticExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Recommended Fix
            if (!result.recommendedFix.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarningBg.copy(alpha = 0.3f),
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
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fix Advice:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text(
                                text = result.recommendedFix,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }

                        if (result.recommendedFix.contains(".htaccess")) {
                            IconButton(
                                onClick = {
                                    onCopy(
                                        "SetEnvIf Authorization \"(.*)\" HTTP_AUTHORIZATION=\$1",
                                        ".htaccess Fix Code"
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Fix",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Raw Response Body JSON Accordion
            if (!result.responseBodySnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { expandedJson = !expandedJson },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (expandedJson) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expandedJson) "Hide Raw Response JSON" else "View Raw Response JSON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = expandedJson) {
                    Column {
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
                                        text = "RAW HTTP RESPONSE PAYLOAD",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Slate400,
                                        fontWeight = FontWeight.Bold
                                    )

                                    TextButton(
                                        onClick = { onCopy(result.responseBodySnippet, "Raw JSON Response") },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Copy JSON",
                                            color = EmeraldSuccess,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = result.responseBodySnippet,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Slate200,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
