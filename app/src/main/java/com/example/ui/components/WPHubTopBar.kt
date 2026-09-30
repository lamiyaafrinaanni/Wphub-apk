package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldSuccess

@Composable
fun WPHubTopBar(
    currentSite: SiteEntity?,
    isSyncing: Boolean,
    unreadNotificationCount: Int = 0,
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onOpenDrawerClick: () -> Unit = {},
    onSiteSelectorClick: () -> Unit,
    onSyncClick: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onNotificationBellClick: () -> Unit = {},
    onUserLoginClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Menu Drawer Toggle Button
            IconButton(
                onClick = onOpenDrawerClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_menu_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Drawer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Site Switcher Chip
            Surface(
                onClick = onSiteSelectorClick,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("top_bar_site_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentSite?.iconEmoji ?: "🌐",
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentSite?.name ?: "Select Site",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentSite?.sslEnabled == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "SSL Verified",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = currentSite?.url?.removePrefix("https://")?.removePrefix("http://") ?: "WP REST API",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Site",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // User Profile / Login Avatar Chip
            Surface(
                onClick = onUserLoginClick,
                shape = CircleShape,
                color = if (currentSite?.isAuthenticated == true) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_user_profile_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (currentSite?.isAuthenticated == true) {
                        Text(
                            text = (currentSite.username.take(1).ifBlank { "A" }).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Log In",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dark Mode / Light Mode toggle icon button
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_theme_toggle_button")
            ) {
                val themeIcon = when (themeMode) {
                    AppThemeMode.LIGHT -> Icons.Default.DarkMode
                    AppThemeMode.DARK -> Icons.Default.LightMode
                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                }
                val themeDesc = when (themeMode) {
                    AppThemeMode.LIGHT -> "Switch to Dark Mode"
                    AppThemeMode.DARK -> "Switch to Light Mode"
                    AppThemeMode.SYSTEM -> "System Theme Mode (Tap to switch)"
                }
                Icon(
                    imageVector = themeIcon,
                    contentDescription = themeDesc,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification Bell with unread counter badge
            IconButton(
                onClick = onNotificationBellClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_bar_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Push Notifications ($unreadNotificationCount unread)",
                        tint = if (unreadNotificationCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Sync Button with status
            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("sync_site_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync WordPress REST API",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = (if (isSyncing) Modifier.rotate(rotation) else Modifier).size(20.dp)
                )
            }
        }
    }
}
