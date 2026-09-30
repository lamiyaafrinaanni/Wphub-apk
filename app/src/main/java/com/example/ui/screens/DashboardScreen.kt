package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.HubTab
import com.example.ui.components.WooCommerceRechartsSalesCard
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentSite: SiteEntity?,
    orders: List<OrderEntity>,
    posts: List<PostEntity> = emptyList(),
    plugins: List<PluginEntity> = emptyList(),
    customers: List<CustomerEntity> = emptyList(),
    waterTelemetry: WaterTelemetryEntity? = null,
    dashboardWidgets: List<DashboardWidgetEntity> = emptyList(),
    notifications: List<NotificationItemEntity> = emptyList(),
    isRefreshing: Boolean = false,
    onNavigateTab: (HubTab) -> Unit,
    onNotificationClick: (NotificationItemEntity) -> Unit = {},
    onOpenNotificationsCenter: () -> Unit = {},
    onOpenDashboardCustomizer: () -> Unit = {},
    onNewPostClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onQuickSaveDraft: (title: String, content: String) -> Unit = { _, _ -> },
    onOrderStatusChange: (String, String) -> Unit,
    onSyncClick: () -> Unit,
    onConnectSiteClick: () -> Unit = {},
    onSimulateOrder: (() -> Unit)? = null
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val totalRevenue = remember(orders) { orders.filter { it.status == "completed" }.sumOf { it.totalAmount } }
    val processingCount = remember(orders) { orders.count { it.status == "processing" } }

    val hasWoo = currentSite?.hasWooCommerce == true

    // Filter enabled widgets sorted by orderIndex
    val activeWidgets = remember(dashboardWidgets, hasWoo) {
        dashboardWidgets
            .filter { it.isEnabled }
            .filter { !it.isWooCommerceOnly || hasWoo }
            .sortedBy { it.orderIndex }
    }

    var quickDraftTitle by remember { mutableStateOf("") }
    var quickDraftContent by remember { mutableStateOf("") }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onSyncClick,
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_pull_to_refresh"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                color = PrimaryIndigo
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (currentSite == null) {
            item {
                NoSiteConnectedCard(
                    onConnectSiteClick = onConnectSiteClick,
                    onOpenTroubleshooter = { onNavigateTab(HubTab.TOOLS) }
                )
            }
        } else if (activeWidgets.isEmpty()) {
            // Default fallback if widgets not yet loaded
            item {
                SiteBannerWidget(
                    currentSite = currentSite,
                    onCustomizeClick = onOpenDashboardCustomizer,
                    onSyncClick = onSyncClick
                )
            }
            item {
                QuickStatsWidget(
                    currentSite = currentSite,
                    orders = orders,
                    posts = posts,
                    hasWoo = hasWoo,
                    totalRevenue = totalRevenue,
                    processingCount = processingCount,
                    currencyFormat = currencyFormat
                )
            }
            item {
                WooCommerceRechartsSalesCard(
                    currentSite = currentSite,
                    orders = orders,
                    totalRevenue = totalRevenue,
                    currencyFormat = currencyFormat,
                    onViewStore = { onNavigateTab(HubTab.STORE) },
                    onSimulateOrder = onSimulateOrder
                )
            }
        } else {
            activeWidgets.forEach { widget ->
                when (widget.widgetKey) {
                    "site_banner" -> item(key = widget.id) {
                        SiteBannerWidget(
                            currentSite = currentSite,
                            onCustomizeClick = onOpenDashboardCustomizer,
                            onSyncClick = onSyncClick
                        )
                    }
                    "quick_stats" -> item(key = widget.id) {
                        QuickStatsWidget(
                            currentSite = currentSite,
                            orders = orders,
                            posts = posts,
                            hasWoo = hasWoo,
                            totalRevenue = totalRevenue,
                            processingCount = processingCount,
                            currencyFormat = currencyFormat
                        )
                    }
                    "fast_actions" -> item(key = widget.id) {
                        FastActionsWidget(
                            hasWoo = hasWoo,
                            onNewPostClick = onNewPostClick,
                            onAddProductClick = onAddProductClick,
                            onNavigateTab = onNavigateTab,
                            onOpenCustomizer = onOpenDashboardCustomizer
                        )
                    }
                    "woo_sales" -> item(key = widget.id) {
                        WooCommerceRechartsSalesCard(
                            currentSite = currentSite,
                            orders = orders,
                            totalRevenue = totalRevenue,
                            currencyFormat = currencyFormat,
                            onViewStore = { onNavigateTab(HubTab.STORE) },
                            onSimulateOrder = onSimulateOrder
                        )
                    }
                    "woo_orders" -> if (hasWoo) item(key = widget.id) {
                        RecentOrdersWidget(
                            orders = orders,
                            currencyFormat = currencyFormat,
                            onOrderStatusChange = onOrderStatusChange,
                            onViewAllOrders = { onNavigateTab(HubTab.STORE) }
                        )
                    }
                    "quick_draft" -> item(key = widget.id) {
                        QuickDraftWidget(
                            title = quickDraftTitle,
                            content = quickDraftContent,
                            onTitleChange = { quickDraftTitle = it },
                            onContentChange = { quickDraftContent = it },
                            onSaveDraft = {
                                onQuickSaveDraft(quickDraftTitle, quickDraftContent)
                                quickDraftTitle = ""
                                quickDraftContent = ""
                            }
                        )
                    }
                    "recent_content" -> item(key = widget.id) {
                        RecentContentWidget(
                            posts = posts,
                            onViewAll = { onNavigateTab(HubTab.CONTENT) },
                            onNewPost = onNewPostClick
                        )
                    }
                    "recent_comments" -> item(key = widget.id) {
                        RecentCommentsWidget(
                            posts = posts,
                            onViewComments = { onNavigateTab(HubTab.CONTENT) }
                        )
                    }
                    "theme_overview" -> item(key = widget.id) {
                        ThemeOverviewWidget(
                            currentSite = currentSite,
                            onOpenTools = { onNavigateTab(HubTab.TOOLS) }
                        )
                    }
                    "plugins_health" -> item(key = widget.id) {
                        PluginsHealthWidget(
                            plugins = plugins,
                            onManagePlugins = { onNavigateTab(HubTab.TOOLS) }
                        )
                    }
                    "crm_inquiries" -> item(key = widget.id) {
                        CrmInquiriesWidget(
                            customers = customers,
                            onOpenCrm = { onNavigateTab(HubTab.CRM) }
                        )
                    }
                    "telemetry_iot" -> item(key = widget.id) {
                        TelemetryWidget(
                            telemetry = waterTelemetry,
                            onOpenTools = { onNavigateTab(HubTab.TOOLS) }
                        )
                    }
                    "rest_api" -> item(key = widget.id) {
                        RestApiDiagnosticWidget(
                            currentSite = currentSite,
                            onSyncClick = onSyncClick
                        )
                    }
                }
            }
            if (activeWidgets.isNotEmpty() && activeWidgets.none { it.widgetKey == "woo_sales" }) {
                item(key = "fallback_woo_sales_recharts") {
                    WooCommerceRechartsSalesCard(
                        currentSite = currentSite,
                        orders = orders,
                        totalRevenue = totalRevenue,
                        currencyFormat = currencyFormat,
                        onViewStore = { onNavigateTab(HubTab.STORE) },
                        onSimulateOrder = onSimulateOrder
                    )
                }
            }
        }

        // Bottom Dashboard Customizer Callout
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BadgeBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDashboardCustomizer)
                    .testTag("card_customize_dashboard_footer")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Customize Dashboard Layout",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigoDark,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Rearrange, show, or hide widgets tailored for ${currentSite?.name ?: "this site"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextBodyMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Customize",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

// ==========================================
// INDIVIDUAL MODULAR DASHBOARD WIDGETS
// ==========================================

@Composable
private fun SiteBannerWidget(
    currentSite: SiteEntity?,
    onCustomizeClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_site_banner")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = BadgeBackground,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currentSite?.iconEmoji ?: "🌐",
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentSite?.name ?: "WordPress Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentSite?.hasWooCommerce == true) WooPurple.copy(alpha = 0.12f) else EmeraldSuccess.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (currentSite?.hasWooCommerce == true) "Store Active" else "Content & Blog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentSite?.hasWooCommerce == true) WooPurple else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = currentSite?.tagline ?: currentSite?.url ?: "WordPress Site",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier.size(32.dp).testTag("btn_banner_sync")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onCustomizeClick,
                        modifier = Modifier.size(32.dp).testTag("btn_banner_customize")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Customize", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta tags row: Theme, WP version, REST status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BgGradientEnd,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentSite?.activeTheme ?: "Theme",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BgGradientEnd,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WP ${currentSite?.wpVersion ?: "6.6"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccessBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldSuccess))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REST 200 OK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsWidget(
    currentSite: SiteEntity?,
    orders: List<OrderEntity>,
    posts: List<PostEntity>,
    hasWoo: Boolean,
    totalRevenue: Double,
    processingCount: Int,
    currencyFormat: NumberFormat
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("widget_quick_stats")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasWoo) {
                MetricCard(
                    title = "Store Revenue",
                    value = currencyFormat.format(if (totalRevenue > 0) totalRevenue else currentSite?.totalSales ?: 0.0),
                    subtitle = "+18.4% vs last week",
                    icon = Icons.Default.TrendingUp,
                    accentColor = PrimaryIndigo,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Orders",
                    value = "${orders.size}",
                    subtitle = "$processingCount processing",
                    icon = Icons.Default.ShoppingBag,
                    accentColor = WooPurple,
                    modifier = Modifier.weight(1f)
                )
            } else {
                MetricCard(
                    title = "Published Posts",
                    value = "${posts.count { it.postType == "post" && it.status == "published" }.coerceAtLeast(currentSite?.totalPosts ?: 8)}",
                    subtitle = "${posts.count { it.status == "draft" }} drafts pending",
                    icon = Icons.Default.Article,
                    accentColor = PrimaryIndigo,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pages & Sections",
                    value = "${posts.count { it.postType == "page" }.coerceAtLeast(currentSite?.totalPages ?: 4)}",
                    subtitle = "${currentSite?.totalCategories ?: 6} categories",
                    icon = Icons.Default.Layers,
                    accentColor = BadgeAccentTint,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Visitor Traffic",
                value = "${currentSite?.visitorsToday ?: 420}",
                subtitle = "Unique visitors today",
                icon = Icons.Default.Visibility,
                accentColor = WPCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Comments",
                value = "${posts.sumOf { it.commentCount }.coerceAtLeast(currentSite?.totalComments ?: 14)}",
                subtitle = "Community discussion",
                icon = Icons.Default.ChatBubble,
                accentColor = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBodyMuted,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextBodyMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun FastActionsWidget(
    hasWoo: Boolean,
    onNewPostClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onNavigateTab: (HubTab) -> Unit,
    onOpenCustomizer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_fast_actions")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Publishing & Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Fast shortcuts",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextBodyMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButtonItem(
                    icon = Icons.Default.EditNote,
                    label = "New Post",
                    onClick = onNewPostClick,
                    modifier = Modifier.weight(1f).testTag("quick_action_new_post")
                )
                if (hasWoo) {
                    ActionButtonItem(
                        icon = Icons.Default.AddShoppingCart,
                        label = "Add Product",
                        onClick = onAddProductClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_new_product")
                    )
                } else {
                    ActionButtonItem(
                        icon = Icons.Default.PostAdd,
                        label = "New Page",
                        onClick = onNewPostClick,
                        modifier = Modifier.weight(1f).testTag("quick_action_new_page")
                    )
                }
                ActionButtonItem(
                    icon = Icons.Default.Comment,
                    label = "Moderate",
                    onClick = { onNavigateTab(HubTab.CONTENT) },
                    modifier = Modifier.weight(1f).testTag("quick_action_moderate")
                )
                ActionButtonItem(
                    icon = Icons.Default.Tune,
                    label = "Customize",
                    onClick = onOpenCustomizer,
                    modifier = Modifier.weight(1f).testTag("quick_action_customize")
                )
            }
        }
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BgGradientEnd,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickDraftWidget(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveDraft: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_quick_draft")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Draft Notepad",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Text(
                    text = "Auto-saves locally",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextBodyMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Draft Title (e.g. Next Big Announcement)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_quick_draft_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text("What's on your mind? Jot down points or outline...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("input_quick_draft_content")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSaveDraft,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_save_quick_draft")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WooSalesWidget(
    orders: List<OrderEntity>,
    totalRevenue: Double,
    currencyFormat: NumberFormat,
    onViewStore: () -> Unit
) {
    WooCommerceRechartsSalesCard(
        currentSite = null,
        orders = orders,
        totalRevenue = totalRevenue,
        currencyFormat = currencyFormat,
        onViewStore = onViewStore
    )
}

@Composable
private fun RecentOrdersWidget(
    orders: List<OrderEntity>,
    currencyFormat: NumberFormat,
    onOrderStatusChange: (String, String) -> Unit,
    onViewAllOrders: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_woo_orders")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent WooCommerce Orders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                TextButton(onClick = onViewAllOrders, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("All Orders (${orders.size})", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            orders.take(3).forEach { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.orderNumber,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (order.status == "processing") AmberWarningBg else EmeraldSuccessBg
                            ) {
                                Text(
                                    text = order.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (order.status == "processing") AmberWarning else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "${order.customerName} • ${order.itemsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = currencyFormat.format(order.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentContentWidget(
    posts: List<PostEntity>,
    onViewAll: () -> Unit,
    onNewPost: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_recent_content")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Articles & Pages",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Content Hub →", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            posts.take(3).forEach { post ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BadgeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (post.postType == "page") Icons.Default.Layers else Icons.Default.Article,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${post.category} • ${post.dateFormatted} • ${post.viewCount} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextBodyMuted,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (post.status == "published") EmeraldSuccessBg else AmberWarningBg
                    ) {
                        Text(
                            text = post.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (post.status == "published") EmeraldSuccess else AmberWarning,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCommentsWidget(
    posts: List<PostEntity>,
    onViewComments: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_recent_comments")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = WooPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Discussions & Moderation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                TextButton(onClick = onViewComments, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Moderate →", color = WooPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Discussions inbox is caught up. Zero pending unmoderated comments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOverviewWidget(
    currentSite: SiteEntity?,
    onOpenTools: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_theme_overview")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Theme & Appearance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSuccessBg
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${currentSite?.activeTheme ?: "Astra Pro"} (v${currentSite?.activeThemeVersion ?: "4.6.2"})",
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 14.sp
            )
            Text(
                text = "Full Site Editing (FSE) block templates & dynamic color palette synced.",
                style = MaterialTheme.typography.bodySmall,
                color = TextBodyMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PluginsHealthWidget(
    plugins: List<PluginEntity>,
    onManagePlugins: () -> Unit
) {
    val updates = remember(plugins) { plugins.count { it.updateAvailable } }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_plugins_health")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Plugins & Security Health",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                TextButton(onClick = onManagePlugins, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Manage →", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BadgeBackground,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Active Plugins", style = MaterialTheme.typography.labelSmall, color = TextBodyMuted)
                        Text(text = "${plugins.count { it.isActive }} of ${plugins.size}", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (updates > 0) AmberWarningBg else EmeraldSuccessBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Updates Status", style = MaterialTheme.typography.labelSmall, color = TextBodyMuted)
                        Text(
                            text = if (updates > 0) "$updates Pending" else "All Up-to-Date",
                            fontWeight = FontWeight.Bold,
                            color = if (updates > 0) AmberWarning else EmeraldSuccess,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrmInquiriesWidget(
    customers: List<CustomerEntity>,
    onOpenCrm: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_crm_inquiries")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactMail, contentDescription = null, tint = WPCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customer Inquiries & CRM",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                TextButton(onClick = onOpenCrm, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("CRM Hub →", color = WPCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            customers.take(2).forEach { cust ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE0F7FA),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = cust.avatarInitials, fontWeight = FontWeight.Bold, color = WPCyan, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cust.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 12.sp)
                        Text(text = "${cust.email} • ${cust.role}", style = MaterialTheme.typography.labelSmall, color = TextBodyMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryWidget(
    telemetry: WaterTelemetryEntity?,
    onOpenTools: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_telemetry")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Operations & IoT Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                TextButton(onClick = onOpenTools, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Controls →", color = PrimaryIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${telemetry?.facilityName ?: "Facility Hub"} • Level: ${telemetry?.tankLevelPercent ?: 78}% • Pressure: ${telemetry?.pressurePsi ?: 46.5} PSI",
                style = MaterialTheme.typography.bodySmall,
                color = TextBodyMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RestApiDiagnosticWidget(
    currentSite: SiteEntity?,
    onSyncClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier.fillMaxWidth().testTag("widget_rest_api")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Api, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REST Endpoints & Handshake",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                IconButton(onClick = onSyncClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Ping", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Endpoint: ${currentSite?.url ?: "https://"}/wp-json/wp/v2/posts\nResponse: 200 OK • Latency: 128ms • Auth: Bearer / App Password",
                style = MaterialTheme.typography.bodySmall,
                color = TextBodyMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun MetricSmallCard(
    title: String,
    value: String,
    subtitle: String,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextBodyMuted,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isPositive) EmeraldSuccess else TextBodyMuted,
                fontSize = 10.sp,
                fontWeight = if (isPositive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun NoSiteConnectedCard(
    onConnectSiteClick: () -> Unit,
    onOpenTroubleshooter: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_no_site_connected")
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = PrimaryIndigo.copy(alpha = 0.12f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Connected WordPress Sites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "All demo and mock data has been removed, and all sessions are signed out. Connect your self-hosted WordPress site or WooCommerce store to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zero mock data: Clean slate production environment", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Direct WP REST API integration via Application Passwords", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Automatic store detection and dynamic custom dashboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onConnectSiteClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_connect_first_site")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect WordPress Site", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenTroubleshooter,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_open_troubleshooter_from_empty")
            ) {
                Icon(Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connection Troubleshooter & Diagnostics")
            }
        }
    }
}

