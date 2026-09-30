package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.NotificationSettingsEntity
import com.example.ui.theme.*

@Composable
fun NotificationSettingsTab(
    settings: NotificationSettingsEntity,
    onSaveSettings: (NotificationSettingsEntity) -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var ordersEnabled by remember(settings) { mutableStateOf(settings.ordersEnabled) }
    var commentsEnabled by remember(settings) { mutableStateOf(settings.commentsEnabled) }
    var lowStockEnabled by remember(settings) { mutableStateOf(settings.lowStockEnabled) }
    var inquiriesEnabled by remember(settings) { mutableStateOf(settings.customerInquiriesEnabled) }
    var soundEnabled by remember(settings) { mutableStateOf(settings.soundEnabled) }
    var vibrationEnabled by remember(settings) { mutableStateOf(settings.vibrationEnabled) }
    var threshold by remember(settings) { mutableIntStateOf(settings.lowStockThreshold) }

    fun commitChanges() {
        val updated = settings.copy(
            ordersEnabled = ordersEnabled,
            commentsEnabled = commentsEnabled,
            lowStockEnabled = lowStockEnabled,
            customerInquiriesEnabled = inquiriesEnabled,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            lowStockThreshold = threshold
        )
        onSaveSettings(updated)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_settings_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission Status Banner
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Notifications Disabled",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Grant Android notification permission to receive real-time push alerts on your lock screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextBodyMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Push Notification Event Types
        item {
            Text(
                text = "REAL-TIME PUSH EVENT SUBSCRIPTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // WooCommerce Orders Toggle
                    NotificationToggleRow(
                        icon = Icons.Default.ShoppingBag,
                        iconTint = PrimaryIndigo,
                        title = "WooCommerce Orders",
                        description = "Instant alerts for new customer orders, checkout payments, and refund requests",
                        checked = ordersEnabled,
                        onCheckedChange = {
                            ordersEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_orders_notifications"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate200)

                    // Customer Inquiries & CRM Toggle
                    NotificationToggleRow(
                        icon = Icons.Default.ContactSupport,
                        iconTint = WPCyan,
                        title = "Customer Inquiries & CRM Leads",
                        description = "Notify when visitors submit contact forms, inquiries, or VIP account requests",
                        checked = inquiriesEnabled,
                        onCheckedChange = {
                            inquiriesEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_inquiries_notifications"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate200)

                    // Low Stock Alerts Toggle
                    NotificationToggleRow(
                        icon = Icons.Default.Inventory2,
                        iconTint = AmberWarning,
                        title = "Low Stock Alerts",
                        description = "Urgent alerts when product inventory drops below critical stock threshold",
                        checked = lowStockEnabled,
                        onCheckedChange = {
                            lowStockEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_stock_notifications"
                    )

                    // Low Stock Threshold Sub-setting
                    AnimatedVisibility(visible = lowStockEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, start = 44.dp)
                                .background(BadgeBackground, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Alert Threshold Quantity:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200)
                                ) {
                                    Text(
                                        text = "$threshold units",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigoDark,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = threshold.toFloat(),
                                onValueChange = {
                                    threshold = it.toInt()
                                },
                                onValueChangeFinished = {
                                    commitChanges()
                                },
                                valueRange = 1f..25f,
                                steps = 24,
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryIndigo,
                                    activeTrackColor = PrimaryIndigo
                                ),
                                modifier = Modifier.testTag("slider_stock_threshold")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate200)

                    // Comments on Posts Toggle
                    NotificationToggleRow(
                        icon = Icons.Default.ChatBubbleOutline,
                        iconTint = WooPurple,
                        title = "Post Comments & Discussions",
                        description = "Alerts when visitors post comments on articles or reply to discussion threads",
                        checked = commentsEnabled,
                        onCheckedChange = {
                            commentsEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_comments_notifications"
                    )
                }
            }
        }

        // Section: Sound & Behavior Preferences
        item {
            Text(
                text = "DELIVERY & BEHAVIOR PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    NotificationToggleRow(
                        icon = Icons.Default.VolumeUp,
                        iconTint = PrimaryIndigo,
                        title = "Notification Sound",
                        description = "Play default WordPress chime when critical notifications arrive",
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_sound_notifications"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate200)

                    NotificationToggleRow(
                        icon = Icons.Default.Vibration,
                        iconTint = PrimaryIndigo,
                        title = "Haptic Vibration",
                        description = "Vibrate device for high-priority WooCommerce store alerts",
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            commitChanges()
                        },
                        tag = "toggle_vibration_notifications"
                    )
                }
            }
        }

        // Section: Channels & Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BadgeBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Configured for Android 14 / Notification Channels",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigoDark,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Each event type routes through dedicated system notification channels (Orders, Comments, Stock, Inquiries) allowing fine-grained Do-Not-Disturb overrides in Android system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextBodyMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryIndigo
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
