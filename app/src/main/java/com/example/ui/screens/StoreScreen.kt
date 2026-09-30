package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CouponEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.model.WooCommerceOrder
import com.example.data.model.WooCommerceOrderStatus
import com.example.data.model.WooOrderFilterState
import com.example.ui.components.SimulateOrderDialog
import com.example.ui.components.WooCommerceRecentOrdersComponent
import com.example.ui.theme.*

@Composable
fun StoreScreen(
    products: List<ProductEntity>,
    orders: List<OrderEntity>,
    coupons: List<CouponEntity>,
    orderFilter: String,
    targetOrderId: String? = null,
    targetProductId: String? = null,
    onClearTargetOrder: () -> Unit = {},
    onClearTargetProduct: () -> Unit = {},
    onFilterChange: (String) -> Unit,
    onOrderStatusChange: (String, String) -> Unit,
    onSaveProduct: (id: String?, name: String, sku: String, regPrice: Double, salePrice: Double?, stockQty: Int, category: String, type: String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCoupon: (code: String, type: String, amount: Double, limit: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    onPlaceCustomOrder: ((customerName: String, customerEmail: String, itemsSummary: String, totalAmount: Double) -> Unit)? = null,
    onTriggerSimulatedOrderAlert: (() -> Unit)? = null
) {
    var selectedStoreSubTab by remember { mutableIntStateOf(if (targetOrderId != null) 1 else 0) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddCouponDialog by remember { mutableStateOf(false) }
    var showSimulateOrderDialog by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<OrderEntity?>(null) }

    LaunchedEffect(targetOrderId) {
        if (targetOrderId != null) {
            selectedStoreSubTab = 1
            val match = orders.find { it.id == targetOrderId }
            if (match != null) {
                selectedOrderForDetail = match
            }
        }
    }

    LaunchedEffect(targetProductId) {
        if (targetProductId != null) {
            selectedStoreSubTab = 0
            val match = products.find { it.id == targetProductId }
            if (match != null) {
                editingProduct = match
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("store_screen")
    ) {
        // Sub-tabs
        TabRow(
            selectedTabIndex = selectedStoreSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedStoreSubTab == 0,
                onClick = { selectedStoreSubTab = 0 },
                text = { Text("Products (${products.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_products")
            )
            Tab(
                selected = selectedStoreSubTab == 1,
                onClick = { selectedStoreSubTab = 1 },
                text = { Text("Orders (${orders.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_orders")
            )
            Tab(
                selected = selectedStoreSubTab == 2,
                onClick = { selectedStoreSubTab = 2 },
                text = { Text("Coupons", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_coupons")
            )
        }

        when (selectedStoreSubTab) {
            0 -> {
                // Products Catalog
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductCatalogCard(
                                product = product,
                                onEdit = { editingProduct = product },
                                onDelete = { onDeleteProduct(product.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    FloatingActionButton(
                        onClick = { showAddProductDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("fab_add_product")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            }
            1 -> {
                // Orders Stream with Live Push Notification Dispatcher
                Box(modifier = Modifier.fillMaxSize()) {
                    WooCommerceRecentOrdersComponent(
                        orders = orders,
                        initialFilterState = WooOrderFilterState(
                            statusFilter = if (orderFilter == "all") null else WooCommerceOrderStatus.fromKey(orderFilter)
                        ),
                        onOrderStatusChange = onOrderStatusChange,
                        onSimulateNewOrderClick = { showSimulateOrderDialog = true },
                        onShowMessage = onShowMessage
                    )

                    // Floating action button to place/simulate a new WooCommerce order with push notification
                    FloatingActionButton(
                        onClick = { showSimulateOrderDialog = true },
                        containerColor = WooPurple,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("fab_simulate_order")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Place New Order")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Order (+ Push)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
            2 -> {
                // Coupons & Marketing
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "WooCommerce Marketing & Coupons",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Manage discount campaigns, percentage vouchers, and cart promo rules.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(coupons, key = { it.id }) { coupon ->
                            CouponCard(coupon = coupon)
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }

                    FloatingActionButton(
                        onClick = { showAddCouponDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("fab_add_coupon")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Coupon")
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog || editingProduct != null) {
        ProductEditDialog(
            initialProduct = editingProduct,
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
                onClearTargetProduct()
            },
            onSave = { name, sku, regPrice, salePrice, stockQty, cat, type ->
                onSaveProduct(editingProduct?.id, name, sku, regPrice, salePrice, stockQty, cat, type)
                showAddProductDialog = false
                editingProduct = null
                onClearTargetProduct()
            }
        )
    }

    // Order Detail Dialog
    if (selectedOrderForDetail != null) {
        OrderDetailDialog(
            order = selectedOrderForDetail!!,
            onDismiss = {
                selectedOrderForDetail = null
                onClearTargetOrder()
            },
            onStatusChange = { newStatus ->
                onOrderStatusChange(selectedOrderForDetail!!.id, newStatus)
                selectedOrderForDetail = selectedOrderForDetail!!.copy(status = newStatus)
            },
            onPrintInvoice = {
                onShowMessage("Receipt generated for ${selectedOrderForDetail!!.orderNumber}")
                selectedOrderForDetail = null
                onClearTargetOrder()
            }
        )
    }

    // Add Coupon Dialog
    if (showAddCouponDialog) {
        AddCouponDialog(
            onDismiss = { showAddCouponDialog = false },
            onSave = { code, type, amt, limit ->
                onSaveCoupon(code, type, amt, limit)
                showAddCouponDialog = false
            }
        )
    }

    // Simulate WooCommerce Order Dialog
    if (showSimulateOrderDialog) {
        SimulateOrderDialog(
            siteName = "WooCommerce Store",
            onDismiss = { showSimulateOrderDialog = false },
            onPlaceOrder = { name, email, items, total ->
                if (onPlaceCustomOrder != null) {
                    onPlaceCustomOrder(name, email, items, total)
                } else {
                    onTriggerSimulatedOrderAlert?.invoke()
                }
            }
        )
    }
}

@Composable
fun ProductCatalogCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SKU: ${product.sku} • ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ProductStockBadge(status = product.stockStatus, qty = product.stockQuantity)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.salePrice != null) {
                        Text(
                            text = "$%.2f".format(product.salePrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$%.2f".format(product.regularPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    } else {
                        Text(
                            text = "$%.2f".format(product.regularPrice),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Product", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductStockBadge(status: String, qty: Int) {
    val (bg, fg, text) = when (status) {
        "instock" -> Triple(EmeraldSuccessBg, EmeraldSuccess, "$qty In Stock")
        "lowstock" -> Triple(AmberWarningBg, AmberWarning, "Low Stock ($qty)")
        else -> Triple(RoseErrorBg, RoseError, "Out of Stock")
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun OrderFulfillmentCard(
    order: OrderEntity,
    onCardClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onPrintInvoice: () -> Unit
) {
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OrderStatusBadge(status = order.status)
                }
                Text(
                    text = "${order.currency}%.2f".format(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${order.customerName} • ${order.customerEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.status == "processing") {
                    Button(
                        onClick = { onStatusChange("completed") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fulfill Order", fontSize = 12.sp)
                    }
                } else if (order.status == "completed") {
                    OutlinedButton(
                        onClick = { onStatusChange("processing") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Text("Reopen Order", fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onPrintInvoice,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Receipt", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CouponCard(coupon: CouponEntity) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("coupon_card_${coupon.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = coupon.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = coupon.discountType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${coupon.usageCount} / ${coupon.usageLimit} uses",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Exp: ${coupon.expiryDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProductEditDialog(
    initialProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, regPrice: Double, salePrice: Double?, stockQty: Int, category: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var regPriceStr by remember { mutableStateOf(initialProduct?.regularPrice?.toString() ?: "49.99") }
    var salePriceStr by remember { mutableStateOf(initialProduct?.salePrice?.toString() ?: "") }
    var stockQtyStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "20") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Electronics") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (initialProduct == null) "Add WooCommerce Product" else "Edit Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = regPriceStr,
                        onValueChange = { regPriceStr = it },
                        label = { Text("Regular ($)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { salePriceStr = it },
                        label = { Text("Sale ($)") },
                        placeholder = { Text("Optional") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = stockQtyStr,
                    onValueChange = { stockQtyStr = it },
                    label = { Text("Stock Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val regPrice = regPriceStr.toDoubleOrNull() ?: 0.0
                            val salePrice = salePriceStr.toDoubleOrNull()
                            val stockQty = stockQtyStr.toIntOrNull() ?: 0
                            onSave(name, sku, regPrice, salePrice, stockQty, category, "Simple Product")
                        },
                        modifier = Modifier.weight(1f).testTag("save_product_confirm_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailDialog(
    order: OrderEntity,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onPrintInvoice: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Order ${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OrderStatusBadge(status = order.status)
                }
                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Customer Information", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = order.customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = order.customerEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Shipping: ${order.shippingCity}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Line Items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Payment: ${order.paymentMethod}", style = MaterialTheme.typography.labelSmall)
                            Text(text = "Total: ${order.currency}%.2f".format(order.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Update Status:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onStatusChange("completed") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { onStatusChange("on-hold") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("On Hold", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { onStatusChange("cancelled") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPrintInvoice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print Customer Receipt")
                }
            }
        }
    }
}

@Composable
fun AddCouponDialog(
    onDismiss: () -> Unit,
    onSave: (code: String, type: String, amount: Double, limit: Int) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Percentage (20% Off)") }
    var amountStr by remember { mutableStateOf("20") }
    var limitStr by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Create WooCommerce Coupon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Promo Code (e.g. FLASH30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("coupon_code_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Discount Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Discount Value") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Usage Limit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (code.isNotBlank()) {
                                onSave(code, type, amountStr.toDoubleOrNull() ?: 10.0, limitStr.toIntOrNull() ?: 50)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_coupon_button")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "completed" -> EmeraldSuccessBg to EmeraldSuccess
        "processing" -> BadgeBackground to PrimaryIndigo
        "on-hold" -> AmberWarningBg to AmberWarning
        "cancelled" -> RoseErrorBg to RoseError
        else -> BgGradientEnd to TextDark
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp
        )
    }
}

