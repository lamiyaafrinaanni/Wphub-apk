package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.AddSiteDialog
import com.example.ui.components.BlockEditorDialog
import com.example.ui.components.DashboardCustomizerSheet
import com.example.ui.components.GutenbergEditor
import com.example.ui.components.GutenbergVisualEditorSheet
import com.example.ui.components.NotificationsCenterSheet
import com.example.ui.components.SiteSwitcherSheet
import com.example.ui.components.WPHubNavigationDrawerContent
import com.example.ui.components.WPHubTopBar
import com.example.ui.components.WordPressLoginDialog
import com.example.ui.screens.*

enum class AppScreen {
    SPLASH,
    CONNECT,
    LOGIN,
    MAIN_HUB
}

data class NavItem(
    val tab: HubTab,
    val title: String,
    val icon: ImageVector
)

@Composable
fun WPHubApp(viewModel: WPHubViewModel) {
    val currentSite by viewModel.currentSite.collectAsStateWithLifecycle()
    val allSites by viewModel.allSites.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val hasWooCommerce by viewModel.hasWooCommerce.collectAsStateWithLifecycle()
    val dashboardWidgets by viewModel.dashboardWidgets.collectAsStateWithLifecycle()

    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val waterTelemetry by viewModel.waterTelemetry.collectAsStateWithLifecycle()

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val targetOrderId by viewModel.targetOrderId.collectAsStateWithLifecycle()
    val targetPostId by viewModel.targetPostId.collectAsStateWithLifecycle()
    val targetProductId by viewModel.targetProductId.collectAsStateWithLifecycle()

    val orderFilter by viewModel.orderFilter.collectAsStateWithLifecycle()
    val contentFilter by viewModel.contentFilter.collectAsStateWithLifecycle()

    var appScreen by remember { mutableStateOf(AppScreen.SPLASH) }

    var showSiteSwitcherSheet by remember { mutableStateOf(false) }
    var showNotificationsCenterSheet by remember { mutableStateOf(false) }
    var showDashboardCustomizerSheet by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showGlobalEditorDialog by remember { mutableStateOf(false) }
    var showGlobalAddProductDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Safety redirection: if switching to non-WooCommerce site while on STORE tab, navigate to DASHBOARD
    LaunchedEffect(hasWooCommerce, selectedTab) {
        if (!hasWooCommerce && selectedTab == HubTab.STORE) {
            viewModel.selectTab(HubTab.DASHBOARD)
        }
    }

    // Auto logout redirection: if current site is logged out or null, redirect to step 1 (CONNECT) screen
    LaunchedEffect(currentSite) {
        if (currentSite == null && appScreen == AppScreen.MAIN_HUB) {
            appScreen = AppScreen.CONNECT
        }
    }

    when (appScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                onAnimationComplete = {
                    if (currentSite != null && currentSite?.isAuthenticated == true) {
                        appScreen = AppScreen.MAIN_HUB
                    } else {
                        appScreen = AppScreen.CONNECT
                    }
                }
            )
        }

        AppScreen.CONNECT -> {
            WordPressConnectionScreen(
                viewModel = viewModel,
                onConnectedSuccess = { site ->
                    viewModel.selectTab(HubTab.DASHBOARD)
                    appScreen = AppScreen.MAIN_HUB
                },
                onDismiss = if (currentSite?.isAuthenticated == true) {
                    { appScreen = AppScreen.MAIN_HUB }
                } else null
            )
        }

        AppScreen.LOGIN -> {
            WordPressConnectionScreen(
                viewModel = viewModel,
                onConnectedSuccess = { site ->
                    viewModel.selectTab(HubTab.DASHBOARD)
                    appScreen = AppScreen.MAIN_HUB
                },
                onDismiss = if (currentSite?.isAuthenticated == true) {
                    { appScreen = AppScreen.MAIN_HUB }
                } else null
            )
        }

        AppScreen.MAIN_HUB -> {
            // Navigation items adapt dynamically based on whether WooCommerce is installed!
            val navItems = remember(hasWooCommerce) {
                buildList {
                    add(NavItem(HubTab.DASHBOARD, "Overview", Icons.Default.Dashboard))
                    if (hasWooCommerce) {
                        add(NavItem(HubTab.STORE, "Store", Icons.Default.Storefront))
                    }
                    add(NavItem(HubTab.CONTENT, "Content", Icons.Default.Article))
                    add(NavItem(HubTab.CRM, "CRM", Icons.Default.Groups))
                    add(NavItem(HubTab.TOOLS, "Tools", Icons.Default.Build))
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    WPHubNavigationDrawerContent(
                        currentSite = currentSite,
                        allSites = allSites,
                        selectedTab = selectedTab,
                        hasWooCommerce = hasWooCommerce,
                        unreadNotificationCount = unreadNotificationCount,
                        themeMode = themeMode,
                        isSyncing = isSyncing,
                        onTabSelected = { viewModel.selectTab(it) },
                        onSiteSelected = { viewModel.switchSite(it) },
                        onAddSiteClick = { showAddSiteDialog = true },
                        onOpenGutenbergEditor = { showGlobalEditorDialog = true },
                        onOpenBiometricVault = { viewModel.selectTab(HubTab.TOOLS) },
                        onToggleTheme = { viewModel.toggleThemeMode() },
                        onSyncClick = { viewModel.syncCurrentSite() },
                        onCloseDrawer = {
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        WPHubTopBar(
                            currentSite = currentSite,
                            isSyncing = isSyncing,
                            unreadNotificationCount = unreadNotificationCount,
                            themeMode = themeMode,
                            onOpenDrawerClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onSiteSelectorClick = { showSiteSwitcherSheet = true },
                            onSyncClick = { viewModel.syncCurrentSite() },
                            onToggleTheme = { viewModel.toggleThemeMode() },
                            onNotificationBellClick = { showNotificationsCenterSheet = true },
                            onUserLoginClick = { appScreen = AppScreen.CONNECT }
                        )
                    },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar"),
                        tonalElevation = 6.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = selectedTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        HubTab.DASHBOARD -> {
                            DashboardScreen(
                                currentSite = currentSite,
                                orders = orders,
                                posts = posts,
                                plugins = plugins,
                                customers = customers,
                                waterTelemetry = waterTelemetry,
                                dashboardWidgets = dashboardWidgets,
                                notifications = notifications,
                                isRefreshing = isSyncing,
                                onNavigateTab = { viewModel.selectTab(it) },
                                onNotificationClick = { notif ->
                                    viewModel.onNotificationClicked(notif)
                                },
                                onOpenNotificationsCenter = { showNotificationsCenterSheet = true },
                                onOpenDashboardCustomizer = { showDashboardCustomizerSheet = true },
                                onNewPostClick = { showGlobalEditorDialog = true },
                                onAddProductClick = { showGlobalAddProductDialog = true },
                                onQuickSaveDraft = { title, content ->
                                    viewModel.quickSaveDraft(title, content)
                                },
                                onOrderStatusChange = { id, status -> viewModel.updateOrderStatus(id, status) },
                                onSyncClick = { viewModel.syncCurrentSite() },
                                onConnectSiteClick = { appScreen = AppScreen.LOGIN },
                                onSimulateOrder = { viewModel.triggerSimulatedOrderAlert() }
                            )
                        }
                        HubTab.STORE -> {
                            if (hasWooCommerce) {
                                StoreScreen(
                                    products = products,
                                    orders = orders,
                                    coupons = coupons,
                                    orderFilter = orderFilter,
                                    targetOrderId = targetOrderId,
                                    targetProductId = targetProductId,
                                    onClearTargetOrder = { viewModel.clearTargetOrder() },
                                    onClearTargetProduct = { viewModel.clearTargetProduct() },
                                    onFilterChange = { viewModel.setOrderFilter(it) },
                                    onOrderStatusChange = { id, status -> viewModel.updateOrderStatus(id, status) },
                                    onSaveProduct = { id, name, sku, regPrice, salePrice, stockQty, cat, type ->
                                        viewModel.saveProduct(id, name, sku, regPrice, salePrice, stockQty, cat, type)
                                    },
                                    onDeleteProduct = { id -> viewModel.deleteProduct(id) },
                                    onSaveCoupon = { code, type, amt, limit -> viewModel.saveCoupon(code, type, amt, limit) },
                                    onShowMessage = { viewModel.showMessage(it) },
                                    onPlaceCustomOrder = { name, email, items, total ->
                                        viewModel.placeWooCommerceOrder(name, email, items, total)
                                    },
                                    onTriggerSimulatedOrderAlert = { viewModel.triggerSimulatedOrderAlert() }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("WooCommerce Not Installed", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("This site operates in pure Content / Blog mode without WooCommerce e-commerce modules.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { viewModel.selectTab(HubTab.DASHBOARD) }) {
                                            Text("Go to Dashboard")
                                        }
                                    }
                                }
                            }
                        }
                        HubTab.CONTENT -> {
                            ContentScreen(
                                posts = posts,
                                contentFilter = contentFilter,
                                activeSite = currentSite,
                                targetPostId = targetPostId,
                                onClearTargetPost = { viewModel.clearTargetPost() },
                                onFilterChange = { viewModel.setContentFilter(it) },
                                onSavePost = { id, title, excerpt, content, status, postType, cat ->
                                    viewModel.savePost(id, title, excerpt, content, status, postType, cat)
                                },
                                onDeletePost = { id -> viewModel.deletePost(id) },
                                onShowMessage = { viewModel.showMessage(it) }
                            )
                        }
                        HubTab.CRM -> {
                            CrmScreen(
                                customers = customers,
                                onUpdateRole = { id, role -> viewModel.updateCustomerRole(id, role) },
                                onShowMessage = { viewModel.showMessage(it) }
                            )
                        }
                        HubTab.TOOLS -> {
                            ToolsScreen(
                                plugins = plugins,
                                waterTelemetry = waterTelemetry,
                                notificationSettings = notificationSettings,
                                currentSite = currentSite,
                                allSites = allSites,
                                themeMode = themeMode,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSelectSite = { viewModel.switchSite(it) },
                                onOpenLoginDialog = { appScreen = AppScreen.CONNECT },
                                onLogout = {
                                    viewModel.logoutCurrentSite()
                                    appScreen = AppScreen.CONNECT
                                },
                                onSignOutAll = {
                                    viewModel.signOutAll()
                                    appScreen = AppScreen.CONNECT
                                },
                                onSignOutAllAndClearDemoData = {
                                    viewModel.signOutAllAndClearDemoData()
                                    appScreen = AppScreen.CONNECT
                                },
                                onUpdateNotificationSettings = { viewModel.updateNotificationSettings(it) },
                                onTriggerTestOrderAlert = { viewModel.triggerSimulatedOrderAlert() },
                                onTriggerTestCommentAlert = { viewModel.triggerSimulatedCommentAlert() },
                                onTriggerTestLowStockAlert = { viewModel.triggerSimulatedLowStockAlert() },
                                onTriggerTestInquiryAlert = { viewModel.triggerSimulatedInquiryAlert() },
                                onTogglePlugin = { id, active -> viewModel.togglePlugin(id, active) },
                                onUpdatePlugin = { id, newVer -> viewModel.updatePlugin(id, newVer) },
                                onToggleWaterPump = { viewModel.toggleWaterPump() },
                                onToggleWaterAutoMode = { viewModel.toggleWaterAutoMode() },
                                onShowMessage = { viewModel.showMessage(it) }
                            )
                        }
                    }
                }
            }

            // Site Switcher Modal Sheet
            if (showSiteSwitcherSheet) {
                SiteSwitcherSheet(
                    sites = allSites,
                    currentSite = currentSite,
                    onSelectSite = { siteId ->
                        viewModel.switchSite(siteId)
                        showSiteSwitcherSheet = false
                    },
                    onAddNewSiteClick = {
                        showSiteSwitcherSheet = false
                        appScreen = AppScreen.LOGIN
                    },
                    onOpenLoginClick = {
                        showSiteSwitcherSheet = false
                        appScreen = AppScreen.LOGIN
                    },
                    onDismiss = { showSiteSwitcherSheet = false }
                )
            }

            // Add WordPress Site Dialog
            if (showAddSiteDialog) {
                AddSiteDialog(
                    onDismiss = { showAddSiteDialog = false },
                    onConnectSite = { name, url, username, appPassword ->
                        viewModel.addNewSite(name, url, username, appPassword)
                        showAddSiteDialog = false
                    },
                    onLoginWithCredentials = { name, url, username, pass ->
                        viewModel.loginWithWordPress(
                            siteId = null,
                            siteUrl = url,
                            siteName = name,
                            usernameOrEmail = username,
                            passwordOrToken = pass
                        )
                        showAddSiteDialog = false
                    }
                )
            }

            // WordPress Account Login Dialog (legacy fallback)
            if (showLoginDialog) {
                WordPressLoginDialog(
                    currentSite = currentSite,
                    allSites = allSites,
                    onDismiss = { showLoginDialog = false },
                    onLogin = { siteId, siteUrl, siteName, usernameOrEmail, password, role, rememberMe ->
                        viewModel.loginWithWordPress(
                            siteId = siteId,
                            siteUrl = siteUrl,
                            siteName = siteName,
                            usernameOrEmail = usernameOrEmail,
                            passwordOrToken = password,
                            role = role,
                            rememberMe = rememberMe
                        )
                        showLoginDialog = false
                    }
                )
            }

            // Global Gutenberg Post Editor Dialog
            if (showGlobalEditorDialog) {
                GutenbergEditor(
                    activeSite = currentSite,
                    initialPost = null,
                    defaultPostType = "post",
                    onDismiss = { showGlobalEditorDialog = false },
                    onSavePost = { title, excerpt, blocksJson, status, postType, category, featuredImageUrl ->
                        viewModel.savePost(
                            id = null,
                            title = title,
                            excerpt = excerpt,
                            content = blocksJson,
                            status = status,
                            postType = postType,
                            category = category,
                            featuredImageUrl = featuredImageUrl
                        )
                        showGlobalEditorDialog = false
                    },
                    onAutoSave = { title, excerpt, blocksJson, status, postType, category, featuredImageUrl ->
                        viewModel.savePost(
                            id = null,
                            title = title,
                            excerpt = excerpt,
                            content = blocksJson,
                            status = "draft",
                            postType = postType,
                            category = category,
                            featuredImageUrl = featuredImageUrl
                        )
                    }
                )
            }

            // Global Add Product Dialog
            if (showGlobalAddProductDialog) {
                ProductEditDialog(
                    initialProduct = null,
                    onDismiss = { showGlobalAddProductDialog = false },
                    onSave = { name, sku, regPrice, salePrice, stockQty, cat, type ->
                        viewModel.saveProduct(null, name, sku, regPrice, salePrice, stockQty, cat, type)
                        showGlobalAddProductDialog = false
                    }
                )
            }

            // Notifications Center Sheet
            if (showNotificationsCenterSheet) {
                NotificationsCenterSheet(
                    notifications = notifications,
                    settings = notificationSettings,
                    onDismiss = { showNotificationsCenterSheet = false },
                    onNotificationClick = { notif ->
                        viewModel.onNotificationClicked(notif)
                        showNotificationsCenterSheet = false
                    },
                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                    onClearAll = { viewModel.clearAllNotifications() },
                    onDeleteNotification = { id -> viewModel.deleteNotification(id) },
                    onUpdateSettings = { newSettings -> viewModel.updateNotificationSettings(newSettings) },
                    onTriggerTestOrderAlert = { viewModel.triggerSimulatedOrderAlert() },
                    onTriggerTestCommentAlert = { viewModel.triggerSimulatedCommentAlert() },
                    onTriggerTestLowStockAlert = { viewModel.triggerSimulatedLowStockAlert() },
                    onTriggerTestInquiryAlert = { viewModel.triggerSimulatedInquiryAlert() }
                )
            }

            // Dashboard Customizer Sheet
            if (showDashboardCustomizerSheet) {
                DashboardCustomizerSheet(
                    currentSite = currentSite,
                    widgets = dashboardWidgets,
                    onDismiss = { showDashboardCustomizerSheet = false },
                    onToggleWidget = { id, isEnabled ->
                        viewModel.toggleDashboardWidget(id, isEnabled)
                    },
                    onSaveWidgets = { list ->
                        viewModel.saveDashboardWidgets(list)
                    },
                    onAutoDesign = {
                        viewModel.inspectAndAutoDesignCurrentSite()
                    },
                    onResetDefaults = {
                        viewModel.resetDashboardToAutoDesigned()
                    }
                )
            }
            }
        }
    }
}
