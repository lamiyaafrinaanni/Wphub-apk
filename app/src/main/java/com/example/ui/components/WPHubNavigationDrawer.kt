package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SiteEntity
import com.example.ui.HubTab
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WPHubNavigationDrawerContent(
    currentSite: SiteEntity?,
    allSites: List<SiteEntity>,
    selectedTab: HubTab,
    hasWooCommerce: Boolean,
    unreadNotificationCount: Int,
    themeMode: AppThemeMode,
    isSyncing: Boolean,
    onTabSelected: (HubTab) -> Unit,
    onSiteSelected: (String) -> Unit,
    onAddSiteClick: () -> Unit,
    onOpenGutenbergEditor: () -> Unit,
    onOpenBiometricVault: () -> Unit,
    onToggleTheme: () -> Unit,
    onSyncClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    var isSiteListExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("wp_navigation_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Drawer Header with Current Active WordPress Site Info
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Site Icon Emoji Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { isSiteListExpanded = !isSiteListExpanded },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentSite?.iconEmoji ?: "🌐",
                                fontSize = 24.sp
                            )
                        }

                        // Close Drawer Button
                        IconButton(onClick = onCloseDrawer) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSiteListExpanded = !isSiteListExpanded }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSite?.name ?: "No Site Connected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentSite?.sslEnabled == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "SSL Verified",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = currentSite?.url ?: "Tap to add WordPress site",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { isSiteListExpanded = !isSiteListExpanded }) {
                            Icon(
                                imageVector = if (isSiteListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Switch Site List",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (currentSite != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentSite.userDisplayName.ifBlank { currentSite.username },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = currentSite.userRole,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Multi-site Dropdown Switcher Section
            AnimatedVisibility(
                visible = isSiteListExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Registered WordPress Sites (${allSites.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = {
                                    onAddSiteClick()
                                    onCloseDrawer()
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Site", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        allSites.forEach { site ->
                            val isSelected = site.id == currentSite?.id
                            Surface(
                                onClick = {
                                    onSiteSelected(site.id)
                                    isSiteListExpanded = false
                                    onCloseDrawer()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("drawer_site_item_${site.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(site.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = site.url.removePrefix("https://").removePrefix("http://"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active Site",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Hub Menu Items
            PaddingValues(horizontal = 12.dp).let { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    Text(
                        text = "WordPress Navigation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Dashboard Overview", fontWeight = FontWeight.SemiBold) },
                        selected = selectedTab == HubTab.DASHBOARD,
                        onClick = {
                            onTabSelected(HubTab.DASHBOARD)
                            onCloseDrawer()
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag("drawer_tab_dashboard")
                    )

                    if (hasWooCommerce) {
                        NavigationDrawerItem(
                            label = { Text("WooCommerce Store", fontWeight = FontWeight.SemiBold) },
                            selected = selectedTab == HubTab.STORE,
                            onClick = {
                                onTabSelected(HubTab.STORE)
                                onCloseDrawer()
                            },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                            badge = {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Woo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("drawer_tab_store")
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("Content & Articles", fontWeight = FontWeight.SemiBold) },
                        selected = selectedTab == HubTab.CONTENT,
                        onClick = {
                            onTabSelected(HubTab.CONTENT)
                            onCloseDrawer()
                        },
                        icon = { Icon(Icons.Default.Article, contentDescription = null) },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag("drawer_tab_content")
                    )

                    NavigationDrawerItem(
                        label = { Text("CRM & Customers", fontWeight = FontWeight.SemiBold) },
                        selected = selectedTab == HubTab.CRM,
                        onClick = {
                            onTabSelected(HubTab.CRM)
                            onCloseDrawer()
                        },
                        icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag("drawer_tab_crm")
                    )

                    NavigationDrawerItem(
                        label = { Text("Admin Tools & Security", fontWeight = FontWeight.SemiBold) },
                        selected = selectedTab == HubTab.TOOLS,
                        onClick = {
                            onTabSelected(HubTab.TOOLS)
                            onCloseDrawer()
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .testTag("drawer_tab_tools")
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Quick Actions & Creator Tools
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = "Quick Publishing & Security",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Surface(
                    onClick = {
                        onOpenGutenbergEditor()
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("drawer_open_gutenberg_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gutenberg Block Editor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Publish posts, pages, & blocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onOpenBiometricVault()
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("drawer_open_biometric_vault_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Passwords Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Protected Application Passwords",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Footer Status & Quick Preferences
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
                                val themeIcon = when (themeMode) {
                                    AppThemeMode.LIGHT -> Icons.Default.DarkMode
                                    AppThemeMode.DARK -> Icons.Default.LightMode
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                }
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Theme: ${themeMode.title}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = onSyncClick, enabled = !isSyncing, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "WPMobile Hub v2.8 • WP REST v2 Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
