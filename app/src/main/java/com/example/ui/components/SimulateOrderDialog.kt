package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.WooPurple

@Composable
fun SimulateOrderDialog(
    siteName: String,
    onDismiss: () -> Unit,
    onPlaceOrder: (customerName: String, customerEmail: String, itemsSummary: String, totalAmount: Double) -> Unit
) {
    var customerName by remember { mutableStateOf("Sophia Martinez") }
    var customerEmail by remember { mutableStateOf("sophia.m@example.com") }
    var itemsSummary by remember { mutableStateOf("Wireless ANC Headset (x1), USB-C Fast Charger (x1)") }
    var totalAmountStr by remember { mutableStateOf("189.50") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_simulate_order")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WooPurple.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = WooPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New WooCommerce Order",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulate order on $siteName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Placing this order will immediately dispatch an Android system push notification with the Order ID & Total Amount to your device.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Templates
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = totalAmountStr == "89.00",
                        onClick = {
                            customerName = "Liam O'Connor"
                            customerEmail = "liam.oc@example.com"
                            itemsSummary = "Mechanical Keycaps Set (x1)"
                            totalAmountStr = "89.00"
                        },
                        label = { Text("$89.00 • Keycaps") }
                    )
                    FilterChip(
                        selected = totalAmountStr == "279.00",
                        onClick = {
                            customerName = "Alexander Wright"
                            customerEmail = "alex.wright@example.com"
                            itemsSummary = "Ergonomic Desk Chair (x1)"
                            totalAmountStr = "279.00"
                        },
                        label = { Text("$279.00 • Desk Chair") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_order_customer_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Customer Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_order_customer_email")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Items Summary") },
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_order_items")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalAmountStr,
                    onValueChange = { totalAmountStr = it },
                    label = { Text("Total Amount ($)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = EmeraldSuccess) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_order_total")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amount = totalAmountStr.toDoubleOrNull() ?: 149.00
                            onPlaceOrder(customerName, customerEmail, itemsSummary, amount)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WooPurple),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_confirm_place_order")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Order")
                    }
                }
            }
        }
    }
}
