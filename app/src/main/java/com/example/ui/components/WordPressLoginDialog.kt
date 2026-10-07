package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SiteEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.WordPressNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressLoginDialog(
    currentSite: SiteEntity?,
    allSites: List<SiteEntity>,
    onDismiss: () -> Unit,
    onLogin: (
        siteId: String?,
        siteUrl: String,
        siteName: String,
        usernameOrEmail: String,
        password: String,
        role: String,
        rememberMe: Boolean
    ) -> Unit
) {
    var selectedSiteId by remember { mutableStateOf(currentSite?.id) }
    var siteUrl by remember { mutableStateOf(currentSite?.url ?: "https://") }
    var siteName by remember { mutableStateOf(currentSite?.name ?: "") }
    var usernameOrEmail by remember { mutableStateOf(currentSite?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(currentSite?.userRole ?: "Administrator") }
    var rememberMe by remember { mutableStateOf(true) }
    var authMode by remember { mutableStateOf("credentials") } // "credentials" or "app_password"
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showTroubleshooter by remember { mutableStateOf(false) }
    var showHttpWarningDialog by remember { mutableStateOf(false) }
    var showWebAuthDialog by remember { mutableStateOf(false) }

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

    val onConfirmSubmission = {
        val sanitizedUrl = cleanUrl(siteUrl)
        isSubmitting = true
        val cleanedPass = if (authMode == "app_password") password.replace(" ", "").trim() else password.trim()
        onLogin(
            selectedSiteId,
            sanitizedUrl,
            siteName,
            usernameOrEmail.trim(),
            cleanedPass,
            selectedRole,
            rememberMe
        )
    }

    if (showHttpWarningDialog) {
        AlertDialog(
            onDismissRequest = { showHttpWarningDialog = false },
            title = { Text("Unsecured Connection Warning") },
            text = { Text("You are connecting to an unencrypted HTTP site. Your login credentials and all site data will be transmitted in plain text across the network. Are you sure you want to proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHttpWarningDialog = false
                        onConfirmSubmission()
                    }
                ) {
                    Text("Proceed Anyway", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHttpWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTroubleshooter) {
        WordPressConnectTroubleshooterDialog(
            initialUrl = siteUrl,
            initialUsername = usernameOrEmail,
            onDismiss = { showTroubleshooter = false },
            onApplyAndConnect = { diagnosedUrl, diagnosedUser, diagnosedPass ->
                siteUrl = diagnosedUrl
                if (diagnosedUser.isNotBlank()) usernameOrEmail = diagnosedUser
                if (diagnosedPass.isNotBlank()) {
                    password = diagnosedPass
                    authMode = "app_password"
                }
                showTroubleshooter = false
            }
        )
    }

    if (showWebAuthDialog) {
        WordPressWebAuthorizationDialog(
            siteUrl = if (siteUrl.isNotBlank() && siteUrl != "https://") siteUrl else "https://trendifyboost.com",
            initialUsername = usernameOrEmail,
            onDismiss = { showWebAuthDialog = false },
            onAuthorized = { authSiteUrl, authUser, authAppPassword ->
                showWebAuthDialog = false
                val sanitizedUrl = cleanUrl(authSiteUrl)
                val cleanAppPass = authAppPassword.replace(" ", "").trim()
                siteUrl = sanitizedUrl
                if (authUser.isNotBlank()) usernameOrEmail = authUser
                password = cleanAppPass
                authMode = "app_password"
                onLogin(
                    selectedSiteId,
                    sanitizedUrl,
                    siteName.ifBlank { "WordPress Site" },
                    usernameOrEmail.trim(),
                    cleanAppPass,
                    selectedRole,
                    rememberMe
                )
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_wordpress_login")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with WordPress Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WordPressNavy)
                    ) {
                        Text(
                            text = "W",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WordPress Login",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in with Username/Email & Password",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = authMode == "credentials",
                        onClick = { authMode = "credentials" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Username & Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        selected = authMode == "app_password",
                        onClick = { authMode = "app_password" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("App Password / Token", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Site selection chips if sites exist
                if (allSites.isNotEmpty()) {
                    Text(
                        text = "Target WordPress Site",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allSites.take(3).forEach { site ->
                            FilterChip(
                                selected = selectedSiteId == site.id,
                                onClick = {
                                    selectedSiteId = site.id
                                    siteUrl = site.url
                                    siteName = site.name
                                    if (site.username.isNotBlank()) usernameOrEmail = site.username
                                },
                                label = { Text(site.name, maxLines = 1) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Site URL Field
                OutlinedTextField(
                    value = siteUrl,
                    onValueChange = {
                        siteUrl = it
                        errorMessage = null
                    },
                    label = { Text("WordPress Site URL") },
                    placeholder = { Text("https://mywordpress.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (siteUrl.contains("/wp-admin")) {
                            TextButton(onClick = { siteUrl = cleanUrl(siteUrl) }) {
                                Text("Fix URL", fontSize = 10.sp)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wp_site_url")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        if (siteUrl.isBlank() || siteUrl == "https://") {
                            siteUrl = "https://trendifyboost.com"
                        }
                        showWebAuthDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1-Click Web Authorization & Connect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Username or Email Field
                OutlinedTextField(
                    value = usernameOrEmail,
                    onValueChange = {
                        usernameOrEmail = it
                        errorMessage = null
                    },
                    label = { Text("WordPress Username or Email") },
                    placeholder = { Text("admin or user@domain.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wp_username_or_email")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = {
                        Text(
                            if (authMode == "credentials") "WordPress Password"
                            else "Application Password / Token"
                        )
                    },
                    placeholder = {
                        Text(
                            if (authMode == "credentials") "Enter your WordPress password"
                            else "e.g. abcd efgh ijkl mnop"
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wp_password")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // User Role Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "User Role",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Administrator", "Shop Manager", "Editor").forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remember Me Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        modifier = Modifier.testTag("checkbox_remember_me")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Remember credentials & stay logged in",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Error alert if present
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Troubleshoot / Connect Issues Helper
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection issues? Run REST API test & fix guides",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showTroubleshooter = true }) {
                            Text("Diagnose", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val sanitizedUrl = cleanUrl(siteUrl)
                            if (sanitizedUrl.isBlank() || sanitizedUrl == "https://") {
                                errorMessage = "Please provide your WordPress site URL"
                                return@Button
                            }
                            if (usernameOrEmail.isBlank()) {
                                errorMessage = "Username or email is required"
                                return@Button
                            }
                            if (password.isBlank()) {
                                errorMessage = "Password is required"
                                return@Button
                            }
                            if (sanitizedUrl.startsWith("http://")) {
                                showHttpWarningDialog = true
                            } else {
                                onConfirmSubmission()
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = WordPressNavy),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_submit_wp_login")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logging in...")
                        } else {
                            Icon(
                                Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log In")
                        }
                    }
                }
            }
        }
    }
}
