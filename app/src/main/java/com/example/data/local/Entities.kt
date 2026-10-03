package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val iconEmoji: String = "🌐",
    val sslEnabled: Boolean = true,
    val restApiStatus: String = "Not Connected",
    val isCurrent: Boolean = false,
    val totalSales: Double = 0.0,
    val totalPosts: Int = 0,
    val totalPages: Int = 0,
    val totalCategories: Int = 0,
    val totalComments: Int = 0,
    val totalOrders: Int = 0,
    val visitorsToday: Int = 0,
    val lastSyncTime: String = "Never",
    val username: String = "",
    val userEmail: String = "",
    val userDisplayName: String = "",
    val userRole: String = "Administrator",
    val appPasswordToken: String = "",
    val isAuthenticated: Boolean = false,
    // Site Archetype & Inspection Properties
    val siteType: String = "custom", // "ecommerce", "blog", "corporate", "portfolio", "custom"
    val hasWooCommerce: Boolean = false,
    val activeTheme: String = "",
    val activeThemeVersion: String = "",
    val wpVersion: String = "",
    val phpVersion: String = "",
    val tagline: String = "",
    val siteInspectionReport: String? = null
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val title: String,
    val excerpt: String,
    val content: String, // Gutenberg block-structured content
    val status: String, // "published", "draft", "scheduled"
    val postType: String, // "post", "page"
    val authorName: String,
    val category: String,
    val dateFormatted: String,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val featuredImageUrl: String? = null
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
    val sku: String,
    val regularPrice: Double,
    val salePrice: Double? = null,
    val stockStatus: String, // "instock", "lowstock", "outofstock"
    val stockQuantity: Int,
    val category: String,
    val productType: String = "Simple Product",
    val imageUrl: String? = null,
    val salesCount: Int = 0
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String,
    val status: String, // "processing", "completed", "on-hold", "cancelled"
    val totalAmount: Double,
    val currency: String = "$",
    val itemsSummary: String,
    val paymentMethod: String = "Stripe / Credit Card",
    val shippingCity: String = "New York, USA",
    val dateFormatted: String
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
    val email: String,
    val role: String, // "Customer", "Administrator", "Editor", "Author", "Subscriber"
    val totalSpent: Double,
    val ordersCount: Int,
    val lastActive: String,
    val avatarInitials: String
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
    val slug: String,
    val version: String,
    val updateAvailable: Boolean = false,
    val newVersion: String? = null,
    val isActive: Boolean = true,
    val author: String = "WordPress Community",
    val description: String
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val code: String,
    val discountType: String, // "Percentage (20%)", "Fixed Cart ($15)"
    val discountValue: Double,
    val usageCount: Int,
    val usageLimit: Int,
    val expiryDate: String
)

@Entity(tableName = "notifications")
data class NotificationItemEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val type: String, // "order", "comment", "stock", "inquiry"
    val title: String,
    val message: String,
    val targetType: String, // "order", "comment", "product", "customer"
    val targetId: String?, // orderId, postId, productId, customerId
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val extraData: String? = null
)

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val siteId: String,
    val ordersEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val lowStockEnabled: Boolean = true,
    val customerInquiriesEnabled: Boolean = true,
    val lowStockThreshold: Int = 5,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

@Entity(tableName = "dashboard_widgets")
data class DashboardWidgetEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val widgetKey: String, // "site_banner", "connection_health", "quick_stats", "woo_sales", "woo_orders", "quick_draft", "recent_content", "recent_comments", "quick_actions", "theme_overview", "plugins_health", "rest_api"
    val title: String,
    val description: String = "",
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0,
    val isWooCommerceOnly: Boolean = false
)
