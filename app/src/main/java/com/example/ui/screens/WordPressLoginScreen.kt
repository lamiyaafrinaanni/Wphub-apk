package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import androidx.compose.foundation.horizontalScroll
import com.example.ui.components.WordPressConnectTroubleshooterDialog
import com.example.ui.components.WordPressWebAuthorizationDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressLoginScreen(
    currentSite: SiteEntity?,
    allSites: List<SiteEntity>,
    onLoginSuccess: (SiteEntity) -> Unit,
    onLoginSubmit: (
        siteId: String?,
        siteUrl: String,
        siteName: String,
        usernameOrEmail: String,
        password: String,
        role: String,
        onSuccess: (SiteEntity) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    var selectedSiteId by remember { mutableStateOf(currentSite?.id) }
    var siteUrl by remember {
        mutableStateOf(currentSite?.url ?: "https://")
    }
    var siteName by remember {
        mutableStateOf(currentSite?.name ?: "")
    }
    var usernameOrEmail by remember {
        mutableStateOf(currentSite?.username ?: "")
    }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Segmented tabs: 0 -> "Username & Password", 1 -> "App Password/Token"
    var selectedAuthModeTab by remember { mutableIntStateOf(0) }

    var selectedRole by remember { mutableStateOf(currentSite?.userRole ?: "Administrator") }
    var rememberMe by remember { mutableStateOf(true) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTroubleshooter by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }
    var showHttpWarningDialog by remember { mutableStateOf(false) }
    var showWebAuthDialog by remember { mutableStateOf(false) }

    fun cleanUrl(raw: String): String {
        var url = raw.trim()
        if (url.contains("/wp-admin")) url = url.substringBefore("/wp-admin")
        if (url.contains("/wp-login.php")) url = url.substringBefore("/wp-login.php")
        url = url.removeSuffix("/")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    val performLoginSubmission = {
        val sanitizedUrl = cleanUrl(siteUrl)
        val sanitizedUser = usernameOrEmail.trim()
        val sanitizedPass = if (selectedAuthModeTab == 1) {
            password.replace(" ", "").trim()
        } else {
            password.trim()
        }

        errorMessage = null
        isSubmitting = true
        focusManager.clearFocus()

        val matchingSite = allSites.find {
            it.url.equals(sanitizedUrl, ignoreCase = true) || it.name.equals(siteName, ignoreCase = true)
        }

        onLoginSubmit(
            matchingSite?.id,
            sanitizedUrl,
            siteName.ifBlank { "WordPress Site" },
            sanitizedUser,
            sanitizedPass,
            selectedRole,
            { site ->
                isSubmitting = false
                onLoginSuccess(site)
            },
            { err ->
                isSubmitting = false
                errorMessage = err
            }
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
                        performLoginSubmission()
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

    // If user presses back and an authenticated site exists, allow returning to dashboard
    BackHandler(enabled = currentSite?.isAuthenticated == true && onDismiss != null) {
        onDismiss?.invoke()
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
                    selectedAuthModeTab = 1
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
                selectedAuthModeTab = 1

                val matchingSite = allSites.find {
                    it.url.equals(sanitizedUrl, ignoreCase = true) || it.name.equals(siteName, ignoreCase = true)
                }

                onLoginSubmit(
                    matchingSite?.id,
                    sanitizedUrl,
                    siteName.ifBlank { "WordPress Site" },
                    usernameOrEmail.trim(),
                    cleanAppPass,
                    selectedRole,
                    { site ->
                        isSubmitting = false
                        onLoginSuccess(site)
                    },
                    { err ->
                        isSubmitting = false
                        errorMessage = err
                    }
                )
            }
        )
    }

    fun handleLogin() {
        val sanitizedUrl = cleanUrl(siteUrl)
        val sanitizedUser = usernameOrEmail.trim()
        // If app password tab is selected, strip accidental whitespace often pasted from WordPress admin
        val sanitizedPass = if (selectedAuthModeTab == 1) {
            password.replace(" ", "").trim()
        } else {
            password.trim()
        }

        if (sanitizedUrl.isBlank() || sanitizedUrl == "https://") {
            errorMessage = "Please enter a valid WordPress Site URL"
            return
        }
        if (sanitizedUser.isBlank()) {
            errorMessage = "Please enter your WordPress username or email"
            return
        }
        if (sanitizedPass.isBlank()) {
            errorMessage = if (selectedAuthModeTab == 1) {
                "Please enter your WordPress Application Password"
            } else {
                "Please enter your WordPress password"
            }
            return
        }

        if (sanitizedUrl.startsWith("http://")) {
            showHttpWarningDialog = true
        } else {
            performLoginSubmission()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_wordpress_login"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top App Bar / Header with Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // WordPress Emblem Badge
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigo,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "W",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "WordPress Login",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Sign in with Username/Email & Password",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Close / Dismiss 'X' Button if return navigation is available
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_close_login_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Auth Mode Segmented Tabs: "Username & Password" vs "App Password/Token"
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 0: Username & Password
                    Surface(
                        onClick = { selectedAuthModeTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAuthModeTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedAuthModeTab == 0) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_auth_username_password")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Username & Password",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedAuthModeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAuthModeTab == 0) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tab 1: App Password / Token
                    Surface(
                        onClick = { selectedAuthModeTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedAuthModeTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (selectedAuthModeTab == 1) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_auth_app_password")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "App Password/Token",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedAuthModeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAuthModeTab == 1) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // If user has actual saved sites from local database, show them here
            if (allSites.isNotEmpty()) {
                Text(
                    text = "Saved WordPress Sites",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSites.forEach { site ->
                        val isSelected = selectedSiteId == site.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSiteId = site.id
                                siteUrl = site.url
                                siteName = site.name
                                usernameOrEmail = site.username
                            },
                            label = { Text("${site.iconEmoji} ${site.name}", maxLines = 1) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    FilterChip(
                        selected = selectedSiteId == null,
                        onClick = {
                            selectedSiteId = null
                            siteUrl = "https://"
                            siteName = ""
                            usernameOrEmail = ""
                        },
                        label = { Text("+ New Site") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Form Fields
            // 1. WordPress Site URL
            OutlinedTextField(
                value = siteUrl,
                onValueChange = { siteUrl = it },
                label = { Text("WordPress Site URL") },
                placeholder = { Text("https://your-wordpress-site.com") },
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryIndigo)
                },
                trailingIcon = {
                    if (siteUrl.isNotBlank() && siteUrl != "https://") {
                        IconButton(onClick = { siteUrl = "https://" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    focusedLabelColor = PrimaryIndigo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_wordpress_site_url")
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

            Spacer(modifier = Modifier.height(14.dp))

            // Site Name or Label (Optional)
            OutlinedTextField(
                value = siteName,
                onValueChange = { siteName = it },
                label = { Text("Site Name or Label (Optional)") },
                placeholder = { Text("e.g. My WordPress Site") },
                leadingIcon = {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryIndigo)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    focusedLabelColor = PrimaryIndigo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_wordpress_site_name")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. WordPress Username or Email
            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it },
                label = { Text("WordPress Username or Email") },
                placeholder = { Text("admin") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigo)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    focusedLabelColor = PrimaryIndigo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_wordpress_username")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. WordPress Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        if (selectedAuthModeTab == 1) "Application Password" else "WordPress Password"
                    )
                },
                placeholder = {
                    Text(
                        if (selectedAuthModeTab == 1) "xxxx xxxx xxxx xxxx" else "••••••••••••"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedAuthModeTab == 1) Icons.Default.Key else Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryIndigo
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { handleLogin() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    focusedLabelColor = PrimaryIndigo
                ),
                supportingText = {
                    if (selectedAuthModeTab == 1) {
                        Text(
                            text = "Created in WP Admin > Users > Profile > Application Passwords. Spaces are stripped automatically.",
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Your standard WordPress administrative login credentials.",
                            fontSize = 11.sp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_wordpress_password")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // User Role Selector & Remember Me Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role Picker Box
                Box {
                    OutlinedButton(
                        onClick = { showRoleMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_select_role")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Role: $selectedRole", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        listOf("Administrator", "Shop Manager", "Editor", "Author").forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    selectedRole = role
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }

                // Remember Me Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Remember Me",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        modifier = Modifier.testTag("switch_remember_me")
                    )
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseErrorBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RoseError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "Login error",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Log In Button
            Button(
                onClick = { handleLogin() },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_wordpress_login_submit")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Authenticating with WP REST API...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log In to WordPress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic & Troubleshooter Link Card
            Surface(
                onClick = { showTroubleshooter = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_open_login_troubleshooter")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Troubleshoot,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connection Troubleshooter & Test",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Test REST API endpoints, SSL handshake, or App Passwords",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info note
            Text(
                text = "WordPress Mobile Hub connects directly from this device to your WordPress REST API. No intermediate proxies or third-party cloud servers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
