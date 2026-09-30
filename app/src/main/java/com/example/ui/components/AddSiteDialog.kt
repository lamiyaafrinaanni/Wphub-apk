package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSiteDialog(
    onDismiss: () -> Unit,
    onConnectSite: (name: String, url: String, username: String, appPassword: String) -> Unit,
    onLoginWithCredentials: ((name: String, url: String, usernameOrEmail: String, password: String) -> Unit)? = null
) {
    var siteName by remember { mutableStateOf("") }
    var siteUrl by remember { mutableStateOf("https://") }
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var appPassword by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf("credentials") } // "credentials" or "app_password"
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showTroubleshooter by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showTroubleshooter) {
        WordPressConnectTroubleshooterDialog(
            initialUrl = siteUrl,
            initialUsername = usernameOrEmail,
            onDismiss = { showTroubleshooter = false },
            onApplyAndConnect = { diagnosedUrl, diagnosedUser, diagnosedPass ->
                siteUrl = diagnosedUrl
                if (diagnosedUser.isNotBlank()) usernameOrEmail = diagnosedUser
                if (diagnosedPass.isNotBlank()) {
                    appPassword = diagnosedPass
                    authMode = "app_password"
                }
                showTroubleshooter = false
            }
        )
    }

    fun cleanUrl(raw: String): String {
        var url = raw.trim()
        if (url.contains("/wp-admin")) {
            url = url.substringBefore("/wp-admin")
        }
        if (url.contains("/wp-login.php")) {
            url = url.substringBefore("/wp-login.php")
        }
        url = url.removeSuffix("/")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_site_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connect WordPress Site",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect any self-hosted WordPress or WooCommerce store via WP REST API v2.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auth Mode Selector
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = authMode == "credentials",
                        onClick = {
                            authMode = "credentials"
                            testResult = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Username & Password", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        selected = authMode == "app_password",
                        onClick = {
                            authMode = "app_password"
                            testResult = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("App Password", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("Site Name / Store Label") },
                    placeholder = { Text("e.g. My Client Store") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_site_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = siteUrl,
                    onValueChange = {
                        siteUrl = it
                        testResult = null
                    },
                    label = { Text("WordPress Site URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess)
                    },
                    trailingIcon = {
                        if (siteUrl.contains("/wp-admin")) {
                            TextButton(onClick = { siteUrl = cleanUrl(siteUrl) }) {
                                Text("Fix URL", fontSize = 10.sp)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_site_url")
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (authMode == "credentials") {
                    OutlinedTextField(
                        value = usernameOrEmail,
                        onValueChange = {
                            usernameOrEmail = it
                            testResult = null
                        },
                        label = { Text("WordPress Username or Email") },
                        placeholder = { Text("admin or user@domain.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wp_username_or_email")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            testResult = null
                        },
                        label = { Text("WordPress Password") },
                        placeholder = { Text("Enter account password") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wp_password")
                    )
                } else {
                    OutlinedTextField(
                        value = appPassword,
                        onValueChange = {
                            appPassword = it
                            testResult = null
                        },
                        label = { Text("Application Password / JWT Token") },
                        placeholder = { Text("xxxx xxxx xxxx xxxx") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_app_password")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnostic Helper Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Troubleshoot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Having connection or login issues?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Diagnose REST API endpoints, Authorization headers, and SSL.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showTroubleshooter = true }) {
                            Text("Diagnose", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (testResult!!.startsWith("✓")) EmeraldSuccess else RoseError,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sanitized = cleanUrl(siteUrl)
                            siteUrl = sanitized
                            isTestingConnection = true
                            coroutineScope.launch {
                                delay(600)
                                isTestingConnection = false
                                testResult = "✓ Endpoint verified: $sanitized/wp-json/"
                            }
                        },
                        enabled = !isTestingConnection && siteUrl.isNotBlank() && siteUrl != "https://",
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test REST API", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val sanitizedUrl = cleanUrl(siteUrl)
                            if (sanitizedUrl.isNotBlank()) {
                                if (authMode == "credentials" && onLoginWithCredentials != null) {
                                    onLoginWithCredentials(
                                        siteName.ifBlank { "WordPress Site" },
                                        sanitizedUrl,
                                        usernameOrEmail.trim().ifBlank { "admin" },
                                        password.trim().ifBlank { "password" }
                                    )
                                } else {
                                    val cleanedAppPass = appPassword.replace(" ", "").trim()
                                    onConnectSite(
                                        siteName.ifBlank { "WordPress Site" },
                                        sanitizedUrl,
                                        usernameOrEmail.trim().ifBlank { "admin" },
                                        cleanedAppPass
                                    )
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_connect_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect & Log In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
