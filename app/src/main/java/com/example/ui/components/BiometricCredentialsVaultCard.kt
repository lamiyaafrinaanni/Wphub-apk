package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.SiteEntity
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus
import kotlinx.coroutines.delay

/**
 * BiometricPrompt Secured Credentials Vault Composable.
 * Protects saved WordPress Application Passwords, usernames, and REST API tokens.
 * Requires biometric verification (fingerprint, face unlock, or device PIN) before unmasking.
 */
@Composable
fun BiometricCredentialsVaultCard(
    currentSite: SiteEntity?,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = remember(context) { context as? FragmentActivity }

    var isUnlocked by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricAvailability(context) }

    val rawPassword = currentSite?.appPasswordToken ?: ""
    val maskedPassword = remember(rawPassword) {
        if (rawPassword.length > 8) {
            "${rawPassword.take(4)} •••• •••• ${rawPassword.takeLast(4)}"
        } else if (rawPassword.isNotBlank()) {
            "•••• •••• •••• ••••"
        } else {
            "No Application Password Saved"
        }
    }

    // Auto-lock credentials after 30 seconds of inactivity
    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            delay(30_000)
            isUnlocked = false
            onShowMessage("Credentials auto-locked after 30 seconds.")
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("biometric_credentials_vault_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
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
                            .background(if (isUnlocked) EmeraldSuccessBg else PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isUnlocked) EmeraldSuccess else PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Biometric Secured Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Securing saved Application Passwords & REST Tokens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isUnlocked) EmeraldSuccessBg else PrimaryIndigo.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isUnlocked) EmeraldSuccess else PrimaryIndigo.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isUnlocked) EmeraldSuccess else PrimaryIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUnlocked) "UNLOCKED" else "BIOMETRIC LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) EmeraldSuccess else PrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Site & Username Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WordPress Site Domain:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentSite?.url ?: "Not Connected",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Username:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentSite?.username ?: "admin",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Saved Application Password Value Row
            Text(
                text = "Saved Application Password / REST Token:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked) rawPassword.ifBlank { "No Password Set" } else maskedPassword,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) EmeraldSuccess else Slate100,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnlocked && rawPassword.isNotBlank()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(rawPassword))
                                onShowMessage("Application Password copied to clipboard!")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Biometric Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isUnlocked) {
                    Button(
                        onClick = {
                            if (activity == null) {
                                onShowMessage("Biometric prompt unavailable in this context.")
                                return@Button
                            }
                            isAuthenticating = true
                            BiometricAuthManager.authenticateToAccessCredentials(
                                activity = activity,
                                title = "WordPress Credentials Vault",
                                subtitle = "Verify identity to reveal saved Application Password",
                                description = "Confirm fingerprint, face, or device PIN to unmask ${currentSite?.name ?: "WordPress Site"}",
                                onSuccess = {
                                    isUnlocked = true
                                    isAuthenticating = false
                                    onShowMessage("Identity Verified! Credentials unlocked.")
                                },
                                onError = { errorMsg ->
                                    isAuthenticating = false
                                    onShowMessage(errorMsg)
                                }
                            )
                        },
                        enabled = !isAuthenticating && rawPassword.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_unlock_biometrics")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAuthenticating) "Verifying..." else "Verify Biometrics & Unlock", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            isUnlocked = false
                            onShowMessage("Credentials re-locked.")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Credentials", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(rawPassword))
                            onShowMessage("Application Password copied to clipboard!")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Code")
                    }
                }
            }

            AnimatedVisibility(visible = biometricStatus == BiometricStatus.NO_HARDWARE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Biometric hardware is not configured on this emulator/device. Device PIN / Password fallback is supported.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
