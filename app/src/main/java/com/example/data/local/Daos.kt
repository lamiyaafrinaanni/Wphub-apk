package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentSite(): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSiteDirect(): SiteEntity?

    @Query("SELECT * FROM sites WHERE id = :siteId LIMIT 1")
    suspend fun getSiteById(siteId: String): SiteEntity?

    @Query("SELECT * FROM sites")
    suspend fun getAllSitesDirect(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE url = :url OR url = :cleanUrl OR url = :urlWithSlash LIMIT 1")
    suspend fun getSiteByUrl(url: String, cleanUrl: String, urlWithSlash: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity)

    @Query("UPDATE sites SET isCurrent = CASE WHEN id = :siteId THEN 1 ELSE 0 END")
    suspend fun setCurrentSite(siteId: String)

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :siteId")
    suspend fun deleteSite(siteId: String)

    @Query("DELETE FROM sites WHERE id = 'site_demo_store_123' OR id = 'demo_site_seed'")
    suspend fun deleteDemoSites()

    @Query("DELETE FROM sites")
    suspend fun clearAllSites()

    @Query("UPDATE sites SET isAuthenticated = 0, isCurrent = 0")
    suspend fun signOutAllSites()
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE siteId = :siteId ORDER BY dateFormatted DESC")
    fun getPostsForSite(siteId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE siteId = :siteId AND postType = :type ORDER BY dateFormatted DESC")
    fun getPostsByType(siteId: String, type: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM posts WHERE siteId = :siteId")
    suspend fun deletePostsForSite(siteId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE siteId = :siteId ORDER BY name ASC")
    fun getProductsForSite(siteId: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("DELETE FROM products WHERE siteId = :siteId")
    suspend fun deleteProductsForSite(siteId: String)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE siteId = :siteId ORDER BY id DESC")
    fun getOrdersForSite(siteId: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, newStatus: String)

    @Query("DELETE FROM orders WHERE siteId = :siteId")
    suspend fun deleteOrdersForSite(siteId: String)

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE siteId = :siteId ORDER BY totalSpent DESC")
    fun getCustomersForSite(siteId: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("UPDATE customers SET role = :newRole WHERE id = :customerId")
    suspend fun updateCustomerRole(customerId: String, newRole: String)

    @Query("DELETE FROM customers WHERE siteId = :siteId")
    suspend fun deleteCustomersForSite(siteId: String)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins WHERE siteId = :siteId ORDER BY name ASC")
    fun getPluginsForSite(siteId: String): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugins(plugins: List<PluginEntity>)

    @Query("UPDATE plugins SET isActive = :isActive WHERE id = :pluginId")
    suspend fun setPluginActive(pluginId: String, isActive: Boolean)

    @Query("UPDATE plugins SET version = :newVersion, updateAvailable = 0, newVersion = NULL WHERE id = :pluginId")
    suspend fun updatePluginVersion(pluginId: String, newVersion: String)

    @Query("DELETE FROM plugins WHERE siteId = :siteId")
    suspend fun deletePluginsForSite(siteId: String)

    @Query("DELETE FROM plugins")
    suspend fun clearAllPlugins()
}

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons WHERE siteId = :siteId ORDER BY code ASC")
    fun getCouponsForSite(siteId: String): Flow<List<CouponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<CouponEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE siteId = :siteId")
    suspend fun deleteCouponsForSite(siteId: String)

    @Query("DELETE FROM coupons")
    suspend fun clearAllCoupons()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getNotificationsForSite(siteId: String): Flow<List<NotificationItemEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE siteId = :siteId AND isRead = 0")
    fun getUnreadCount(siteId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationItemEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE siteId = :siteId")
    suspend fun markAllAsRead(siteId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)

    @Query("DELETE FROM notifications WHERE siteId = :siteId")
    suspend fun clearAllNotifications(siteId: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotificationsGlobal()
}

@Dao
interface NotificationSettingsDao {
    @Query("SELECT * FROM notification_settings WHERE siteId = :siteId LIMIT 1")
    fun getSettingsForSite(siteId: String): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE siteId = :siteId LIMIT 1")
    suspend fun getSettingsDirect(siteId: String): NotificationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: NotificationSettingsEntity)

    @Query("DELETE FROM notification_settings WHERE siteId = :siteId")
    suspend fun deleteSettingsForSite(siteId: String)

    @Query("DELETE FROM notification_settings")
    suspend fun clearAllSettings()
}

@Dao
interface DashboardWidgetDao {
    @Query("SELECT * FROM dashboard_widgets WHERE siteId = :siteId ORDER BY orderIndex ASC")
    fun getWidgetsForSite(siteId: String): Flow<List<DashboardWidgetEntity>>

    @Query("SELECT * FROM dashboard_widgets WHERE siteId = :siteId ORDER BY orderIndex ASC")
    suspend fun getWidgetsForSiteDirect(siteId: String): List<DashboardWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgets(widgets: List<DashboardWidgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: DashboardWidgetEntity)

    @Update
    suspend fun updateWidget(widget: DashboardWidgetEntity)

    @Query("UPDATE dashboard_widgets SET isEnabled = :isEnabled WHERE id = :widgetId")
    suspend fun toggleWidget(widgetId: String, isEnabled: Boolean)

    @Query("DELETE FROM dashboard_widgets WHERE siteId = :siteId")
    suspend fun deleteWidgetsForSite(siteId: String)

    @Query("DELETE FROM dashboard_widgets")
    suspend fun clearAllWidgets()
}
