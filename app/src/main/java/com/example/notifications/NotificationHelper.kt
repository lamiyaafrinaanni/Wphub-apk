package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    val sessionToken = java.util.UUID.randomUUID().toString()

    const val CHANNEL_ID_ORDERS = "wp_channel_orders"
    const val CHANNEL_ID_COMMENTS = "wp_channel_comments"
    const val CHANNEL_ID_STOCK = "wp_channel_stock"
    const val CHANNEL_ID_INQUIRIES = "wp_channel_inquiries"

    const val EXTRA_DESTINATION_TAB = "extra_destination_tab" // "store", "content", "crm", "dashboard"
    const val EXTRA_TARGET_TYPE = "extra_target_type" // "order", "comment", "product", "customer"
    const val EXTRA_TARGET_ID = "extra_target_id"
    const val EXTRA_SITE_ID = "extra_site_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            // Channel 1: WooCommerce Orders
            val ordersChannel = NotificationChannel(
                CHANNEL_ID_ORDERS,
                "WooCommerce Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for incoming customer orders and payments"
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: Post Comments & Discussion
            val commentsChannel = NotificationChannel(
                CHANNEL_ID_COMMENTS,
                "WordPress Comments & Discussions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when visitors or customers post comments on articles and pages"
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 3: Inventory & Low Stock Alerts
            val stockChannel = NotificationChannel(
                CHANNEL_ID_STOCK,
                "Inventory & Low Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when product stock quantities dip below configured thresholds"
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 4: Customer Inquiries & CRM
            val inquiriesChannel = NotificationChannel(
                CHANNEL_ID_INQUIRIES,
                "Customer Inquiries & CRM Leads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when new customer inquiries, contact form entries, or VIP leads arrive"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(ordersChannel, commentsChannel, stockChannel, inquiriesChannel)
            )
        }
    }

    /**
     * Specialized Push Notification for new WooCommerce Orders.
     * Displays Order ID and Total Amount prominently in both collapsed and expanded views.
     */
    fun showWooCommerceOrderNotification(
        context: Context,
        orderId: String,
        orderNumber: String,
        totalAmount: Double,
        currency: String = "$",
        customerName: String,
        itemsSummary: String,
        siteId: String?,
        siteName: String? = null,
        playDefaultSound: Boolean = true
    ) {
        val notificationId = try {
            val digits = orderNumber.filter { it.isDigit() }
            if (digits.isNotEmpty()) digits.takeLast(6).toInt() else (1000..9999).random()
        } catch (e: Exception) {
            (1000..9999).random()
        }

        val formattedAmount = String.format(java.util.Locale.US, "%.2f", totalAmount)
        val title = "🛍️ New WooCommerce Order $orderNumber"
        val shortContent = "Order $orderNumber • $currency$formattedAmount placed by $customerName"

        val expandedContent = buildString {
            appendLine("New customer order placed on ${siteName ?: "WordPress Store"}")
            appendLine("• Order ID: $orderNumber")
            appendLine("• Total Amount: $currency$formattedAmount")
            appendLine("• Customer: $customerName")
            appendLine("• Items: $itemsSummary")
            append("Status: Processing • Tap to view & fulfill")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("session_token", sessionToken)
            putExtra(EXTRA_DESTINATION_TAB, "store")
            putExtra(EXTRA_TARGET_TYPE, "order")
            putExtra(EXTRA_TARGET_ID, orderId)
            putExtra(EXTRA_SITE_ID, siteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ORDERS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stat_notification, "View Order", pendingIntent)

        if (playDefaultSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Notifications blocked or permission denied
        }
    }

    /**
     * Push notification for comments on posts.
     */
    fun showCommentNotification(
        context: Context,
        postId: String?,
        postTitle: String,
        authorName: String,
        commentSnippet: String,
        siteId: String?,
        playDefaultSound: Boolean = true
    ) {
        val notificationId = (2000..3999).random()
        val title = "💬 New Comment from $authorName"
        val shortContent = "On: \"$postTitle\" • $commentSnippet"

        val expandedContent = buildString {
            appendLine("New discussion comment on \"$postTitle\"")
            appendLine("Author: $authorName")
            appendLine("Message: \"$commentSnippet\"")
            append("Tap to review & moderate in WordPress Content Hub.")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("session_token", sessionToken)
            putExtra(EXTRA_DESTINATION_TAB, "content")
            putExtra(EXTRA_TARGET_TYPE, "comment")
            putExtra(EXTRA_TARGET_ID, postId)
            putExtra(EXTRA_SITE_ID, siteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_COMMENTS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stat_notification, "Moderate", pendingIntent)

        if (playDefaultSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    /**
     * Push notification for Low Stock alerts.
     */
    fun showLowStockNotification(
        context: Context,
        productId: String?,
        productName: String,
        sku: String,
        currentStock: Int,
        siteId: String?,
        playDefaultSound: Boolean = true
    ) {
        val notificationId = (4000..5999).random()
        val title = "⚠️ Low Stock Warning: $productName"
        val shortContent = "Only $currentStock units remaining (SKU: $sku). Restock recommended."

        val expandedContent = buildString {
            appendLine("WooCommerce Inventory Alert")
            appendLine("• Product: $productName")
            appendLine("• SKU: $sku")
            appendLine("• Stock Remaining: $currentStock unit(s)")
            append("Tap to update stock level in Store Manager.")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("session_token", sessionToken)
            putExtra(EXTRA_DESTINATION_TAB, "store")
            putExtra(EXTRA_TARGET_TYPE, "product")
            putExtra(EXTRA_TARGET_ID, productId)
            putExtra(EXTRA_SITE_ID, siteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_STOCK)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stat_notification, "Update Stock", pendingIntent)

        if (playDefaultSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    /**
     * Push notification for Customer Inquiries & CRM leads.
     */
    fun showCustomerInquiryNotification(
        context: Context,
        customerId: String?,
        customerName: String,
        customerEmail: String,
        subject: String,
        messageSnippet: String,
        siteId: String?,
        playDefaultSound: Boolean = true
    ) {
        val notificationId = (6000..7999).random()
        val title = "📩 Customer Inquiry from $customerName"
        val shortContent = "Re: $subject • $messageSnippet"

        val expandedContent = buildString {
            appendLine("New CRM Customer Inquiry")
            appendLine("• From: $customerName ($customerEmail)")
            appendLine("• Topic: $subject")
            appendLine("• Message: \"$messageSnippet\"")
            append("Tap to view CRM profile and respond.")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("session_token", sessionToken)
            putExtra(EXTRA_DESTINATION_TAB, "crm")
            putExtra(EXTRA_TARGET_TYPE, "customer")
            putExtra(EXTRA_TARGET_ID, customerId)
            putExtra(EXTRA_SITE_ID, siteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_INQUIRIES)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stat_notification, "Open CRM", pendingIntent)

        if (playDefaultSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        try {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Ignore
        }
    }
}
