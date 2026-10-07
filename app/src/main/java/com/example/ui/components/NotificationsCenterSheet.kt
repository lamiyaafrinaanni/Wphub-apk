package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NotificationItemEntity
import com.example.data.local.NotificationSettingsEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterSheet(
    notifications: List<NotificationItemEntity>,
    settings: NotificationSettingsEntity?,
    onDismiss: () -> Unit,
    onNotificationClick: (NotificationItemEntity) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onUpdateSettings: (NotificationSettingsEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") } // "all", "order", "comment", "stock", "inquiry"
    var showSettingsInSheet by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        if (selectedFilter == "all") notifications
        else notifications.filter { it.type.equals(selectedFilter, ignoreCase = true) }
    }

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("notifications_center_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BadgeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notifications Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryIndigo
                                ) {
                                    Text(
                                        text = "$unreadCount new",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Real-time WordPress & WooCommerce alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSettingsInSheet = !showSettingsInSheet },
                        modifier = Modifier.testTag("toggle_settings_in_sheet")
                    ) {
                        Icon(
                            imageVector = if (showSettingsInSheet) Icons.Default.List else Icons.Default.Tune,
                            contentDescription = "Notification Settings",
                            tint = PrimaryIndigo
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showSettingsInSheet) {
                // Settings view
                if (settings != null) {
                    NotificationSettingsTab(
                        settings = settings,
                        onSaveSettings = onUpdateSettings
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Notifications List view
                Spacer(modifier = Modifier.height(6.dp))

                // Filters & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = when (selectedFilter) {
                            "order" -> 1
                            "comment" -> 2
                            "stock" -> 3
                            "inquiry" -> 4
                            else -> 0
                        },
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = selectedFilter == "all",
                            onClick = { selectedFilter = "all" },
                            text = { Text("All (${notifications.size})", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedFilter == "order",
                            onClick = { selectedFilter = "order" },
                            text = { Text("Orders", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedFilter == "comment",
                            onClick = { selectedFilter = "comment" },
                            text = { Text("Comments", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedFilter == "stock",
                            onClick = { selectedFilter = "stock" },
                            text = { Text("Low Stock", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedFilter == "inquiry",
                            onClick = { selectedFilter = "inquiry" },
                            text = { Text("Inquiries", fontSize = 12.sp) }
                        )
                    }

                    Row {
                        if (unreadCount > 0) {
                            TextButton(
                                onClick = onMarkAllAsRead,
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Mark Read", fontSize = 11.sp, color = PrimaryIndigo)
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            TextButton(
                                onClick = onClearAll,
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = RoseError)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // List
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You're all caught up! Use the simulation buttons above to test push notifications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextBodyMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredNotifications, key = { it.id }) { notif ->
                            NotificationCardItem(
                                notification = notif,
                                onClick = { onNotificationClick(notif) },
                                onDelete = { onDeleteNotification(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    notification: NotificationItemEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconColor, badgeBg) = when (notification.type.lowercase()) {
        "order" -> Triple(Icons.Default.ShoppingBag, PrimaryIndigo, BadgeBackground)
        "comment" -> Triple(Icons.Default.ChatBubble, WooPurple, BadgeBackground)
        "stock" -> Triple(Icons.Default.Warning, AmberWarning, AmberWarningBg)
        "inquiry" -> Triple(Icons.Default.Email, WPCyan, Color(0xFFE0F7FA))
        else -> Triple(Icons.Default.Notifications, PrimaryIndigo, BadgeBackground)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color.White else BgGradientStart
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!notification.isRead) PrimaryIndigo.copy(alpha = 0.35f) else BorderSlate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextBodyMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (notification.extraData != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = notification.extraData,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actionLabel = when (notification.targetType) {
                        "order" -> "View in Store →"
                        "comment" -> "Moderate in Content →"
                        "product" -> "Update Stock →"
                        "customer" -> "View CRM Profile →"
                        else -> "Open →"
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
