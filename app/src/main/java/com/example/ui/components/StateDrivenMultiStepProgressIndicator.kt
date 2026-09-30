package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionStage
import com.example.ui.ProgressState
import com.example.ui.StageStatus
import com.example.ui.StepProgress
import com.example.ui.theme.*

/**
 * State-driven multi-step progress indicator for the WordPress onboarding connection screen.
 * Tracks and provides real-time feedback for:
 * 1. URL validation (DNS, TLS/SSL, host reachability)
 * 2. REST API discovery (/wp-json/, namespaces, WooCommerce detection)
 * 3. Authentication handshake (Application Password verification, roles & scopes)
 */
@Composable
fun StateDrivenMultiStepProgressIndicator(
    progressState: ProgressState,
    onOpenDebugger: () -> Unit,
    onOpenWebAuth: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val steps = progressState.steps
    val animatedProgress by animateFloatAsState(
        targetValue = progressState.progressFraction,
        animationSpec = tween(durationMillis = 350),
        label = "state_driven_pipeline_progress"
    )

    val currentFailedStage = (progressState as? ProgressState.Error)?.failedStage
    val isComplete = progressState.isComplete
    val isFailed = progressState.isFailed
    val isLoading = progressState.isLoading

    val borderColor = when {
        isComplete -> EmeraldSuccess.copy(alpha = 0.55f)
        isFailed -> RoseError.copy(alpha = 0.55f)
        isLoading -> PrimaryIndigo.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("state_driven_progress_indicator_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Title, Real-time Status Icon & Raw Logs Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isComplete -> EmeraldSuccessBg
                            isFailed -> RoseError.copy(alpha = 0.15f)
                            isLoading -> PrimaryIndigo.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                isComplete -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                isFailed -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Failed",
                                        tint = RoseError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                isLoading -> {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp),
                                        color = PrimaryIndigo
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Idle",
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Connection Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (progressState) {
                                is ProgressState.Idle -> "Tracks URL reachability, REST API & auth handshake"
                                is ProgressState.UrlValidation -> "Stage 1/3: Validating host reachability & SSL..."
                                is ProgressState.RestDiscovery -> "Stage 2/3: Discovering REST API & namespaces..."
                                is ProgressState.Handshake -> "Stage 3/3: Validating Application Password credentials..."
                                is ProgressState.Success -> "All 3 checks passed • Connected to ${progressState.site.name}"
                                is ProgressState.Error -> "Check failed at Stage ${progressState.failedStage.stepIndex} of 3"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = if (isLoading || isComplete) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                isComplete -> EmeraldSuccess
                                isFailed -> RoseError
                                isLoading -> PrimaryIndigo
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                TextButton(
                    onClick = onOpenDebugger,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("button_open_raw_logs")
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Raw Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Segmented Step Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEach { step ->
                    val isCurrentStage = progressState.currentStage == step.stage
                    val stageColor = when (step.status) {
                        StageStatus.SUCCESS -> EmeraldSuccess
                        StageStatus.FAILURE -> RoseError
                        StageStatus.IN_PROGRESS -> PrimaryIndigo
                        StageStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("step_indicator_${step.stage.stepIndex}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(stageColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = step.stage.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrentStage || step.isSuccess) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentStage) PrimaryIndigo else stageColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-Stage Linear Progress Indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("state_driven_progress_bar"),
                color = when {
                    isComplete -> EmeraldSuccess
                    isFailed -> RoseError
                    else -> PrimaryIndigo
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Detailed Interactive Step Rows
            steps.forEachIndexed { index, step ->
                StateDrivenStepRow(
                    step = step,
                    isCurrentStage = progressState.currentStage == step.stage
                )
                if (index < steps.size - 1) {
                    val connectorColor = when {
                        step.isSuccess -> EmeraldSuccess.copy(alpha = 0.45f)
                        step.isInProgress -> PrimaryIndigo.copy(alpha = 0.4f)
                        step.isFailure -> RoseError.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .width(2.dp)
                            .height(14.dp)
                            .background(connectorColor)
                    )
                }
            }

            // Diagnostic Advice on Error
            if (progressState is ProgressState.Error) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseError.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_error_diagnostic_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Issue in Stage ${progressState.failedStage.stepIndex}: ${progressState.failedStage.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoseError
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = progressState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!progressState.diagnosticAdvice.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = progressState.diagnosticAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (onRetry != null) {
                                OutlinedButton(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("button_retry_pipeline")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = onOpenWebAuth,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("button_error_web_auth")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WP-Admin Login", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Success Summary Box
            if (progressState is ProgressState.Success) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSuccessBg,
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_success_summary_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected: ${progressState.site.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Authenticated as ${progressState.site.userDisplayName.ifBlank { progressState.site.username }} (${progressState.site.userRole})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual step row rendering real-time progress, badges, latency and expandable logs.
 */
@Composable
private fun StateDrivenStepRow(
    step: StepProgress,
    isCurrentStage: Boolean
) {
    var expandedLogs by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("state_driven_step_${step.stage.stepIndex}"),
        verticalAlignment = Alignment.Top
    ) {
        // Step Icon Circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        StageStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        StageStatus.IN_PROGRESS -> PrimaryIndigo.copy(alpha = 0.15f)
                        StageStatus.SUCCESS -> EmeraldSuccessBg
                        StageStatus.FAILURE -> RoseError.copy(alpha = 0.15f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StageStatus.PENDING -> {
                    Text(
                        text = "${step.stage.stepIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StageStatus.IN_PROGRESS -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
                StageStatus.SUCCESS -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
                StageStatus.FAILURE -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = RoseError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${step.stage.stepIndex}. ${step.stage.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrentStage || step.isSuccess) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (step.responseTimeMs != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "${step.responseTimeMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (step.status) {
                            StageStatus.SUCCESS -> EmeraldSuccessBg
                            StageStatus.FAILURE -> RoseError.copy(alpha = 0.15f)
                            StageStatus.IN_PROGRESS -> PrimaryIndigo.copy(alpha = 0.12f)
                            StageStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = step.statusText ?: when (step.status) {
                                StageStatus.PENDING -> "WAITING"
                                StageStatus.IN_PROGRESS -> "CHECKING..."
                                StageStatus.SUCCESS -> "PASSED"
                                StageStatus.FAILURE -> "FAILED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (step.status) {
                                StageStatus.SUCCESS -> EmeraldSuccess
                                StageStatus.FAILURE -> RoseError
                                StageStatus.IN_PROGRESS -> PrimaryIndigo
                                StageStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = step.detailMessage ?: step.stage.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = when (step.status) {
                    StageStatus.FAILURE -> RoseError
                    StageStatus.SUCCESS -> MaterialTheme.colorScheme.onSurface
                    StageStatus.IN_PROGRESS -> PrimaryIndigo
                    StageStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                lineHeight = 16.sp
            )

            // Optional Sub-logs dropdown
            if (step.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { expandedLogs = !expandedLogs }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (expandedLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (expandedLogs) "Hide details" else "${step.logs.size} sub-checks",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expandedLogs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    ) {
                        step.logs.forEach { log ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryIndigo.copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
