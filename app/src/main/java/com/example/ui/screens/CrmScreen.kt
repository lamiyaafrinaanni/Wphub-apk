package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CustomerEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.*

@Composable
fun CrmScreen(
    customers: List<CustomerEntity>,
    onUpdateRole: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    var selectedCustomerForRoleChange by remember { mutableStateOf<CustomerEntity?>(null) }
    val totalLtv = remember(customers) { customers.sumOf { it.totalSpent } }
    val avgSpend = remember(customers) { if (customers.isNotEmpty()) totalLtv / customers.size else 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("crm_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CRM Insights Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CRM & CUSTOMER INSIGHTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Customer Lifetime Value",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricSmallCard(
                                title = "Total CRM Value",
                                value = currencyFormat.format(totalLtv),
                                subtitle = "${customers.size} Registered accounts",
                                isPositive = true,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSmallCard(
                                title = "Average LTV",
                                value = currencyFormat.format(avgSpend),
                                subtitle = "High retention rate",
                                isPositive = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Directory & Profiles (${customers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to manage roles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(customers, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    currencyFormat = currencyFormat,
                    onRoleClick = { selectedCustomerForRoleChange = customer },
                    onContact = { onShowMessage("Opening communication channel to ${customer.email}...") }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (selectedCustomerForRoleChange != null) {
        RolePickerBottomSheet(
            customer = selectedCustomerForRoleChange!!,
            onDismiss = { selectedCustomerForRoleChange = null },
            onSelectRole = { newRole ->
                onUpdateRole(selectedCustomerForRoleChange!!.id, newRole)
                selectedCustomerForRoleChange = null
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    currencyFormat: NumberFormat,
    onRoleClick: () -> Unit,
    onContact: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("customer_card_${customer.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Initials
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.avatarInitials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        UserRoleBadge(role = customer.role, onClick = onRoleClick)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customer.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lifetime Spent: ${currencyFormat.format(customer.totalSpent)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${customer.ordersCount} Completed Orders • ${customer.lastActive}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onContact,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UserRoleBadge(role: String, onClick: () -> Unit) {
    val (bg, fg) = when (role.lowercase()) {
        "administrator" -> Pair(RoseErrorBg, RoseError)
        "editor" -> Pair(Color(0xFFEDE9FE), VioletPurple)
        "author" -> Pair(AmberWarningBg, AmberWarning)
        else -> Pair(EmeraldSuccessBg, EmeraldSuccess)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePickerBottomSheet(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSelectRole: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Change Role for ${customer.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "WordPress User Capability Matrix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val roles = listOf(
                "Administrator" to "Full administrative access to settings, plugins, and users.",
                "Editor" to "Can publish and manage posts, including posts of others.",
                "Author" to "Can publish and manage their own posts.",
                "Customer" to "Standard WooCommerce customer with storefront access.",
                "Subscriber" to "Read-only access with profile management."
            )

            roles.forEach { (role, desc) ->
                Surface(
                    onClick = { onSelectRole(role) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (customer.role.equals(role, ignoreCase = true)) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = role, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (customer.role.equals(role, ignoreCase = true)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
