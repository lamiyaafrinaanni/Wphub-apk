package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.local.OrderEntity
import com.example.ui.theme.*

/**
 * Status representation for WooCommerce orders with rich UI styling and semantic colors.
 */
enum class WooCommerceOrderStatus(
    val key: String,
    val displayName: String,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val description: String
) {
    PROCESSING(
        key = "processing",
        displayName = "Processing",
        badgeBgColor = Color(0xFFE0E7FF), // Indigo 100
        badgeTextColor = Color(0xFF3730A3), // Indigo 800
        description = "Payment received and order is awaiting fulfillment"
    ),
    COMPLETED(
        key = "completed",
        displayName = "Completed",
        badgeBgColor = EmeraldSuccessBg,
        badgeTextColor = Color(0xFF065F46), // Emerald 800
        description = "Order fulfilled and shipped to customer"
    ),
    ON_HOLD(
        key = "on-hold",
        displayName = "On Hold",
        badgeBgColor = AmberWarningBg,
        badgeTextColor = Color(0xFF92400E), // Amber 800
        description = "Awaiting payment verification or inventory allocation"
    ),
    PENDING(
        key = "pending",
        displayName = "Pending",
        badgeBgColor = Color(0xFFF1F5F9), // Slate 100
        badgeTextColor = Color(0xFF334155), // Slate 700
        description = "Order created, payment pending"
    ),
    CANCELLED(
        key = "cancelled",
        displayName = "Cancelled",
        badgeBgColor = RoseErrorBg,
        badgeTextColor = Color(0xFF991B1B), // Rose 800
        description = "Cancelled by customer or admin"
    ),
    REFUNDED(
        key = "refunded",
        displayName = "Refunded",
        badgeBgColor = Color(0xFFF3E8FF), // Purple 100
        badgeTextColor = Color(0xFF6B21A8), // Purple 800
        description = "Full or partial refund issued"
    ),
    FAILED(
        key = "failed",
        displayName = "Failed",
        badgeBgColor = Color(0xFFFFE4E6), // Rose 100
        badgeTextColor = Color(0xFFBE123C), // Rose 700
        description = "Payment failed or rejected by gateway"
    );

    companion object {
        fun fromKey(rawKey: String?): WooCommerceOrderStatus {
            if (rawKey.isNullOrBlank()) return PROCESSING
            val normalized = rawKey.trim().lowercase()
            return entries.firstOrNull { 
                it.key == normalized || it.displayName.equals(normalized, ignoreCase = true) 
            } ?: PROCESSING
        }
    }
}

/**
 * Enhanced data model representing a WooCommerce order.
 */
data class WooCommerceOrder(
    val id: String,
    val siteId: String,
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String,
    val status: WooCommerceOrderStatus,
    val totalAmount: Double,
    val currency: String = "$",
    val itemsSummary: String,
    val paymentMethod: String = "Stripe / Credit Card",
    val shippingCity: String = "New York, USA",
    val dateFormatted: String,
    val itemCount: Int = 1,
    val isPaid: Boolean = true,
    val customerNote: String? = null
) {
    val formattedTotal: String
        get() = "$currency%.2f".format(totalAmount)

    fun toEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            siteId = siteId,
            orderNumber = orderNumber,
            customerName = customerName,
            customerEmail = customerEmail,
            status = status.key,
            totalAmount = totalAmount,
            currency = currency,
            itemsSummary = itemsSummary,
            paymentMethod = paymentMethod,
            shippingCity = shippingCity,
            dateFormatted = dateFormatted
        )
    }

    companion object {
        fun fromEntity(entity: OrderEntity): WooCommerceOrder {
            // Count items from itemsSummary by splitting on comma or '+'
            val guessedCount = runCatching {
                if (entity.itemsSummary.contains(",")) {
                    entity.itemsSummary.split(",").size
                } else if (entity.itemsSummary.contains("+")) {
                    entity.itemsSummary.split("+").size
                } else {
                    1
                }
            }.getOrDefault(1)

            return WooCommerceOrder(
                id = entity.id,
                siteId = entity.siteId,
                orderNumber = entity.orderNumber,
                customerName = entity.customerName,
                customerEmail = entity.customerEmail,
                status = WooCommerceOrderStatus.fromKey(entity.status),
                totalAmount = entity.totalAmount,
                currency = entity.currency.ifBlank { "$" },
                itemsSummary = entity.itemsSummary,
                paymentMethod = entity.paymentMethod,
                shippingCity = entity.shippingCity,
                dateFormatted = entity.dateFormatted,
                itemCount = maxOf(1, guessedCount),
                isPaid = !entity.status.equals("cancelled", ignoreCase = true) && !entity.status.equals("failed", ignoreCase = true)
            )
        }
    }
}

/**
 * Filter and sort configuration for the WooCommerce orders list interface.
 */
enum class WooOrderSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Total"),
    AMOUNT_ASC("Lowest Total"),
    CUSTOMER_ASC("Customer (A-Z)")
}

data class WooOrderFilterState(
    val statusFilter: WooCommerceOrderStatus? = null, // null means "All"
    val searchQuery: String = "",
    val sortOption: WooOrderSortOption = WooOrderSortOption.DATE_DESC
)
