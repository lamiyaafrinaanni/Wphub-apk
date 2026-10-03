package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.model.WooCommerceOrder
import com.example.data.model.WooCommerceOrderStatus
import com.example.data.model.WooOrderFilterState
import com.example.data.model.WooOrderSortOption
import com.example.ui.theme.*

/**
 * Filterable UI component to display recent WooCommerce orders with their statuses,
 * dynamic search, status badges, sort controls, and status update actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooCommerceRecentOrdersComponent(
    orders: List<OrderEntity>,
    modifier: Modifier = Modifier,
    initialFilterState: WooOrderFilterState = WooOrderFilterState(),
    onOrderStatusChange: (orderId: String, newStatus: String) -> Unit = { _, _ -> },
    onOrderClick: ((WooCommerceOrder) -> Unit)? = null,
    onSimulateNewOrderClick: (() -> Unit)? = null,
    onShowMessage: (String) -> Unit = {}
) {
    // Convert entities to WooCommerceOrder model
    val wooOrders = remember(orders) {
        orders.map { WooCommerceOrder.fromEntity(it) }
    }

    var filterState by remember { mutableStateOf(initialFilterState) }
    var selectedOrderForDetail by remember { mutableStateOf<WooCommerceOrder?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Apply filtering and sorting
    val filteredOrders = remember(wooOrders, filterState) {
        var result = wooOrders

        // Filter by Status
        if (filterState.statusFilter != null) {
            result = result.filter { it.status == filterState.statusFilter }
        }

        // Filter by Search Query
        if (filterState.searchQuery.isNotBlank()) {
            val query = filterState.searchQuery.trim().lowercase()
            result = result.filter { order ->
                order.orderNumber.lowercase().contains(query) ||
                order.customerName.lowercase().contains(query) ||
                order.customerEmail.lowercase().contains(query) ||
                order.itemsSummary.lowercase().contains(query) ||
                order.shippingCity.lowercase().contains(query)
            }
        }

        // Apply Sorting
        when (filterState.sortOption) {
            WooOrderSortOption.DATE_DESC -> result.sortedByDescending { it.id }
            WooOrderSortOption.DATE_ASC -> result.sortedBy { it.id }
            WooOrderSortOption.AMOUNT_DESC -> result.sortedByDescending { it.totalAmount }
            WooOrderSortOption.AMOUNT_ASC -> result.sortedBy { it.totalAmount }
            WooOrderSortOption.CUSTOMER_ASC -> result.sortedBy { it.customerName.lowercase() }
        }
    }

    // Counts for status chips
    val totalCount = wooOrders.size
    val processingCount = remember(wooOrders) {
        wooOrders.count { it.status == WooCommerceOrderStatus.PROCESSING }
    }
    val completedCount = remember(wooOrders) {
        wooOrders.count { it.status == WooCommerceOrderStatus.COMPLETED }
    }
    val onHoldCount = remember(wooOrders) {
        wooOrders.count { it.status == WooCommerceOrderStatus.ON_HOLD }
    }
    val cancelledCount = remember(wooOrders) {
        wooOrders.count { it.status == WooCommerceOrderStatus.CANCELLED }
    }

    val totalRevenueFiltered = remember(filteredOrders) {
        filteredOrders.sumOf { it.totalAmount }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("woocommerce_recent_orders_component")
    ) {
        // Summary Metrics Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("woo_orders_summary_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WooCommerce Order Pipeline",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$%.2f".format(totalRevenueFiltered),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Filtered Volume (${filteredOrders.size} of $totalCount orders)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                if (onSimulateNewOrderClick != null) {
                    FilledTonalButton(
                        onClick = onSimulateNewOrderClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BadgeBackground,
                            contentColor = PrimaryIndigo
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_add_simulate_order")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Order", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Search Bar and Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { filterState = filterState.copy(searchQuery = it) },
                placeholder = { Text("Search by #order, customer, items...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { filterState = filterState.copy(searchQuery = "") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSlate200
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_woo_orders_input")
            )

            // Sort Button with Dropdown
            Box {
                OutlinedIconButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                    modifier = Modifier.testTag("btn_sort_woo_orders")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort Orders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    WooOrderSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (filterState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filterState.sortOption == option) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                if (filterState.sortOption == option) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                                }
                            },
                            onClick = {
                                filterState = filterState.copy(sortOption = option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All" chip
            FilterChip(
                selected = filterState.statusFilter == null,
                onClick = { filterState = filterState.copy(statusFilter = null) },
                label = { Text("All ($totalCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryIndigo,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("chip_status_all")
            )

            // "Processing" chip
            FilterChip(
                selected = filterState.statusFilter == WooCommerceOrderStatus.PROCESSING,
                onClick = {
                    filterState = filterState.copy(
                        statusFilter = if (filterState.statusFilter == WooCommerceOrderStatus.PROCESSING) null else WooCommerceOrderStatus.PROCESSING
                    )
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Processing ($processingCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (processingCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3730A3))
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WooCommerceOrderStatus.PROCESSING.badgeBgColor,
                    selectedLabelColor = WooCommerceOrderStatus.PROCESSING.badgeTextColor
                ),
                modifier = Modifier.testTag("chip_status_processing")
            )

            // "Completed" chip
            FilterChip(
                selected = filterState.statusFilter == WooCommerceOrderStatus.COMPLETED,
                onClick = {
                    filterState = filterState.copy(
                        statusFilter = if (filterState.statusFilter == WooCommerceOrderStatus.COMPLETED) null else WooCommerceOrderStatus.COMPLETED
                    )
                },
                label = { Text("Completed ($completedCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldSuccessBg,
                    selectedLabelColor = Color(0xFF065F46)
                ),
                modifier = Modifier.testTag("chip_status_completed")
            )

            // "On Hold" chip
            FilterChip(
                selected = filterState.statusFilter == WooCommerceOrderStatus.ON_HOLD,
                onClick = {
                    filterState = filterState.copy(
                        statusFilter = if (filterState.statusFilter == WooCommerceOrderStatus.ON_HOLD) null else WooCommerceOrderStatus.ON_HOLD
                    )
                },
                label = { Text("On Hold ($onHoldCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberWarningBg,
                    selectedLabelColor = Color(0xFF92400E)
                ),
                modifier = Modifier.testTag("chip_status_on_hold")
            )

            // "Cancelled" chip
            FilterChip(
                selected = filterState.statusFilter == WooCommerceOrderStatus.CANCELLED,
                onClick = {
                    filterState = filterState.copy(
                        statusFilter = if (filterState.statusFilter == WooCommerceOrderStatus.CANCELLED) null else WooCommerceOrderStatus.CANCELLED
                    )
                },
                label = { Text("Cancelled ($cancelledCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoseErrorBg,
                    selectedLabelColor = Color(0xFF991B1B)
                ),
                modifier = Modifier.testTag("chip_status_cancelled")
            )
        }

        // Active Filter Indicators / Clear option
        if (filterState.statusFilter != null || filterState.searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredOrders.size} matching orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                TextButton(
                    onClick = { filterState = WooOrderFilterState() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Clear All Filters", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Orders List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No WooCommerce orders found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try adjusting your search criteria or status filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { filterState = WooOrderFilterState() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    WooCommerceOrderItemCard(
                        order = order,
                        onClick = {
                            selectedOrderForDetail = order
                            onOrderClick?.invoke(order)
                        },
                        onStatusChange = { newStatusKey ->
                            onOrderStatusChange(order.id, newStatusKey)
                        },
                        onShowMessage = onShowMessage
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Order Detail Modal BottomSheet
    if (selectedOrderForDetail != null) {
        val detailOrder = selectedOrderForDetail!!
        WooCommerceOrderDetailSheet(
            order = detailOrder,
            onDismiss = { selectedOrderForDetail = null },
            onStatusChange = { newStatusKey ->
                onOrderStatusChange(detailOrder.id, newStatusKey)
                selectedOrderForDetail = detailOrder.copy(
                    status = WooCommerceOrderStatus.fromKey(newStatusKey)
                )
            },
            onShowMessage = onShowMessage
        )
    }
}

/**
 * Individual Order Item Card with status badge, customer info, totals, and quick status action.
 */
@Composable
fun WooCommerceOrderItemCard(
    order: WooCommerceOrder,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("woo_order_card_${order.orderNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Order Number, Date, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive Status Badge with Menu
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = order.status.badgeBgColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showStatusMenu = true }
                            .testTag("status_badge_${order.orderNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.status.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = order.status.badgeTextColor,
                                fontSize = 11.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Status",
                                tint = order.status.badgeTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Status Switcher Dropdown Menu
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        WooCommerceOrderStatus.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusOption.badgeTextColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(statusOption.displayName)
                                    }
                                },
                                onClick = {
                                    onStatusChange(statusOption.key)
                                    showStatusMenu = false
                                    onShowMessage("${order.orderNumber} updated to ${statusOption.displayName}")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customer Initials Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = order.customerName
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifBlank { "C" }
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.customerEmail} • ${order.shippingCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Price Total
                Text(
                    text = order.formattedTotal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Line items pill and payment badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.itemsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = order.paymentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Bottom Sheet displaying complete WooCommerce order details with line items and fulfillment actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooCommerceOrderDetailSheet(
    order: WooCommerceOrder,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("woo_order_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order ${order.orderNumber}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Placed on ${order.dateFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = order.status.badgeBgColor
                ) {
                    Text(
                        text = order.status.displayName,
                        fontWeight = FontWeight.Bold,
                        color = order.status.badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Details Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Customer Information",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Name: ${order.customerName}", fontWeight = FontWeight.SemiBold)
                    Text(text = "Email: ${order.customerEmail}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(text = "Shipping: ${order.shippingCity}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(text = "Payment: ${order.paymentMethod}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items Purchased Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Line Items Breakdown",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = order.itemsSummary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = BorderSlate200)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order Grand Total:", fontWeight = FontWeight.Bold)
                        Text(order.formattedTotal, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Text(
                text = "Fulfillment Status Actions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onStatusChange("completed")
                        onShowMessage("Marked ${order.orderNumber} as Completed")
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Complete", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        onStatusChange("processing")
                        onShowMessage("Marked ${order.orderNumber} as Processing")
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Processing", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onStatusChange("on-hold")
                        onShowMessage("Marked ${order.orderNumber} as On-Hold")
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("On-Hold", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onShowMessage("PDF Invoice generated for ${order.orderNumber}")
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print / Download WooCommerce Invoice")
            }
        }
    }
}
