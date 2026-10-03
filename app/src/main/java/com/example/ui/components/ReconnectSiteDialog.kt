package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.SiteEntity
import com.example.ui.screens.WordPressAuthWebViewDialog
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoseErrorBg

@Composable
fun ReconnectSiteDialog(
    site: SiteEntity,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onReconnect: (username: String, appPassword: String) -> Unit,
    onRemoveSite: (() -> Unit)? = null
) {
    var usernameInput by remember(site) { mutableStateOf(site.username) }
    var passwordInput by remember(site) { mutableStateOf(com.example.data.security.SecureCredentialsVault.decrypt(site.appPasswordToken)) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showWebAuthDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val displayError = errorMessage ?: validationError

    if (showWebAuthDialog) {
        WordPressAuthWebViewDialog(
            siteUrl = site.url,
            onDismiss = { showWebAuthDialog = false },
            onAuthSuccess = { _, userLogin, pwd ->
                showWebAuthDialog = false
                usernameInput = userLogin
                passwordInput = pwd
                validationError = null
                onReconnect(userLogin, pwd)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("reconnect_site_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Surface(
                    shape = CircleShape,
                    color = RoseErrorBg,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VpnKeyOff,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "HTTP 401: Reconnect Site",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "The WordPress application password or credentials for ${site.name} were rejected or expired.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Site Info Banner Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(site.iconEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = site.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = site.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Error Message if present
                displayError?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = RoseErrorBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseError,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1-Click Web Authorization Shortcut Button
                OutlinedButton(
                    onClick = { showWebAuthDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("button_web_reauthorize")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-Click Re-authorize via Web Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = " OR UPDATE MANUALLY ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username TextField
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        usernameInput = it
                        validationError = null
                    },
                    label = { Text("WordPress Admin Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reconnect_username")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Application Password TextField
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        validationError = null
                    },
                    label = { Text("New Application Password") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (usernameInput.isBlank() || passwordInput.isBlank()) {
                            validationError = "Username and Application Password cannot be empty."
                        } else {
                            onReconnect(usernameInput, passwordInput)
                        }
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reconnect_password")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (usernameInput.isBlank() || passwordInput.isBlank()) {
                            validationError = "Username and Application Password cannot be empty."
                        } else {
                            onReconnect(usernameInput, passwordInput)
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_reconnect_submit")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Credentials...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Save Credentials", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("button_reconnect_dismiss")
                    ) {
                        Text("Keep Offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (onRemoveSite != null) {
                        TextButton(
                            onClick = onRemoveSite,
                            modifier = Modifier.testTag("button_reconnect_remove")
                        ) {
                            Text("Remove Site", color = RoseError)
                        }
                    }
                }
            }
        }
    }
}
