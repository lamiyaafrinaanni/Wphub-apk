package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.*
import com.example.data.repository.WPHubRepository
import com.example.notifications.NotificationHelper
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class HubTab(val title: String) {
    DASHBOARD("Dashboard"),
    STORE("WooCommerce"),
    CONTENT("Content"),
    CRM("CRM & Users"),
    TOOLS("Admin & Tools")
}

class WPHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WPHubRepository
    private val themeManager = ThemeManager.getInstance(application)
    val themeMode: StateFlow<AppThemeMode> = themeManager.themeMode

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WPHubRepository(db)
        NotificationHelper.createNotificationChannels(application)
        viewModelScope.launch {
            repository.removeDemoSites()
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        themeManager.setThemeMode(mode)
        _userMessage.value = "Theme: ${mode.title}"
    }

    fun toggleThemeMode() {
        val next = themeManager.toggleThemeMode()
        _userMessage.value = "Theme: ${next.title}"
    }

    val currentSite: StateFlow<SiteEntity?> = repository.getCurrentSite()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSites: StateFlow<List<SiteEntity>> = repository.getAllSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(HubTab.DASHBOARD)
    val selectedTab: StateFlow<HubTab> = _selectedTab.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _showReconnectDialog = MutableStateFlow(false)
    val showReconnectDialog: StateFlow<Boolean> = _showReconnectDialog.asStateFlow()

    private val _reconnectSite = MutableStateFlow<SiteEntity?>(null)
    val reconnectSite: StateFlow<SiteEntity?> = _reconnectSite.asStateFlow()

    private val _reconnectErrorMessage = MutableStateFlow<String?>(null)
    val reconnectErrorMessage: StateFlow<String?> = _reconnectErrorMessage.asStateFlow()

    // Deep navigation / item highlighting targets from actionable notifications
    private val _targetOrderId = MutableStateFlow<String?>(null)
    val targetOrderId: StateFlow<String?> = _targetOrderId.asStateFlow()

    private val _targetPostId = MutableStateFlow<String?>(null)
    val targetPostId: StateFlow<String?> = _targetPostId.asStateFlow()

    private val _targetProductId = MutableStateFlow<String?>(null)
    val targetProductId: StateFlow<String?> = _targetProductId.asStateFlow()

    // Filters
    private val _orderFilter = MutableStateFlow("all")
    val orderFilter: StateFlow<String> = _orderFilter.asStateFlow()

    private val _contentFilter = MutableStateFlow("all") // "all", "post", "page"
    val contentFilter: StateFlow<String> = _contentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val posts: StateFlow<List<PostEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getPostsForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getProductsForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<OrderEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getOrdersForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<CustomerEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getCustomersForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val plugins: StateFlow<List<PluginEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getPluginsForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val coupons: StateFlow<List<CouponEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getCouponsForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Push Notifications
    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationItemEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getNotificationsForSite(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadNotificationCount: StateFlow<Int> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getUnreadNotificationCount(site.id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationSettings: StateFlow<NotificationSettingsEntity?> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getNotificationSettings(site.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic Customizable Dashboard Widgets
    @OptIn(ExperimentalCoroutinesApi::class)
    val dashboardWidgets: StateFlow<List<DashboardWidgetEntity>> = currentSite.flatMapLatest { site ->
        if (site != null) repository.getDashboardWidgets(site.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasWooCommerce: StateFlow<Boolean> = currentSite.map { it?.hasWooCommerce == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun selectTab(tab: HubTab) {
        _selectedTab.value = tab
    }

    fun clearTargetOrder() {
        _targetOrderId.value = null
    }

    fun clearTargetPost() {
        _targetPostId.value = null
    }

    fun clearTargetProduct() {
        _targetProductId.value = null
    }

    fun handleNotificationNavigation(
        destinationTab: String?,
        targetType: String?,
        targetId: String?,
        siteId: String?
    ) {
        viewModelScope.launch {
            if (siteId != null && siteId != currentSite.value?.id) {
                repository.switchSite(siteId)
            }
            when (destinationTab?.lowercase()) {
                "store" -> {
                    _selectedTab.value = HubTab.STORE
                    if (targetType == "order" && targetId != null) {
                        _targetOrderId.value = targetId
                    } else if (targetType == "product" && targetId != null) {
                        _targetProductId.value = targetId
                    }
                }
                "content" -> {
                    _selectedTab.value = HubTab.CONTENT
                    if (targetId != null) {
                        _targetPostId.value = targetId
                    }
                }
                "dashboard" -> {
                    _selectedTab.value = HubTab.DASHBOARD
                }
                "tools" -> {
                    _selectedTab.value = HubTab.TOOLS
                }
                "crm" -> {
                    _selectedTab.value = HubTab.CRM
                }
                else -> {
                    // Fallback based on target type
                    when (targetType) {
                        "order" -> {
                            _selectedTab.value = HubTab.STORE
                            _targetOrderId.value = targetId
                        }
                        "product" -> {
                            _selectedTab.value = HubTab.STORE
                            _targetProductId.value = targetId
                        }
                        "comment" -> {
                            _selectedTab.value = HubTab.CONTENT
                            _targetPostId.value = targetId
                        }
                        else -> _selectedTab.value = HubTab.DASHBOARD
                    }
                }
            }
        }
    }

    fun onNotificationClicked(notification: NotificationItemEntity) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notification.id)
            when (notification.targetType) {
                "order" -> {
                    _selectedTab.value = HubTab.STORE
                    _targetOrderId.value = notification.targetId
                }
                "comment" -> {
                    _selectedTab.value = HubTab.CONTENT
                    _targetPostId.value = notification.targetId
                }
                "product" -> {
                    _selectedTab.value = HubTab.STORE
                    _targetProductId.value = notification.targetId
                }
                else -> {
                    _selectedTab.value = HubTab.DASHBOARD
                }
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            repository.markAllNotificationsAsRead(site.id)
            _userMessage.value = "All notifications marked as read"
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            repository.clearAllNotifications(site.id)
            _userMessage.value = "Notification history cleared"
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun updateNotificationSettings(settings: NotificationSettingsEntity) {
        viewModelScope.launch {
            repository.saveNotificationSettings(settings)
            _userMessage.value = "Notification settings saved"
        }
    }

    // Trigger simulation alerts
    fun triggerSimulatedOrderAlert() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val settings = repository.getNotificationSettingsDirect(site.id)
            if (settings?.ordersEnabled == false) {
                _userMessage.value = "Orders alerts are disabled in Settings"
                return@launch
            }

            val randomNum = (1044..1999).random()
            val amounts = listOf(89.00, 149.50, 279.00, 420.00, 69.95)
            val buyers = listOf("Alexander Wright", "Chloe Bennett", "Liam O'Connor", "Maya Patel", "David Kim")
            val items = listOf("Wireless ANC Headphones", "Mechanical Keyboard", "USB-C Fast Charging Dock", "Desk Organizer Pad")
            val buyer = buyers.random()
            val amount = amounts.random()
            val item = items.random()
            val orderId = "${site.id}_ord_${System.currentTimeMillis()}"

            // Insert new order in DB
            val newOrder = OrderEntity(
                id = orderId,
                siteId = site.id,
                orderNumber = "#$randomNum",
                customerName = buyer,
                customerEmail = "${buyer.lowercase().replace(" ", ".")}@example.com",
                status = "processing",
                totalAmount = amount,
                itemsSummary = item,
                dateFormatted = "Just now"
            )
            repository.insertOrder(newOrder)

            // Create Notification
            val notifId = (1000..9999).random()
            val notifTitle = "🎉 New WooCommerce Order #$randomNum"
            val notifMsg = "$buyer placed order #$randomNum for $amount ($item). Awaiting fulfillment."

            val notifEntity = NotificationItemEntity(
                id = "${site.id}_notif_${System.currentTimeMillis()}",
                siteId = site.id,
                type = "order",
                title = notifTitle,
                message = notifMsg,
                targetType = "order",
                targetId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                extraData = "$amount • Processing"
            )
            repository.saveNotification(notifEntity)

            // Post Android System Notification with key order details: Order ID and Total Amount
            NotificationHelper.showWooCommerceOrderNotification(
                context = getApplication(),
                orderId = orderId,
                orderNumber = "#$randomNum",
                totalAmount = amount,
                currency = "$",
                customerName = buyer,
                itemsSummary = item,
                siteId = site.id,
                siteName = site.name,
                playDefaultSound = settings?.soundEnabled ?: true
            )
            _userMessage.value = "New WooCommerce Order #$randomNum ($$amount) received! Push notification sent."
        }
    }

    /**
     * Places a new WooCommerce customer order on the connected site and sends
     * an instant push notification including key order details (Order ID and Total Amount).
     */
    fun placeWooCommerceOrder(
        customerName: String,
        customerEmail: String,
        itemsSummary: String,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val settings = repository.getNotificationSettingsDirect(site.id)

            val randomNum = (2000..9999).random()
            val orderId = "${site.id}_ord_${System.currentTimeMillis()}"
            val finalName = customerName.ifBlank { "Valued Customer" }
            val finalEmail = customerEmail.ifBlank { "customer@example.com" }
            val finalItems = itemsSummary.ifBlank { "WordPress Store Products" }

            val newOrder = OrderEntity(
                id = orderId,
                siteId = site.id,
                orderNumber = "#$randomNum",
                customerName = finalName,
                customerEmail = finalEmail,
                status = "processing",
                totalAmount = totalAmount,
                itemsSummary = finalItems,
                dateFormatted = "Just now"
            )
            repository.insertOrder(newOrder)

            // Save in-app notification
            val notifTitle = "🛍️ New WooCommerce Order #$randomNum"
            val notifMsg = "$finalName placed order #$randomNum for $${String.format(java.util.Locale.US, "%.2f", totalAmount)} ($finalItems)"
            val notifEntity = NotificationItemEntity(
                id = "${site.id}_notif_${System.currentTimeMillis()}",
                siteId = site.id,
                type = "order",
                title = notifTitle,
                message = notifMsg,
                targetType = "order",
                targetId = orderId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                extraData = "$$totalAmount • Processing"
            )
            repository.saveNotification(notifEntity)

            // Send Push Notification to device
            NotificationHelper.showWooCommerceOrderNotification(
                context = getApplication(),
                orderId = orderId,
                orderNumber = "#$randomNum",
                totalAmount = totalAmount,
                currency = "$",
                customerName = finalName,
                itemsSummary = finalItems,
                siteId = site.id,
                siteName = site.name,
                playDefaultSound = settings?.soundEnabled ?: true
            )
            _userMessage.value = "Order #$randomNum placed! Push notification dispatched with ID & Total ($$totalAmount)"
        }
    }

    fun triggerSimulatedCommentAlert() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val settings = repository.getNotificationSettingsDirect(site.id)
            if (settings?.commentsEnabled == false) {
                _userMessage.value = "Comment alerts are disabled in Settings"
                return@launch
            }

            val postList = repository.getPostsForSite(site.id).firstOrNull() ?: emptyList()
            val targetPost = postList.firstOrNull { it.status == "published" } ?: postList.firstOrNull()
            val postTitle = targetPost?.title ?: "Launching Our Next-Gen Flagship Audio"
            val postId = targetPost?.id

            val commenters = listOf("Sarah Jenkins", "Devon Miles", "Oliver Chen", "Priya Sharma")
            val comments = listOf(
                "Great review! Does this firmware release include support for LE Audio and LC3 codec?",
                "Will there be an option to purchase extended warranty for EU customers?",
                "Appreciate the in-depth comparison. Really helped our design studio make a choice!",
                "Are these compatible with standard 75mm VESA mounts?"
            )
            val commenter = commenters.random()
            val commentText = comments.random()

            val notifId = (1000..9999).random()
            val notifTitle = "💬 New Comment on '$postTitle'"
            val notifMsg = "$commenter: \"$commentText\""

            val notifEntity = NotificationItemEntity(
                id = "${site.id}_notif_${System.currentTimeMillis()}",
                siteId = site.id,
                type = "comment",
                title = notifTitle,
                message = notifMsg,
                targetType = "comment",
                targetId = postId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                extraData = "$commenter • Discussion"
            )
            repository.saveNotification(notifEntity)

            NotificationHelper.showCommentNotification(
                context = getApplication(),
                postId = postId,
                postTitle = postTitle,
                authorName = commenter,
                commentSnippet = commentText,
                siteId = site.id,
                playDefaultSound = settings?.soundEnabled ?: true
            )
            _userMessage.value = "New comment alert dispatched!"
        }
    }

    fun triggerSimulatedLowStockAlert() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val settings = repository.getNotificationSettingsDirect(site.id)
            if (settings?.lowStockEnabled == false) {
                _userMessage.value = "Low stock alerts are disabled in Settings"
                return@launch
            }

            val productList = repository.getProductsForSite(site.id).firstOrNull() ?: emptyList()
            val threshold = settings?.lowStockThreshold ?: 5
            val targetProduct = productList.firstOrNull()
            val prodName = targetProduct?.name ?: "Featured Store Product"
            val prodId = targetProduct?.id ?: "${site.id}_prod_stock_test"
            val remainingStock = (1..threshold).random()

            // Update product stock in DB
            targetProduct?.let {
                val updated = it.copy(
                    stockQuantity = remainingStock,
                    stockStatus = if (remainingStock <= 0) "outofstock" else "lowstock"
                )
                repository.saveProduct(updated)
            }

            val notifTitle = "⚠️ Low Stock Alert: $prodName"
            val notifMsg = "Inventory dipped to $remainingStock units remaining (Threshold is ≤ $threshold units). Restock advised."

            val notifEntity = NotificationItemEntity(
                id = "${site.id}_notif_${System.currentTimeMillis()}",
                siteId = site.id,
                type = "stock",
                title = notifTitle,
                message = notifMsg,
                targetType = "product",
                targetId = prodId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                extraData = "$remainingStock in stock (Threshold ≤ $threshold)"
            )
            repository.saveNotification(notifEntity)

            NotificationHelper.showLowStockNotification(
                context = getApplication(),
                productId = prodId,
                productName = prodName,
                sku = targetProduct?.sku ?: "SKU-APX-01",
                currentStock = remainingStock,
                siteId = site.id,
                playDefaultSound = settings?.soundEnabled ?: true
            )
            _userMessage.value = "Low stock alert ($remainingStock left) dispatched!"
        }
    }

    fun triggerSimulatedInquiryAlert() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val settings = repository.getNotificationSettingsDirect(site.id)
            if (settings?.customerInquiriesEnabled == false) {
                _userMessage.value = "Customer inquiry alerts are disabled in Settings"
                return@launch
            }

            val customerList = repository.getCustomersForSite(site.id).firstOrNull() ?: emptyList()
            val targetCustomer = customerList.randomOrNull()
            val custName = targetCustomer?.name ?: "Eleanor Vance"
            val custEmail = targetCustomer?.email ?: "eleanor.vance@example.com"
            val custId = targetCustomer?.id

            val inquiries = listOf(
                "Enterprise Licensing" to "Looking to deploy your WooCommerce extension across 14 multisite instances. Could you provide a volume quotation?",
                "Custom Integration" to "Inquiring about REST API webhooks for automated ERP stock synchronization.",
                "Order Support" to "Requested expedited DHL shipping change for recent wholesale purchase."
            )
            val (subject, messageText) = inquiries.random()

            val notifTitle = "📩 Customer Inquiry: $subject"
            val notifMsg = "$custName ($custEmail): \"$messageText\""

            val notifEntity = NotificationItemEntity(
                id = "${site.id}_notif_${System.currentTimeMillis()}",
                siteId = site.id,
                type = "inquiry",
                title = notifTitle,
                message = notifMsg,
                targetType = "customer",
                targetId = custId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                extraData = "$custName • Inquiry"
            )
            repository.saveNotification(notifEntity)

            NotificationHelper.showCustomerInquiryNotification(
                context = getApplication(),
                customerId = custId,
                customerName = custName,
                customerEmail = custEmail,
                subject = subject,
                messageSnippet = messageText,
                siteId = site.id,
                playDefaultSound = settings?.soundEnabled ?: true
            )
            _userMessage.value = "Customer inquiry from $custName dispatched!"
        }
    }

    fun setOrderFilter(filter: String) {
        _orderFilter.value = filter
    }

    fun setContentFilter(filter: String) {
        _contentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun switchSite(siteId: String) {
        viewModelScope.launch {
            repository.switchSite(siteId)
            val updated = repository.getCurrentSite().firstOrNull()
            if (updated?.hasWooCommerce == false && _selectedTab.value == HubTab.STORE) {
                _selectedTab.value = HubTab.DASHBOARD
            }
            _userMessage.value = "Switched to ${updated?.name ?: "WordPress Site"}"
        }
    }

    fun syncCurrentSite() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            _isSyncing.value = true
            try {
                val msg = repository.syncLiveSiteData(site.id)
                _isSyncing.value = false
                _userMessage.value = msg
            } catch (e: com.example.data.remote.WordPressUnauthorizedException) {
                _isSyncing.value = false
                val siteToReconnect = site
                _reconnectSite.value = siteToReconnect
                _showReconnectDialog.value = true
                _userMessage.value = "HTTP 401 Unauthorized: Connection rejected for ${siteToReconnect.name}. Reconnect dialog opened."
            } catch (e: Exception) {
                _isSyncing.value = false
                _userMessage.value = e.message ?: "Failed to sync WordPress data"
            }
        }
    }

    fun triggerReconnectDialog(site: SiteEntity? = currentSite.value) {
        val target = site ?: currentSite.value
        if (target != null) {
            _reconnectErrorMessage.value = null
            _reconnectSite.value = target
            _showReconnectDialog.value = true
        }
    }

    fun dismissReconnectDialog() {
        _showReconnectDialog.value = false
        _reconnectSite.value = null
        _reconnectErrorMessage.value = null
    }

    fun updateSiteCredentials(
        siteId: String,
        username: String,
        appPasswordToken: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            val site = repository.getSiteById(siteId)
            val siteUrl = site?.url ?: ""

            val testResult = repository.performLiveConnectionChecks(
                siteUrl = siteUrl,
                username = username,
                tokenOrPass = appPasswordToken
            )

            if (testResult.isSuccess || testResult.steps.any { it.stepIndex == 3 && it.state == com.example.data.remote.VerificationStepState.SUCCESS }) {
                repository.updateSiteCredentials(siteId, username, appPasswordToken)
                _showReconnectDialog.value = false
                _reconnectSite.value = null
                _reconnectErrorMessage.value = null
                _isSyncing.value = false
                _userMessage.value = "Site reconnected successfully! Syncing live data..."
                syncCurrentSite()
                onSuccess()
            } else {
                _isSyncing.value = false
                val errorMsg = testResult.errorMessage ?: "Authentication failed with updated credentials."
                _reconnectErrorMessage.value = errorMsg
                _userMessage.value = errorMsg
                onError(errorMsg)
            }
        }
    }

    fun toggleDashboardWidget(widgetId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleDashboardWidget(widgetId, isEnabled)
        }
    }

    fun saveDashboardWidgets(widgets: List<DashboardWidgetEntity>) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            repository.saveDashboardWidgets(site.id, widgets)
            _userMessage.value = "Custom dashboard layout saved"
        }
    }

    fun resetDashboardToAutoDesigned() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            _isSyncing.value = true
            delay(600)
            repository.resetDashboardToAutoDesigned(site.id)
            _isSyncing.value = false
            _userMessage.value = "Dashboard re-designed automatically for ${site.name}"
        }
    }

    fun inspectAndAutoDesignCurrentSite() {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            _isSyncing.value = true
            delay(1000)
            repository.inspectAndAutoDesignDashboard(site.id)
            _isSyncing.value = false
            _userMessage.value = "Site scan complete! Dashboard generated for ${site.siteType.replaceFirstChar { it.uppercase() }} Archetype."
        }
    }

    fun quickSaveDraft(title: String, content: String) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            if (title.isBlank() && content.isBlank()) {
                _userMessage.value = "Please enter a draft title or content"
                return@launch
            }
            val postId = "${site.id}_draft_${System.currentTimeMillis()}"
            val post = PostEntity(
                id = postId,
                siteId = site.id,
                title = title.ifBlank { "Untitled Quick Draft" },
                excerpt = content.take(120),
                content = """[{"type":"paragraph","text":"${content.replace("\"", "\\\"")}"}]""",
                status = "draft",
                postType = "post",
                authorName = site.userDisplayName,
                category = "General",
                dateFormatted = "Just now",
                commentCount = 0,
                viewCount = 0
            )
            repository.savePost(post)
            _userMessage.value = "Draft \"${post.title}\" saved locally!"
        }
    }

    private val _isVerifyingConnection = MutableStateFlow(false)
    val isVerifyingConnection: StateFlow<Boolean> = _isVerifyingConnection.asStateFlow()

    private val _liveVerificationSteps = MutableStateFlow<List<LiveVerificationStep>>(emptyList())
    val liveVerificationSteps: StateFlow<List<LiveVerificationStep>> = _liveVerificationSteps.asStateFlow()

    private val _connectionVerificationResult = MutableStateFlow<MultiStepConnectionResult?>(null)
    val connectionVerificationResult: StateFlow<MultiStepConnectionResult?> = _connectionVerificationResult.asStateFlow()

    fun clearVerificationState() {
        _isVerifyingConnection.value = false
        _liveVerificationSteps.value = emptyList()
        _connectionVerificationResult.value = null
    }

    fun runSimpleRestConnectionTest(siteUrl: String, onResult: (com.example.data.remote.RestTestResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.runSimpleRestConnectionTest(siteUrl)
            onResult(result)
        }
    }

    fun verifyAndConnectWordPressSite(
        siteUrl: String,
        username: String,
        appPasswordOrToken: String,
        autoSaveOnSuccess: Boolean = true,
        onSuccess: (SiteEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanUrl = siteUrl.trim().let {
                if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
            }
            if (siteUrl.isBlank()) {
                onError("Please enter your WordPress site URL")
                _userMessage.value = "Site URL cannot be empty"
                return@launch
            }
            if (username.isBlank()) {
                onError("Please enter your WordPress username")
                _userMessage.value = "Username is required"
                return@launch
            }
            if (appPasswordOrToken.isBlank()) {
                onError("Please enter your Application Password")
                _userMessage.value = "Application password is required"
                return@launch
            }

            _isVerifyingConnection.value = true
            _connectionVerificationResult.value = null

            val initialSteps = listOf(
                LiveVerificationStep(
                    stepIndex = 1,
                    name = "1. URL Validation",
                    description = "Verifying URL format, DNS resolution and TLS/SSL certificate...",
                    state = VerificationStepState.IDLE,
                    subLogs = listOf("Pending URL format & host reachability check")
                ),
                LiveVerificationStep(
                    stepIndex = 2,
                    name = "2. REST API Discovery",
                    description = "Discovering /wp-json/ endpoints, namespaces & permalink settings...",
                    state = VerificationStepState.IDLE,
                    subLogs = listOf("Pending /wp-json/ discovery & namespace check")
                ),
                LiveVerificationStep(
                    stepIndex = 3,
                    name = "3. Authentication Handshake",
                    description = "Validating Application Password credentials & user capabilities...",
                    state = VerificationStepState.IDLE,
                    subLogs = listOf("Pending user credentials & capability verification")
                )
            )
            _liveVerificationSteps.value = initialSteps

            val currentStepsMap = initialSteps.associateBy { it.stepIndex }.toMutableMap()

            val result = repository.performLiveConnectionChecks(
                siteUrl = cleanUrl,
                username = username,
                tokenOrPass = appPasswordOrToken,
                onStepUpdate = { updatedStep ->
                    currentStepsMap[updatedStep.stepIndex] = updatedStep
                    _liveVerificationSteps.value = currentStepsMap.values.sortedBy { it.stepIndex }
                }
            )

            _connectionVerificationResult.value = result
            _isVerifyingConnection.value = false

            if (result.isSuccess) {
                _userMessage.value = "Verified! Connected to ${result.siteName} as ${result.userDisplayName} (${result.userRole})"
                if (autoSaveOnSuccess) {
                    _isSyncing.value = true
                    try {
                        val site = repository.loginToWordPressSite(
                            siteId = null,
                            siteUrl = result.siteUrl,
                            siteName = result.siteName,
                            usernameOrEmail = username,
                            passwordOrToken = appPasswordOrToken,
                            role = result.userRole,
                            displayName = result.userDisplayName
                        )
                        _isSyncing.value = false
                        onSuccess(site)
                    } catch (e: Exception) {
                        _isSyncing.value = false
                        onError("Saved connection but initial sync encountered: ${e.message}")
                    }
                }
            } else {
                onError(result.errorMessage ?: "Verification failed")
                _userMessage.value = "Connection check failed: ${result.errorMessage}"
            }
        }
    }

    fun connectWithDemoSite(onSuccess: (SiteEntity) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(500)
            val demo = repository.loginToWordPressSite(
                siteId = "site_demo_apex",
                siteUrl = "https://apex-wp-store.local",
                siteName = "Apex WP Store & Studio",
                usernameOrEmail = "admin",
                passwordOrToken = "demo-app-pass-token-2026",
                role = "Administrator",
                displayName = "Site Administrator"
            )
            _isSyncing.value = false
            _userMessage.value = "Connected to Apex WP Demo Site!"
            onSuccess(demo)
        }
    }

    fun loginWithWordPress(
        siteId: String?,
        siteUrl: String,
        siteName: String,
        usernameOrEmail: String,
        passwordOrToken: String,
        role: String = "Administrator",
        rememberMe: Boolean = true,
        onSuccess: (SiteEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (siteUrl.isBlank()) {
                onError("Please enter your WordPress site URL")
                _userMessage.value = "Site URL cannot be empty"
                return@launch
            }
            if (usernameOrEmail.isBlank()) {
                onError("Please enter your WordPress username or email")
                _userMessage.value = "Username or email is required"
                return@launch
            }
            if (passwordOrToken.isBlank()) {
                onError("Please enter your WordPress password")
                _userMessage.value = "Password is required"
                return@launch
            }

            _isSyncing.value = true
            // Simulate realistic WordPress REST API authentication handshake (token exchange & user profile query)
            delay(1100)
            try {
                val site = repository.loginToWordPressSite(
                    siteId = siteId,
                    siteUrl = siteUrl,
                    siteName = siteName,
                    usernameOrEmail = usernameOrEmail,
                    passwordOrToken = passwordOrToken,
                    role = role
                )
                _isSyncing.value = false
                _userMessage.value = "Logged in as ${site.username} (${site.userRole}) on ${site.name}"
                onSuccess(site)
            } catch (e: Exception) {
                _isSyncing.value = false
                onError(e.message ?: "Authentication failed")
                _userMessage.value = "WordPress login failed: ${e.localizedMessage}"
            }
        }
    }

    fun logoutCurrentSite() {
        viewModelScope.launch {
            val current = currentSite.value ?: return@launch
            repository.logoutSite(current.id)
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.CookieManager.getInstance().flush()
                android.webkit.WebStorage.getInstance().deleteAllData()
            } catch (e: Exception) {
                // Ignore WebView clean failure if running in test environment
            }
            _userMessage.value = "Logged out from ${current.name}"
        }
    }

    fun removeDemoSites() {
        viewModelScope.launch {
            repository.removeDemoSites()
            _userMessage.value = "All demo sites have been removed."
        }
    }

    fun signOutAllAndClearDemoData() {
        viewModelScope.launch {
            repository.clearAllDataAndSignOut()
            _userMessage.value = "Signed out of all sites. All demo and mock data removed."
        }
    }

    fun signOutAll() {
        viewModelScope.launch {
            repository.signOutAllSessions()
            _userMessage.value = "Signed out of all WordPress sites."
        }
    }

    fun addNewSite(name: String, url: String, username: String, appPassword: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(900)
            val success = repository.addNewSite(name, url, username, appPassword)
            _isSyncing.value = false
            if (success) {
                _userMessage.value = "Connected to $name via Application Password"
            } else {
                _userMessage.value = "Failed to connect to $name. Please verify credentials."
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            _userMessage.value = "Order status updated to ${newStatus.replaceFirstChar { it.uppercase() }}"
        }
    }

    fun savePost(
        id: String?,
        title: String,
        excerpt: String,
        content: String,
        status: String,
        postType: String,
        category: String,
        featuredImageUrl: String? = null
    ) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val postId = id ?: "${site.id}_${postType}_${System.currentTimeMillis()}"
            val post = PostEntity(
                id = postId,
                siteId = site.id,
                title = title.ifBlank { "Untitled $postType" },
                excerpt = excerpt,
                content = content,
                status = status,
                postType = postType,
                authorName = site.userDisplayName.ifBlank { "Admin" },
                category = category.ifBlank { "General" },
                dateFormatted = "Just now",
                commentCount = 0,
                viewCount = 0,
                featuredImageUrl = featuredImageUrl
            )
            repository.savePost(post)
            _userMessage.value = if (status == "published") "${postType.replaceFirstChar { it.uppercase() }} published live!" else "Draft saved locally"
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            _userMessage.value = "Item moved to trash"
        }
    }

    fun saveProduct(
        id: String?,
        name: String,
        sku: String,
        regularPrice: Double,
        salePrice: Double?,
        stockQuantity: Int,
        category: String,
        productType: String
    ) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val stockStatus = when {
                stockQuantity <= 0 -> "outofstock"
                stockQuantity <= 5 -> "lowstock"
                else -> "instock"
            }
            val prodId = id ?: "${site.id}_prod_${System.currentTimeMillis()}"
            val product = ProductEntity(
                id = prodId,
                siteId = site.id,
                name = name.ifBlank { "New Product" },
                sku = sku.ifBlank { "APX-NEW-${(100..999).random()}" },
                regularPrice = regularPrice,
                salePrice = salePrice,
                stockStatus = stockStatus,
                stockQuantity = stockQuantity,
                category = category.ifBlank { "General" },
                productType = productType,
                salesCount = 0
            )
            repository.saveProduct(product)
            _userMessage.value = "Product catalog updated"
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _userMessage.value = "Product deleted"
        }
    }

    fun togglePlugin(pluginId: String, currentActive: Boolean) {
        viewModelScope.launch {
            repository.togglePlugin(pluginId, !currentActive)
            _userMessage.value = if (!currentActive) "Plugin activated" else "Plugin deactivated"
        }
    }

    fun updatePlugin(pluginId: String, newVersion: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(800)
            repository.updatePlugin(pluginId, newVersion)
            _isSyncing.value = false
            _userMessage.value = "Plugin successfully updated to v$newVersion"
        }
    }

    fun updateCustomerRole(customerId: String, newRole: String) {
        viewModelScope.launch {
            repository.updateCustomerRole(customerId, newRole)
            _userMessage.value = "User role updated to $newRole"
        }
    }

    fun saveCoupon(code: String, discountType: String, amount: Double, limit: Int) {
        viewModelScope.launch {
            val site = currentSite.value ?: return@launch
            val coupon = CouponEntity(
                id = "${site.id}_coup_${System.currentTimeMillis()}",
                siteId = site.id,
                code = code.uppercase().trim(),
                discountType = discountType,
                discountValue = amount,
                usageCount = 0,
                usageLimit = limit,
                expiryDate = "2026-12-31"
            )
            repository.saveCoupon(coupon)
            _userMessage.value = "Discount coupon ${coupon.code} created"
        }
    }
}
