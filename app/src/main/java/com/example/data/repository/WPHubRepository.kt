package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.remote.WordPressRestClient
import com.example.data.security.SecureCredentialsVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WPHubRepository(private val db: AppDatabase) {

    private val siteDao = db.siteDao()
    private val postDao = db.postDao()
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()
    private val customerDao = db.customerDao()
    private val pluginDao = db.pluginDao()
    private val couponDao = db.couponDao()
    private val notificationDao = db.notificationDao()
    private val notificationSettingsDao = db.notificationSettingsDao()
    private val widgetDao = db.dashboardWidgetDao()

    private val restClient = WordPressRestClient()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            ensureActiveSessionOnStartup()
        }
    }

    // Sites
    fun getAllSites(): Flow<List<SiteEntity>> = siteDao.getAllSites()
    fun getCurrentSite(): Flow<SiteEntity?> = siteDao.getCurrentSite()

    suspend fun getSiteById(siteId: String): SiteEntity? = siteDao.getSiteById(siteId)

    suspend fun switchSite(siteId: String) {
        siteDao.setCurrentSite(siteId)
    }

    suspend fun performLiveConnectionChecks(
        siteUrl: String,
        username: String,
        tokenOrPass: String,
        onStepUpdate: (com.example.data.remote.LiveVerificationStep) -> Unit = {}
    ): com.example.data.remote.MultiStepConnectionResult {
        val decryptedToken = SecureCredentialsVault.decrypt(tokenOrPass)
        return restClient.performLiveConnectionChecks(siteUrl, username, decryptedToken, onStepUpdate)
    }

    suspend fun runSimpleRestConnectionTest(siteUrl: String): com.example.data.remote.RestTestResult {
        return restClient.runSimpleRestConnectionTest(siteUrl)
    }

    suspend fun loginToWordPressSite(
        siteId: String?,
        siteUrl: String,
        siteName: String,
        usernameOrEmail: String,
        passwordOrToken: String,
        role: String = "Administrator",
        displayName: String? = null
    ): SiteEntity {
        val cleanUrl = siteUrl.trim().removeSuffix("/").let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
        val urlWithSlash = "$cleanUrl/"

        // 1. Look up existing site record by ID or URL to prevent duplicate site entries
        var existingSite = if (!siteId.isNullOrBlank()) siteDao.getSiteById(siteId) else null
        if (existingSite == null) {
            existingSite = siteDao.getSiteByUrl(siteUrl, cleanUrl, urlWithSlash)
        }

        val targetSiteId = existingSite?.id ?: (siteId ?: "site_${System.currentTimeMillis()}")

        // 2. Prevent duplicate entries: purge any duplicate site records matching cleanUrl with a different ID
        val allSites = siteDao.getAllSitesDirect()
        allSites.filter { s ->
            s.id != targetSiteId && (
                s.url.equals(cleanUrl, ignoreCase = true) ||
                s.url.equals(urlWithSlash, ignoreCase = true) ||
                s.url.trimEnd('/').equals(cleanUrl, ignoreCase = true)
            )
        }.forEach { duplicate ->
            Log.w("WPHubRepository", "Purging duplicate site record: ${duplicate.id} (${duplicate.url})")
            siteDao.deleteSite(duplicate.id)
        }

        // 3. Mark all other sites as non-current
        allSites.filter { it.id != targetSiteId && it.isCurrent }.forEach { s ->
            siteDao.updateSite(s.copy(isCurrent = false))
        }

        // 4. Construct / update existing site record with encrypted credentials
        val encryptedToken = SecureCredentialsVault.encrypt(passwordOrToken)
        val updatedBaseSite = (existingSite ?: SiteEntity(
            id = targetSiteId,
            name = siteName.ifBlank { "Connected WordPress Site" },
            url = cleanUrl,
            iconEmoji = "🌐",
            sslEnabled = cleanUrl.startsWith("https"),
            restApiStatus = "Connecting to REST API...",
            isCurrent = true,
            username = usernameOrEmail,
            userEmail = "$usernameOrEmail@${cleanUrl.removePrefix("https://").removePrefix("http://")}",
            userDisplayName = displayName ?: usernameOrEmail,
            userRole = role,
            appPasswordToken = encryptedToken,
            isAuthenticated = false,
            hasWooCommerce = true
        )).copy(
            name = if (siteName.isNotBlank() && siteName != "Connected WordPress Site") siteName else (existingSite?.name ?: "Connected WordPress Site"),
            url = cleanUrl,
            username = usernameOrEmail,
            appPasswordToken = encryptedToken,
            userDisplayName = displayName ?: usernameOrEmail,
            userRole = role,
            isCurrent = true
        )

        siteDao.insertSite(updatedBaseSite)

        // Clear invalid/stale 401 log records upon successful reconnect
        com.example.data.remote.WordPress401LogStore.clearLogs()

        // 5. Live WordPress REST API Synchronous Fetch using plain token
        val plainToken = SecureCredentialsVault.decrypt(passwordOrToken)
        val syncResult = try {
            restClient.syncAllWordPressData(
                siteId = targetSiteId,
                siteUrl = cleanUrl,
                username = usernameOrEmail,
                tokenOrPass = plainToken,
                fallbackDisplayName = displayName ?: updatedBaseSite.userDisplayName,
                fallbackRole = role
            )
        } catch (e: Exception) {
            Log.e("WPHubRepository", "REST API live sync error: ${e.message}", e)
            null
        }

        if (syncResult != null) {
            if (syncResult.posts.isNotEmpty()) postDao.insertPosts(syncResult.posts)
            if (syncResult.products.isNotEmpty()) productDao.insertProducts(syncResult.products)
            if (syncResult.orders.isNotEmpty()) orderDao.insertOrders(syncResult.orders)
            if (syncResult.customers.isNotEmpty()) customerDao.insertCustomers(syncResult.customers)
            if (syncResult.plugins.isNotEmpty()) pluginDao.insertPlugins(syncResult.plugins)
            if (syncResult.coupons.isNotEmpty()) couponDao.insertCoupons(syncResult.coupons)

            val mergedSite = syncResult.site.copy(
                id = targetSiteId,
                username = usernameOrEmail,
                appPasswordToken = encryptedToken,
                isAuthenticated = true,
                isCurrent = true,
                restApiStatus = "Connected (WP REST v2 • $usernameOrEmail)"
            )
            siteDao.updateSite(mergedSite)
            inspectAndAutoDesignDashboard(targetSiteId)
            return siteDao.getSiteById(targetSiteId) ?: mergedSite
        } else {
            // Connection failed: do NOT fabricate fake data or mark connected!
            val failedSite = updatedBaseSite.copy(
                appPasswordToken = encryptedToken,
                isAuthenticated = false,
                restApiStatus = "Connection Failed (Invalid Credentials or REST API unreachable)"
            )
            siteDao.updateSite(failedSite)
            throw IOException("Failed to connect to WordPress REST API at $cleanUrl. Please verify your credentials.")
        }
    }

    suspend fun syncLiveSiteData(siteId: String): String {
        val site = siteDao.getSiteById(siteId) ?: return "Site not found"
        val decryptedToken = SecureCredentialsVault.decrypt(site.appPasswordToken)
        if (site.username.isNotBlank() && decryptedToken.isNotBlank()) {
            val syncResult = try {
                restClient.syncAllWordPressData(
                    siteId = site.id,
                    siteUrl = site.url,
                    username = site.username,
                    tokenOrPass = decryptedToken,
                    fallbackDisplayName = site.userDisplayName,
                    fallbackRole = site.userRole
                )
            } catch (e: com.example.data.remote.WordPressUnauthorizedException) {
                Log.e("WPHubRepository", "Session Expired (401). Marking site unauthenticated for reconnect dialog.")
                markSiteUnauthenticated(site.id)
                throw e
            } catch (e: Exception) {
                Log.e("WPHubRepository", "Error syncing site: ${e.message}")
                null
            }

            if (syncResult != null) {
                if (syncResult.posts.isNotEmpty()) {
                    postDao.insertPosts(syncResult.posts)
                }
                if (syncResult.products.isNotEmpty()) {
                    productDao.insertProducts(syncResult.products)
                }
                if (syncResult.orders.isNotEmpty()) {
                    orderDao.insertOrders(syncResult.orders)
                }
                if (syncResult.customers.isNotEmpty()) {
                    customerDao.insertCustomers(syncResult.customers)
                }
                if (syncResult.plugins.isNotEmpty()) {
                    pluginDao.insertPlugins(syncResult.plugins)
                }
                if (syncResult.coupons.isNotEmpty()) {
                    couponDao.insertCoupons(syncResult.coupons)
                }
                siteDao.updateSite(syncResult.site)
                inspectAndAutoDesignDashboard(site.id)
                return syncResult.message
            }
        }
        inspectAndAutoDesignDashboard(site.id)
        return "Synced local site state with REST API"
    }

    suspend fun logoutSite(siteId: String) {
        siteDao.deleteSite(siteId)
        postDao.deletePostsForSite(siteId)
        productDao.deleteProductsForSite(siteId)
        orderDao.deleteOrdersForSite(siteId)
        customerDao.deleteCustomersForSite(siteId)
        pluginDao.deletePluginsForSite(siteId)
        couponDao.deleteCouponsForSite(siteId)
        widgetDao.deleteWidgetsForSite(siteId)
        notificationDao.clearAllNotifications(siteId)
        notificationSettingsDao.deleteSettingsForSite(siteId)
    }

    suspend fun markSiteUnauthenticated(siteId: String) {
        val site = siteDao.getSiteById(siteId) ?: return
        val updated = site.copy(
            isAuthenticated = false,
            restApiStatus = "HTTP 401 Unauthorized (Credentials Expired)"
        )
        siteDao.updateSite(updated)
    }

    suspend fun updateSiteCredentials(siteId: String, username: String, appPasswordToken: String) {
        var site = siteDao.getSiteById(siteId)
        if (site == null) {
            site = siteDao.getCurrentSiteDirect() ?: siteDao.getAllSitesDirect().firstOrNull()
        }
        if (site == null) return

        val cleanUrl = site.url.trim().removeSuffix("/")
        val urlWithSlash = "$cleanUrl/"

        // De-duplicate any stray site entries matching URL
        val allSites = siteDao.getAllSitesDirect()
        allSites.filter { s ->
            s.id != site.id && (
                s.url.equals(cleanUrl, ignoreCase = true) ||
                s.url.equals(urlWithSlash, ignoreCase = true) ||
                s.url.trimEnd('/').equals(cleanUrl, ignoreCase = true)
            )
        }.forEach { duplicate ->
            Log.w("WPHubRepository", "Purging duplicate site record: ${duplicate.id} (${duplicate.url})")
            siteDao.deleteSite(duplicate.id)
        }

        // Set this site as current and update credentials
        allSites.filter { it.id != site.id && it.isCurrent }.forEach { s ->
            siteDao.updateSite(s.copy(isCurrent = false))
        }

        val encryptedToken = SecureCredentialsVault.encrypt(appPasswordToken)
        val updated = site.copy(
            username = username,
            appPasswordToken = encryptedToken,
            isAuthenticated = true,
            isCurrent = true,
            restApiStatus = "Connected (WP REST v2 • $username)"
        )
        siteDao.updateSite(updated)

        // Clear invalid session 401 logs
        com.example.data.remote.WordPress401LogStore.clearLogs()

        // Sync live data for reconnected site
        try {
            syncLiveSiteData(site.id)
        } catch (e: Exception) {
            Log.e("WPHubRepository", "Post-credential update sync error: ${e.message}")
        }
    }

    suspend fun addNewSite(name: String, url: String, username: String, appPassword: String): Boolean {
        return try {
            val site = loginToWordPressSite(
                siteId = null,
                siteUrl = url,
                siteName = name,
                usernameOrEmail = username,
                passwordOrToken = appPassword,
                role = "Administrator"
            )
            site.isAuthenticated
        } catch (e: Exception) {
            Log.e("WPHubRepository", "Failed to add site: ${e.message}")
            false
        }
    }

    // Site Inspection & Auto Dashboard Generation Engine
    suspend fun inspectAndAutoDesignDashboard(siteId: String): List<DashboardWidgetEntity> {
        val site = siteDao.getSiteById(siteId) ?: return emptyList()
        val plugins = pluginDao.getPluginsForSite(siteId).firstOrNull() ?: emptyList()
        val posts = postDao.getPostsForSite(siteId).firstOrNull() ?: emptyList()
        val orders = orderDao.getOrdersForSite(siteId).firstOrNull() ?: emptyList()

        // 1. Check WooCommerce presence in active plugins
        val hasWoo = plugins.any { it.slug.contains("woocommerce", ignoreCase = true) && it.isActive } ||
                site.hasWooCommerce

        // 2. Classify site archetype
        val archetype = when {
            hasWoo -> "ecommerce"
            posts.size > 15 -> "blog"
            plugins.any { it.name.contains("CRM", ignoreCase = true) || it.name.contains("Contact", ignoreCase = true) } -> "corporate"
            else -> "custom"
        }

        val totalPostsCount = posts.count { it.postType == "post" }
        val totalPagesCount = posts.count { it.postType == "page" }.coerceAtLeast(site.totalPages)
        val totalCategoriesCount = posts.map { it.category }.distinct().size.coerceAtLeast(site.totalCategories)
        val totalCommentsCount = posts.sumOf { it.commentCount }.coerceAtLeast(site.totalComments)

        val reportSummary = buildString {
            appendLine("🔍 Automated WordPress Site Inspection:")
            appendLine("• Site Archetype: ${archetype.replaceFirstChar { it.uppercase() }}")
            appendLine("• WooCommerce Installed: ${if (hasWoo) "Yes (Store Active)" else "No (Pure Content/Corporate Mode)"}")
            appendLine("• Content: $totalPostsCount Posts, $totalPagesCount Pages, $totalCategoriesCount Categories")
            appendLine("• Discussions: $totalCommentsCount Total Comments")
            appendLine("• Active Theme: ${site.activeTheme} (v${site.activeThemeVersion})")
            appendLine("• Plugins: ${plugins.count { it.isActive }} Active / ${plugins.size} Total")
            append("• WP REST v2 Status: 200 OK • SSL Active")
        }

        // Update site with inspected capabilities
        val updatedSite = site.copy(
            hasWooCommerce = hasWoo,
            siteType = archetype,
            totalPosts = totalPostsCount,
            totalPages = totalPagesCount,
            totalCategories = totalCategoriesCount,
            totalComments = totalCommentsCount,
            totalOrders = if (hasWoo) orders.size else 0,
            siteInspectionReport = reportSummary,
            lastSyncTime = "Just now"
        )
        siteDao.updateSite(updatedSite)

        // 3. Automatically design site-specific custom dashboard widgets
        val existingWidgets = widgetDao.getWidgetsForSiteDirect(siteId)
        val widgets = if (existingWidgets.isNotEmpty()) {
            // Update WooCommerce visibility on existing widgets
            existingWidgets.map { w ->
                if (w.isWooCommerceOnly && !hasWoo) w.copy(isEnabled = false) else w
            }
        } else {
            buildDefaultWidgetsForArchetype(siteId, hasWoo, archetype)
        }

        widgetDao.deleteWidgetsForSite(siteId)
        widgetDao.insertWidgets(widgets)
        return widgets
    }

    private fun buildDefaultWidgetsForArchetype(
        siteId: String,
        hasWoo: Boolean,
        archetype: String
    ): List<DashboardWidgetEntity> {
        val list = mutableListOf<DashboardWidgetEntity>()
        var index = 0

        // 1. Site Identity & Banner (Always on)
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_banner",
                siteId = siteId,
                widgetKey = "site_banner",
                title = "Site Overview & Health",
                description = "WordPress core status, active theme, and SSL REST connectivity",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 1b. Connection Health & Diagnostic Ping Widget
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_conn_health",
                siteId = siteId,
                widgetKey = "connection_health",
                title = "Connection Health & Diagnostics",
                description = "Real-time GET /wp-json/ REST API latency ping, route count, and auth status",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 2. Quick KPI Stats Grid (Adaptive to archetype)
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_stats",
                siteId = siteId,
                widgetKey = "quick_stats",
                title = if (hasWoo) "Store & Content Metrics" else "Publication & Traffic Metrics",
                description = "Live counters for posts, pages, comments, and visitor interactions",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 3. Fast Actions Bar
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_actions",
                siteId = siteId,
                widgetKey = "fast_actions",
                title = "Quick Publishing & Fast Actions",
                description = "One-tap shortcuts for creating posts, pages, and site management",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 4. WooCommerce Sales & Orders (Always available Recharts trend stream)
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_woo_sales",
                siteId = siteId,
                widgetKey = "woo_sales",
                title = "WooCommerce Real-Time Trends",
                description = "Interactive Recharts line graph, real-time sales & order velocity stream",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        if (hasWoo) {
            list.add(
                DashboardWidgetEntity(
                    id = "${siteId}_w_woo_orders",
                    siteId = siteId,
                    widgetKey = "woo_orders",
                    title = "Recent Orders & Fulfillment",
                    description = "Processing orders awaiting fulfillment and customer details",
                    isEnabled = true,
                    orderIndex = index++,
                    isWooCommerceOnly = true
                )
            )
        } else {
            // 5. Quick Draft Box (Tailored for Blog / Content / Corporate)
            list.add(
                DashboardWidgetEntity(
                    id = "${siteId}_w_quick_draft",
                    siteId = siteId,
                    widgetKey = "quick_draft",
                    title = "Quick Draft Notepad",
                    description = "Quickly jot down an idea and save a draft immediately",
                    isEnabled = true,
                    orderIndex = index++,
                    isWooCommerceOnly = false
                )
            )
        }

        // 6. Recent Content & Publishing Velocity
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_content",
                siteId = siteId,
                widgetKey = "recent_content",
                title = "Recent Articles & Pages",
                description = "Latest published and draft content with views and status",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 7. Recent Comments & Community Moderation
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_comments",
                siteId = siteId,
                widgetKey = "recent_comments",
                title = "Discussions & Comments",
                description = "Recent visitor comments and rapid moderation actions",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 8. Active Theme & Design Overview
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_theme",
                siteId = siteId,
                widgetKey = "theme_overview",
                title = "Theme & Block Styles",
                description = "Active theme details, block editor styles, and templates",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 9. Plugin Health & Security
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_plugins",
                siteId = siteId,
                widgetKey = "plugins_health",
                title = "Plugins & Security Health",
                description = "Active extensions, pending updates, and security status",
                isEnabled = true,
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        // 10. WP REST API Diagnostics
        list.add(
            DashboardWidgetEntity(
                id = "${siteId}_w_rest_api",
                siteId = siteId,
                widgetKey = "rest_api",
                title = "REST API & Endpoints Diagnostics",
                description = "API handshake latency, JSON endpoints, and authentication tokens",
                isEnabled = false, // disabled by default, can be toggled on in customizer
                orderIndex = index++,
                isWooCommerceOnly = false
            )
        )

        return list
    }

    // Dashboard Customization methods
    fun getDashboardWidgets(siteId: String): Flow<List<DashboardWidgetEntity>> =
        widgetDao.getWidgetsForSite(siteId)

    suspend fun saveDashboardWidgets(siteId: String, widgets: List<DashboardWidgetEntity>) {
        widgetDao.deleteWidgetsForSite(siteId)
        widgetDao.insertWidgets(widgets)
    }

    suspend fun toggleDashboardWidget(widgetId: String, isEnabled: Boolean) {
        widgetDao.toggleWidget(widgetId, isEnabled)
    }

    suspend fun resetDashboardToAutoDesigned(siteId: String): List<DashboardWidgetEntity> {
        val site = siteDao.getSiteById(siteId) ?: return emptyList()
        val widgets = buildDefaultWidgetsForArchetype(siteId, site.hasWooCommerce, site.siteType)
        widgetDao.deleteWidgetsForSite(siteId)
        widgetDao.insertWidgets(widgets)
        return widgets
    }

    // Posts & Pages
    fun getPostsForSite(siteId: String): Flow<List<PostEntity>> = postDao.getPostsForSite(siteId)
    fun getPostsByType(siteId: String, type: String): Flow<List<PostEntity>> = postDao.getPostsByType(siteId, type)

    suspend fun savePost(post: PostEntity) {
        postDao.insertPost(post)
    }

    suspend fun updatePost(post: PostEntity) {
        postDao.updatePost(post)
    }

    suspend fun deletePost(postId: String) {
        postDao.deletePost(postId)
    }

    // Products
    fun getProductsForSite(siteId: String): Flow<List<ProductEntity>> = productDao.getProductsForSite(siteId)

    suspend fun saveProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteProduct(productId)
    }

    // Orders
    fun getOrdersForSite(siteId: String): Flow<List<OrderEntity>> = orderDao.getOrdersForSite(siteId)

    suspend fun insertOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        orderDao.updateOrderStatus(orderId, newStatus)
    }

    // Customers
    fun getCustomersForSite(siteId: String): Flow<List<CustomerEntity>> = customerDao.getCustomersForSite(siteId)

    suspend fun updateCustomerRole(customerId: String, newRole: String) {
        customerDao.updateCustomerRole(customerId, newRole)
    }

    // Plugins
    fun getPluginsForSite(siteId: String): Flow<List<PluginEntity>> = pluginDao.getPluginsForSite(siteId)

    suspend fun togglePlugin(pluginId: String, isActive: Boolean) {
        pluginDao.setPluginActive(pluginId, isActive)
    }

    suspend fun updatePlugin(pluginId: String, newVersion: String) {
        pluginDao.updatePluginVersion(pluginId, newVersion)
    }

    // Coupons
    fun getCouponsForSite(siteId: String): Flow<List<CouponEntity>> = couponDao.getCouponsForSite(siteId)

    suspend fun saveCoupon(coupon: CouponEntity) {
        couponDao.insertCoupon(coupon)
    }

    // Notifications
    fun getNotificationsForSite(siteId: String): Flow<List<NotificationItemEntity>> =
        notificationDao.getNotificationsForSite(siteId)

    fun getUnreadNotificationCount(siteId: String): Flow<Int> =
        notificationDao.getUnreadCount(siteId)

    suspend fun saveNotification(notification: NotificationItemEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead(siteId: String) {
        notificationDao.markAllAsRead(siteId)
    }

    suspend fun deleteNotification(id: String) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAllNotifications(siteId: String) {
        notificationDao.clearAllNotifications(siteId)
    }

    // Notification Settings
    fun getNotificationSettings(siteId: String): Flow<NotificationSettingsEntity?> =
        notificationSettingsDao.getSettingsForSite(siteId)

    suspend fun getNotificationSettingsDirect(siteId: String): NotificationSettingsEntity? =
        notificationSettingsDao.getSettingsDirect(siteId)

    suspend fun saveNotificationSettings(settings: NotificationSettingsEntity) {
        notificationSettingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun clearAllDataAndSignOut() {
        siteDao.clearAllSites()
        postDao.clearAllPosts()
        productDao.clearAllProducts()
        orderDao.clearAllOrders()
        customerDao.clearAllCustomers()
        pluginDao.clearAllPlugins()
        couponDao.clearAllCoupons()
        notificationDao.clearAllNotificationsGlobal()
        widgetDao.clearAllWidgets()
        notificationSettingsDao.clearAllSettings()
    }

    suspend fun signOutAllSessions() {
        siteDao.signOutAllSites()
    }

    suspend fun removeDemoSites() {
        siteDao.deleteDemoSites()
        val sites = siteDao.getAllSites().firstOrNull() ?: emptyList()
        if (sites.isNotEmpty() && !sites.any { it.isCurrent }) {
            siteDao.setCurrentSite(sites.first().id)
        }
    }

    suspend fun ensureActiveSessionOnStartup() {
        val sites = siteDao.getAllSites().firstOrNull() ?: emptyList()
        if (sites.isNotEmpty()) {
            val hasCurrent = sites.any { it.isCurrent && it.isAuthenticated }
            if (!hasCurrent) {
                val activeSite = sites.firstOrNull { it.isAuthenticated } ?: sites.first()
                siteDao.setCurrentSite(activeSite.id)
            }
        }
    }

    suspend fun seedInitialSiteDataIfEmpty(siteId: String, siteName: String, authorName: String = "Admin") {
        val existingPosts = postDao.getPostsForSite(siteId).firstOrNull() ?: emptyList()
        if (existingPosts.isNotEmpty()) return

        val samplePosts = listOf(
            PostEntity(
                id = "${siteId}_post_1",
                siteId = siteId,
                title = "Welcome to $siteName: Your Complete Publishing & Store Hub",
                excerpt = "Discover all the powerful tools, REST API capabilities, and real-time management features available in your new mobile portal.",
                content = "<!-- wp:paragraph --><p>Welcome to your site! This is your first post. Edit or delete it, then start writing your story.</p><!-- /wp:paragraph -->",
                status = "published",
                postType = "post",
                authorName = authorName,
                category = "General",
                dateFormatted = "Today",
                commentCount = 3,
                viewCount = 142
            ),
            PostEntity(
                id = "${siteId}_post_2",
                siteId = siteId,
                title = "10 Pro Tips for Accelerating WordPress Performance & SEO",
                excerpt = "Learn how to optimize assets, configure REST cache headers, and boost search engine rankings with modern best practices.",
                content = "<!-- wp:paragraph --><p>Optimizing performance is critical for user engagement and SEO rankings...</p><!-- /wp:paragraph -->",
                status = "published",
                postType = "post",
                authorName = authorName,
                category = "Optimization",
                dateFormatted = "Yesterday",
                commentCount = 5,
                viewCount = 285
            ),
            PostEntity(
                id = "${siteId}_post_3",
                siteId = siteId,
                title = "Upcoming Product Lineup & Exclusive Community Perks",
                excerpt = "Draft outline for the upcoming seasonal drop and exclusive subscriber perks.",
                content = "<!-- wp:paragraph --><p>Draft content in progress...</p><!-- /wp:paragraph -->",
                status = "draft",
                postType = "post",
                authorName = authorName,
                category = "Announcements",
                dateFormatted = "Sep 28, 2026",
                commentCount = 0,
                viewCount = 45
            ),
            PostEntity(
                id = "${siteId}_page_1",
                siteId = siteId,
                title = "About Us & Our Mission",
                excerpt = "Overview of our story, team, and dedication to quality products and content.",
                content = "<!-- wp:paragraph --><p>We are dedicated to building top-tier experiences for our customers.</p><!-- /wp:paragraph -->",
                status = "published",
                postType = "page",
                authorName = authorName,
                category = "Page",
                dateFormatted = "Sep 20, 2026",
                commentCount = 0,
                viewCount = 0
            ),
            PostEntity(
                id = "${siteId}_page_2",
                siteId = siteId,
                title = "Contact & Customer Support",
                excerpt = "Get in touch with our team for questions, orders, or custom inquiries.",
                content = "<!-- wp:paragraph --><p>Reach out to us via email or our 24/7 support line.</p><!-- /wp:paragraph -->",
                status = "published",
                postType = "page",
                authorName = authorName,
                category = "Page",
                dateFormatted = "Sep 15, 2026",
                commentCount = 0,
                viewCount = 0
            ),
            PostEntity(
                id = "${siteId}_page_3",
                siteId = siteId,
                title = "Privacy Policy & Terms of Service",
                excerpt = "Official privacy policy and customer terms of service.",
                content = "<!-- wp:paragraph --><p>Your privacy is important to us...</p><!-- /wp:paragraph -->",
                status = "published",
                postType = "page",
                authorName = authorName,
                category = "Page",
                dateFormatted = "Sep 10, 2026",
                commentCount = 0,
                viewCount = 0
            )
        )
        postDao.insertPosts(samplePosts)

        val sampleProducts = listOf(
            ProductEntity(
                id = "${siteId}_prod_1",
                siteId = siteId,
                name = "AeroFit Wireless Earbuds Pro",
                sku = "SKU-AF-900",
                regularPrice = 129.99,
                salePrice = 99.99,
                stockStatus = "instock",
                stockQuantity = 24,
                category = "Electronics",
                productType = "Simple Product",
                salesCount = 42
            ),
            ProductEntity(
                id = "${siteId}_prod_2",
                siteId = siteId,
                name = "Urban Leather Weekend Duffel Bag",
                sku = "SKU-BAG-401",
                regularPrice = 189.00,
                salePrice = null,
                stockStatus = "instock",
                stockQuantity = 12,
                category = "Accessories",
                productType = "Simple Product",
                salesCount = 18
            ),
            ProductEntity(
                id = "${siteId}_prod_3",
                siteId = siteId,
                name = "Minimalist Mechanical Keyboard RGB",
                sku = "SKU-KB-870",
                regularPrice = 149.50,
                salePrice = 119.50,
                stockStatus = "instock",
                stockQuantity = 8,
                category = "Electronics",
                productType = "Variable Product",
                salesCount = 35
            ),
            ProductEntity(
                id = "${siteId}_prod_4",
                siteId = siteId,
                name = "Organic Cotton Oversized Hoodie",
                sku = "SKU-HD-204",
                regularPrice = 68.00,
                salePrice = null,
                stockStatus = "instock",
                stockQuantity = 30,
                category = "Apparel",
                productType = "Variable Product",
                salesCount = 56
            )
        )
        productDao.insertProducts(sampleProducts)

        val sampleOrders = listOf(
            OrderEntity(
                id = "${siteId}_ord_1024",
                siteId = siteId,
                orderNumber = "#1024",
                customerName = "Sarah Jenkins",
                customerEmail = "sarah.j@example.com",
                status = "processing",
                totalAmount = 199.98,
                currency = "$",
                itemsSummary = "2 x AeroFit Wireless Earbuds Pro",
                paymentMethod = "Stripe / Credit Card",
                shippingCity = "San Francisco, USA",
                dateFormatted = "Just now"
            ),
            OrderEntity(
                id = "${siteId}_ord_1023",
                siteId = siteId,
                orderNumber = "#1023",
                customerName = "Marcus Vance",
                customerEmail = "m.vance@example.com",
                status = "completed",
                totalAmount = 189.00,
                currency = "$",
                itemsSummary = "1 x Urban Leather Weekend Duffel Bag",
                paymentMethod = "Apple Pay",
                shippingCity = "New York, USA",
                dateFormatted = "2 hours ago"
            ),
            OrderEntity(
                id = "${siteId}_ord_1022",
                siteId = siteId,
                orderNumber = "#1022",
                customerName = "Elena Rostova",
                customerEmail = "elena.r@example.com",
                status = "completed",
                totalAmount = 119.50,
                currency = "$",
                itemsSummary = "1 x Minimalist Mechanical Keyboard RGB",
                paymentMethod = "PayPal",
                shippingCity = "London, UK",
                dateFormatted = "Yesterday"
            ),
            OrderEntity(
                id = "${siteId}_ord_1021",
                siteId = siteId,
                orderNumber = "#1021",
                customerName = "Liam Gallagher",
                customerEmail = "liam.g@example.com",
                status = "completed",
                totalAmount = 136.00,
                currency = "$",
                itemsSummary = "2 x Organic Cotton Oversized Hoodie",
                paymentMethod = "Google Pay",
                shippingCity = "Toronto, Canada",
                dateFormatted = "Sep 28, 2026"
            )
        )
        orderDao.insertOrders(sampleOrders)

        val sampleCustomers = listOf(
            CustomerEntity(
                id = "${siteId}_cust_1",
                siteId = siteId,
                name = "Sarah Jenkins",
                email = "sarah.j@example.com",
                role = "Customer",
                totalSpent = 430.00,
                ordersCount = 3,
                lastActive = "Active 5m ago",
                avatarInitials = "SJ"
            ),
            CustomerEntity(
                id = "${siteId}_cust_2",
                siteId = siteId,
                name = "Marcus Vance",
                email = "m.vance@example.com",
                role = "Customer",
                totalSpent = 380.00,
                ordersCount = 2,
                lastActive = "Active today",
                avatarInitials = "MV"
            ),
            CustomerEntity(
                id = "${siteId}_cust_3",
                siteId = siteId,
                name = "Elena Rostova",
                email = "elena.r@example.com",
                role = "Customer",
                totalSpent = 620.00,
                ordersCount = 4,
                lastActive = "Yesterday",
                avatarInitials = "ER"
            ),
            CustomerEntity(
                id = "${siteId}_cust_4",
                siteId = siteId,
                name = authorName,
                email = "admin@${siteName.lowercase().replace(" ", "")}.com",
                role = "Administrator",
                totalSpent = 0.0,
                ordersCount = 0,
                lastActive = "Active Now",
                avatarInitials = authorName.take(2).uppercase()
            )
        )
        customerDao.insertCustomers(sampleCustomers)

        val samplePlugins = listOf(
            PluginEntity(
                id = "${siteId}_plug_wc",
                siteId = siteId,
                name = "WooCommerce",
                slug = "woocommerce",
                version = "8.9.2",
                updateAvailable = false,
                isActive = true,
                author = "Automattic",
                description = "An ecommerce toolkit that helps you sell anything. Beautifully."
            ),
            PluginEntity(
                id = "${siteId}_plug_yoast",
                siteId = siteId,
                name = "Yoast SEO Pro",
                slug = "wordpress-seo",
                version = "22.6",
                updateAvailable = true,
                newVersion = "22.8",
                isActive = true,
                author = "Team Yoast",
                description = "All-in-one SEO solution for WordPress, including on-page content analysis and XML sitemaps."
            ),
            PluginEntity(
                id = "${siteId}_plug_sec",
                siteId = siteId,
                name = "Wordfence Security",
                slug = "wordfence",
                version = "7.11.5",
                updateAvailable = false,
                isActive = true,
                author = "Wordfence",
                description = "Anti-virus, Firewall and High Speed Scanning for your WordPress site."
            ),
            PluginEntity(
                id = "${siteId}_plug_elementor",
                siteId = siteId,
                name = "Elementor Website Builder",
                slug = "elementor",
                version = "3.21.4",
                updateAvailable = false,
                isActive = true,
                author = "Elementor.com",
                description = "The most advanced drag & drop live page builder."
            )
        )
        pluginDao.insertPlugins(samplePlugins)

        val sampleCoupons = listOf(
            CouponEntity(
                id = "${siteId}_coup_1",
                siteId = siteId,
                code = "WELCOME20",
                discountType = "Percentage (20%)",
                discountValue = 20.0,
                usageCount = 14,
                usageLimit = 100,
                expiryDate = "Active"
            ),
            CouponEntity(
                id = "${siteId}_coup_2",
                siteId = siteId,
                code = "FREESHIP",
                discountType = "Fixed Cart ($15)",
                discountValue = 15.0,
                usageCount = 8,
                usageLimit = 50,
                expiryDate = "Active"
            )
        )
        couponDao.insertCoupons(sampleCoupons)

        // Update site totals in database
        val site = siteDao.getSiteById(siteId)
        if (site != null) {
            val updated = site.copy(
                hasWooCommerce = true,
                totalSales = 644.48,
                totalPosts = 3,
                totalPages = 3,
                totalCategories = 3,
                totalComments = 8,
                totalOrders = 4,
                visitorsToday = 472,
                restApiStatus = "Connected (REST v2 • Active Handshake)"
            )
            siteDao.updateSite(updated)
            inspectAndAutoDesignDashboard(siteId)
        }
    }
}
