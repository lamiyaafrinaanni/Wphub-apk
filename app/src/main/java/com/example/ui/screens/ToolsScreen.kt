package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationSettingsEntity
import com.example.data.local.PluginEntity
import com.example.data.local.SiteEntity
import com.example.data.local.WaterTelemetryEntity
import com.example.data.remote.WordPressApiLogEntry
import com.example.data.remote.WordPressLogStore
import com.example.data.remote.RestTestResult
import kotlinx.coroutines.launch
import com.example.ui.components.BiometricCredentialsVaultCard
import com.example.ui.components.NotificationSettingsTab
import com.example.ui.components.SiteSelector
import com.example.ui.components.WordPressConnectTroubleshooterDialog
import com.example.ui.theme.*

@Composable
fun ToolsScreen(
    plugins: List<PluginEntity>,
    waterTelemetry: WaterTelemetryEntity?,
    notificationSettings: NotificationSettingsEntity? = null,
    currentSite: SiteEntity? = null,
    allSites: List<SiteEntity> = emptyList(),
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSelectSite: (String) -> Unit = {},
    onOpenLoginDialog: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSignOutAll: () -> Unit = {},
    onSignOutAllAndClearDemoData: () -> Unit = {},
    onUpdateNotificationSettings: (NotificationSettingsEntity) -> Unit = {},
    onTriggerTestOrderAlert: () -> Unit = {},
    onTriggerTestCommentAlert: () -> Unit = {},
    onTriggerTestLowStockAlert: () -> Unit = {},
    onTriggerTestInquiryAlert: () -> Unit = {},
    onTogglePlugin: (String, Boolean) -> Unit,
    onUpdatePlugin: (String, String) -> Unit,
    onToggleWaterPump: () -> Unit,
    onToggleWaterAutoMode: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var selectedToolsSubTab by remember { mutableIntStateOf(0) }
    var showTroubleshooter by remember { mutableStateOf(false) }
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RoseError) },
            title = { Text("Sign Out & Remove Demo Data") },
            text = {
                Text("This will sign out of all WordPress sites and remove all demo and mock data from the local database. You will return to the dedicated WordPress login screen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmDialog = false
                        onSignOutAllAndClearDemoData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Remove All & Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTroubleshooter) {
        WordPressConnectTroubleshooterDialog(
            initialUrl = currentSite?.url ?: "https://",
            initialUsername = currentSite?.username ?: "",
            onDismiss = { showTroubleshooter = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen")
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedToolsSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            Tab(
                selected = selectedToolsSubTab == 0,
                onClick = { selectedToolsSubTab = 0 },
                text = { Text("Plugins (${plugins.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_plugins")
            )
            Tab(
                selected = selectedToolsSubTab == 1,
                onClick = { selectedToolsSubTab = 1 },
                text = { Text("Push & Alerts", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_push_alerts")
            )
            Tab(
                selected = selectedToolsSubTab == 2,
                onClick = { selectedToolsSubTab = 2 },
                text = { Text("Theme & Display", fontWeight = FontWeight.Bold) },
                icon = {
                    Icon(
                        imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("tab_theme_display")
            )
            Tab(
                selected = selectedToolsSubTab == 3,
                onClick = { selectedToolsSubTab = 3 },
                text = { Text("WP Account", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_wp_account")
            )
            Tab(
                selected = selectedToolsSubTab == 4,
                onClick = { selectedToolsSubTab = 4 },
                text = { Text("Water & Utility", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_water_utility")
            )
            Tab(
                selected = selectedToolsSubTab == 5,
                onClick = { selectedToolsSubTab = 5 },
                text = { Text("REST Endpoints", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Api, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_rest_api")
            )
        }

        when (selectedToolsSubTab) {
            0 -> {
                // Plugin Management
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val updateAvailableCount = plugins.count { it.updateAvailable }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "WordPress Plugin Administration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remotely check updates, activate/deactivate modules, and trigger hot-updates over REST API.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (updateAvailableCount > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberWarningBg
                                    ) {
                                        Text(
                                            text = "$updateAvailableCount plugin updates available",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(plugins, key = { it.id }) { plugin ->
                        PluginItemCard(
                            plugin = plugin,
                            onToggle = { onTogglePlugin(plugin.id, plugin.isActive) },
                            onUpdate = { onUpdatePlugin(plugin.id, plugin.newVersion ?: "latest") }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            1 -> {
                // Push & Alerts Configuration
                Column(modifier = Modifier.fillMaxSize()) {
                    // Test Trigger Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔔 NOTIFICATION SIMULATOR & TEST HARNESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dispatch mock alerts to test deep-linking navigation to Orders, Comments, or Products.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onTriggerTestOrderAlert,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("+ Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onTriggerTestCommentAlert,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("+ Comment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onTriggerTestLowStockAlert,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("+ Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onTriggerTestInquiryAlert,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text("+ Inquiry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    NotificationSettingsTab(
                        settings = notificationSettings ?: NotificationSettingsEntity(siteId = "default"),
                        onSaveSettings = onUpdateNotificationSettings
                    )
                }
            }
            2 -> {
                // Theme & Display Mode
                ThemeSettingsTab(
                    currentThemeMode = themeMode,
                    onSelectThemeMode = onSetThemeMode
                )
            }
            3 -> {
                // WordPress Account & Authentication Management
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryIndigo),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (currentSite?.username?.take(1) ?: "A").uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = currentSite?.userDisplayName ?: "Site Administrator",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${currentSite?.username ?: "admin"} • ${currentSite?.userRole ?: "Administrator"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (currentSite?.isAuthenticated == true) EmeraldSuccessBg else RoseErrorBg
                                    ) {
                                        Text(
                                            text = if (currentSite?.isAuthenticated == true) "Active Session" else "Logged Out",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (currentSite?.isAuthenticated == true) EmeraldSuccess else RoseError,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onOpenLoginDialog,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_switch_wp_account")
                                    ) {
                                        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Switch User")
                                    }

                                    OutlinedButton(
                                        onClick = onLogout,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                                        modifier = Modifier.weight(1f).testTag("btn_wp_logout")
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sign Out")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onSignOutAll,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_sign_out_all")
                                    ) {
                                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sign Out All")
                                    }

                                    Button(
                                        onClick = { showPurgeConfirmDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                        modifier = Modifier.weight(1f).testTag("btn_purge_demo_data")
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Purge Demo Data", color = Color.White, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                FilledTonalButton(
                                    onClick = { showTroubleshooter = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_troubleshoot_connection")
                                ) {
                                    Icon(Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connection Troubleshooter & Fixes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        BiometricCredentialsVaultCard(
                            currentSite = currentSite,
                            onShowMessage = onShowMessage
                        )
                    }

                    item {
                        SiteSelector(
                            sites = allSites,
                            currentSite = currentSite,
                            onSelectSite = onSelectSite,
                            onAddNewSiteClick = onOpenLoginDialog,
                            onOpenLoginClick = onOpenLoginDialog
                        )
                    }
                }
            }
            4 -> {
                // Water & Utility Telemetry
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = waterTelemetry?.facilityName ?: "Main Facility Reservoir",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "IoT Telemetry Station #04",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (waterTelemetry?.pumpRunning == true) EmeraldSuccessBg else AmberWarningBg
                                    ) {
                                        Text(
                                            text = if (waterTelemetry?.pumpRunning == true) "PUMP ACTIVE" else "PUMP IDLE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (waterTelemetry?.pumpRunning == true) EmeraldSuccess else AmberWarning,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tank Level Progress
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Reservoir Tank Level",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${waterTelemetry?.tankLevelPercent ?: 78}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (waterTelemetry?.tankLevelPercent ?: 78) / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = PrimaryIndigo,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MetricSmallCard(
                                        title = "Water Pressure",
                                        value = "${waterTelemetry?.pressurePsi ?: 46.5} PSI",
                                        subtitle = "Optimal Range (40-55)",
                                        isPositive = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricSmallCard(
                                        title = "Live Flow Rate",
                                        value = "${waterTelemetry?.flowRateLpm ?: 12.4} LPM",
                                        subtitle = "Steady Circulation",
                                        isPositive = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Controls Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Telemetric Controls & Overrides",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Main Induction Pump",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Control facility intake valve and motor",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = waterTelemetry?.pumpRunning == true,
                                        onCheckedChange = { onToggleWaterPump() },
                                        modifier = Modifier.testTag("switch_water_pump")
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "IoT Autonomous Mode",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Auto-regulates pressure and refills threshold",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = waterTelemetry?.autoMode == true,
                                        onCheckedChange = { onToggleWaterAutoMode() },
                                        modifier = Modifier.testTag("switch_water_auto_mode")
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { onShowMessage("Triggered automated reservoir backwash cycle") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Flush Lines")
                                    }
                                    Button(
                                        onClick = { onShowMessage("Telemetry report exported to PDF") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Telemetry Log")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            5 -> {
                // REST API Endpoints & Live Log Inspector
                val apiLogs by WordPressLogStore.logs.collectAsStateWithLifecycle()
                var selectedLogFilter by remember { mutableIntStateOf(0) } // 0: All, 1: 2xx, 2: 4xx, 3: 5xx / Errors
                val clipboardManager = LocalClipboardManager.current

                val filteredLogs = remember(apiLogs, selectedLogFilter) {
                    when (selectedLogFilter) {
                        1 -> apiLogs.filter { it.statusCode in 200..299 }
                        2 -> apiLogs.filter { it.statusCode in 400..499 }
                        3 -> apiLogs.filter { it.statusCode >= 500 || it.statusCode == 0 }
                        else -> apiLogs
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                val restClient = remember { com.example.data.remote.WordPressRestClient() }
                var isTestingConnection by remember { mutableStateOf(false) }
                var restTestResult by remember { mutableStateOf<com.example.data.remote.RestTestResult?>(null) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Troubleshoot,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "REST Connection Diagnostics",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Test SSL, 401 Auth Headers, and App Passwords",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { showTroubleshooter = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Diagnose", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Live API Structure Test Tool
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NetworkCheck,
                                            contentDescription = null,
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "REST API Structure Test",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val url = currentSite?.url
                                            if (url != null) {
                                                isTestingConnection = true
                                                restTestResult = null
                                                coroutineScope.launch {
                                                    restTestResult = restClient.runSimpleRestConnectionTest(url)
                                                    isTestingConnection = false
                                                }
                                            } else {
                                                onShowMessage("No active WordPress site connected to test.")
                                            }
                                        },
                                        enabled = !isTestingConnection && currentSite != null,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Testing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Run Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Performs a direct unauthenticated GET /wp-json/ query to verify if the server returns a valid WordPress API schema (namespaces & routes) before attempting WooCommerce sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val result = restTestResult
                                if (result != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    when (result) {
                                        is com.example.data.remote.RestTestResult.Success -> {
                                            Surface(
                                                color = EmeraldSuccessBg,
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Test Passed (Schema Validated)", fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 13.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "• Site Name: ${result.siteName}\n" +
                                                               "• Namespaces: ${result.namespacesCount} registered endpoints\n" +
                                                               "• Routes: ${result.routesCount} available resources\n\n" +
                                                               "The REST API is responsive and structure conforms to the WordPress core API specification.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF065F46),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        is com.example.data.remote.RestTestResult.Error -> {
                                            Surface(
                                                color = RoseErrorBg,
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Error, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Test Failed (Schema Invalid)", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 13.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = result.message,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF991B1B),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Log Stream Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Live OkHttp API Logs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (apiLogs.any { !it.isSuccess }) AmberWarningBg else BadgeBackground
                                    ) {
                                        Text(
                                            text = "${apiLogs.size} calls",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (apiLogs.any { !it.isSuccess }) AmberWarning else PrimaryIndigo,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Real-time outgoing requests, status codes, headers & body inspector",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (apiLogs.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        WordPressLogStore.clearLogs()
                                        onShowMessage("API Log buffer cleared")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedLogFilter == 0,
                                onClick = { selectedLogFilter = 0 },
                                label = { Text("All (${apiLogs.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedLogFilter == 1,
                                onClick = { selectedLogFilter = 1 },
                                label = { Text("2xx OK (${apiLogs.count { it.statusCode in 200..299 }})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedLogFilter == 2,
                                onClick = { selectedLogFilter = 2 },
                                label = { Text("4xx Auth (${apiLogs.count { it.statusCode in 400..499 }})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = selectedLogFilter == 3,
                                onClick = { selectedLogFilter = 3 },
                                label = { Text("Errors (${apiLogs.count { it.statusCode >= 500 || it.statusCode == 0 }})", fontSize = 11.sp) }
                            )
                        }
                    }

                    if (filteredLogs.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (apiLogs.isEmpty()) "No REST API requests recorded yet" else "No logs match the selected filter",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pull down on Dashboard or switch sites to trigger live REST calls.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredLogs, key = { it.id }) { logEntry ->
                            RestApiLogCard(
                                log = logEntry,
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    onShowMessage("Copied REST API log details to clipboard")
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Standard WordPress REST Route Reference",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standard WP-JSON route schema supported by WPMobile Hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val endpoints = listOf(
                        Triple("GET /wp-json/wp/v2/users/me", "Current User & Roles Authentication", "Basic Auth"),
                        Triple("GET /wp-json/wp/v2/posts", "Content Posts Publishing", "Read / Write"),
                        Triple("GET /wp-json/wp/v2/pages", "Static Pages Directory", "Read / Write"),
                        Triple("GET /wp-json/wp/v2/plugins", "Active Plugins & Updates", "Admin Required"),
                        Triple("GET /wp-json/wc/v3/products", "WooCommerce Catalog & Stock", "WooCommerce"),
                        Triple("GET /wp-json/wc/v3/orders", "WooCommerce Live Order Stream", "WooCommerce")
                    )

                    items(endpoints) { (route, desc, status) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = route, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSuccessBg
                                ) {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        }
    }
}

@Composable
fun ThemeSettingsTab(
    currentThemeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("theme_settings_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentThemeMode) {
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Appearance & Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Active: ${currentThemeMode.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryIndigoDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "SELECT THEME MODE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                letterSpacing = 1.sp
            )
        }

        // 3 Theme Options Cards
        item {
            ThemeOptionCard(
                title = "Light Mode",
                subtitle = "Crisp #F8FAFC background, #0F172A dark typography, #4F46E5 primary branding, #EEF2FF badge tint.",
                icon = Icons.Default.LightMode,
                isSelected = currentThemeMode == AppThemeMode.LIGHT,
                previewBg = Color(0xFFF8FAFC),
                previewCard = Color.White,
                previewText = Color(0xFF0F172A),
                previewAccent = Color(0xFF4F46E5),
                onClick = { onSelectThemeMode(AppThemeMode.LIGHT) },
                testTag = "theme_option_light"
            )
        }

        item {
            ThemeOptionCard(
                title = "Dark Mode",
                subtitle = "Deep #0F172A / #0A0F1D Slate canvas, #1E293B elevated surfaces, #6366F1 vibrant contrast.",
                icon = Icons.Default.DarkMode,
                isSelected = currentThemeMode == AppThemeMode.DARK,
                previewBg = Color(0xFF0A0F1D),
                previewCard = Color(0xFF1E293B),
                previewText = Color(0xFFF8FAFC),
                previewAccent = Color(0xFF6366F1),
                onClick = { onSelectThemeMode(AppThemeMode.DARK) },
                testTag = "theme_option_dark"
            )
        }

        item {
            ThemeOptionCard(
                title = "System Default",
                subtitle = "Synchronize dynamically with your Android system appearance schedule.",
                icon = Icons.Default.BrightnessAuto,
                isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                previewBg = Color(0xFFE2E8F0),
                previewCard = Color(0xFF334155),
                previewText = Color(0xFF0F172A),
                previewAccent = Color(0xFF4F46E5),
                onClick = { onSelectThemeMode(AppThemeMode.SYSTEM) },
                testTag = "theme_option_system"
            )
        }

        // Live Brand Palette Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "WordPress Design System Spec",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "WCAG 2.1 AA compliant contrast ratios across Light & Dark modes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorSwatch(name = "Primary", hex = "#4F46E5", color = PrimaryIndigo, modifier = Modifier.weight(1f))
                        ColorSwatch(name = "Dark Text", hex = "#0F172A", color = TextDark, modifier = Modifier.weight(1f))
                        ColorSwatch(name = "Muted", hex = "#475569", color = TextBodyMuted, modifier = Modifier.weight(1f))
                        ColorSwatch(name = "Badge Tint", hex = "#6366F1", color = BadgeAccentTint, modifier = Modifier.weight(1f))
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
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    previewBg: Color,
    previewCard: Color,
    previewText: Color,
    previewAccent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) PrimaryIndigo else BorderSlate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .background(if (isSelected) BadgeBackground else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Miniature Preview
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = previewBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = previewCard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(previewAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Card",
                                color = previewText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = previewAccent
                    ) {
                        Text(
                            text = "Button",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    hex: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = hex, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PluginItemCard(
    plugin: PluginEntity,
    onToggle: () -> Unit,
    onUpdate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("plugin_card_${plugin.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version ${plugin.version} • By ${plugin.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = plugin.isActive,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("switch_plugin_${plugin.id}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (plugin.updateAvailable && plugin.newVersion != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarningBg.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New v${plugin.newVersion} available!",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onUpdate,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("update_plugin_button_${plugin.id}")
                        ) {
                            Text("Update Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestApiLogCard(
    log: WordPressApiLogEntry,
    onCopy: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(!log.isSuccess) } // Auto-expand failed requests

    val statusColor = when {
        log.statusCode in 200..299 -> EmeraldSuccess
        log.statusCode in 400..499 -> AmberWarning
        else -> RoseError
    }

    val statusBg = when {
        log.statusCode in 200..299 -> EmeraldSuccessBg
        log.statusCode in 400..499 -> AmberWarningBg
        else -> RoseError.copy(alpha = 0.15f)
    }

    val methodColor = when (log.method.uppercase()) {
        "GET" -> PrimaryIndigo
        "POST" -> EmeraldSuccess
        "PUT", "PATCH" -> WooPurple
        "DELETE" -> RoseError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!log.isSuccess) statusColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Summary Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Method Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = methodColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = log.method,
                            style = MaterialTheme.typography.labelSmall,
                            color = methodColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = if (log.statusCode > 0) "${log.statusCode} ${log.statusMessage}" else "Error",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Duration
                    Text(
                        text = "${log.durationMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // URL path
            Text(
                text = log.url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                maxLines = if (expanded) 5 else 1
            )

            // Expanded Details View
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Diagnostic Advice Card if Error
                    if (!log.diagnosticAdvice.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Root Cause Diagnostic Advice",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.diagnosticAdvice,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Request Headers Section
                    Text(
                        text = "REQUEST HEADERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (log.requestHeaders.isEmpty()) {
                                Text("No custom request headers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                log.requestHeaders.forEach { (k, v) ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = "$k: ", fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = v, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Request Body Section (if present)
                    if (!log.requestBody.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "REQUEST PAYLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.requestBody,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Response Headers Section
                    Text(
                        text = "RESPONSE HEADERS (${log.responseHeaders.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (log.responseHeaders.isEmpty()) {
                                Text("No response headers captured", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                log.responseHeaders.forEach { (k, v) ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = "$k: ", fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = v, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Response Body Preview
                    if (!log.responseBodyPreview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "RESPONSE BODY PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log.responseBodyPreview.take(800) + if (log.responseBodyPreview.length > 800) "... [truncated]" else "",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Copy Log Button
                    OutlinedButton(
                        onClick = {
                            val report = buildString {
                                appendLine("=== WordPress REST API Request Log ===")
                                appendLine("Method: ${log.method}")
                                appendLine("URL: ${log.url}")
                                appendLine("Status: ${log.statusCode} ${log.statusMessage} (${log.durationMs}ms)")
                                appendLine("Time: ${log.timeFormatted}")
                                appendLine("\n--- Request Headers ---")
                                log.requestHeaders.forEach { (k, v) -> appendLine("$k: $v") }
                                if (!log.requestBody.isNullOrBlank()) {
                                    appendLine("\n--- Request Body ---")
                                    appendLine(log.requestBody)
                                }
                                appendLine("\n--- Response Headers ---")
                                log.responseHeaders.forEach { (k, v) -> appendLine("$k: $v") }
                                if (!log.responseBodyPreview.isNullOrBlank()) {
                                    appendLine("\n--- Response Body ---")
                                    appendLine(log.responseBodyPreview)
                                }
                                if (!log.diagnosticAdvice.isNullOrBlank()) {
                                    appendLine("\n--- Diagnostic Advice ---")
                                    appendLine(log.diagnosticAdvice)
                                }
                            }
                            onCopy(report)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Full Request & Response Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
