package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DashboardWidgetEntity
import com.example.data.local.SiteEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCustomizerSheet(
    currentSite: SiteEntity?,
    widgets: List<DashboardWidgetEntity>,
    onDismiss: () -> Unit,
    onToggleWidget: (widgetId: String, isEnabled: Boolean) -> Unit,
    onSaveWidgets: (List<DashboardWidgetEntity>) -> Unit,
    onAutoDesign: () -> Unit,
    onResetDefaults: () -> Unit
) {
    var editableWidgets by remember(widgets) {
        mutableStateOf(widgets.sortedBy { it.orderIndex })
    }

    var showSiteScanReport by remember { mutableStateOf(false) }

    fun moveWidget(index: Int, up: Boolean) {
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in editableWidgets.indices) {
            val list = editableWidgets.toMutableList()
            val temp = list[index]
            list[index] = list[targetIndex]
            list[targetIndex] = temp
            val updated = list.mapIndexed { i, w -> w.copy(orderIndex = i) }
            editableWidgets = updated
            onSaveWidgets(updated)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("dashboard_customizer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BadgeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Customize Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "${currentSite?.name ?: "WordPress Site"} • ${if (currentSite?.hasWooCommerce == true) "WooCommerce Store" else "Content & Blog Site"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBodyMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Toolbar: Auto-Design & Site Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAutoDesign,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("btn_auto_design_dashboard")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Design Layout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showSiteScanReport = !showSiteScanReport },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_toggle_scan_report")
                ) {
                    Icon(
                        if (showSiteScanReport) Icons.Default.VisibilityOff else Icons.Default.FactCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showSiteScanReport) "Hide Report" else "Site Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_reset_dashboard_defaults")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                }
            }

            // Expandable Site Inspection Card
            AnimatedVisibility(visible = showSiteScanReport) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgGradientEnd),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "WP SITE CAPABILITIES REPORT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (currentSite?.hasWooCommerce == true) WooPurple.copy(alpha = 0.15f) else EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (currentSite?.hasWooCommerce == true) "🛒 WooCommerce Active" else "📰 Pure Content / Blog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentSite?.hasWooCommerce == true) WooPurple else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentSite?.siteInspectionReport ?: "Active Theme: ${currentSite?.activeTheme} • Posts: ${currentSite?.totalPosts} • Pages: ${currentSite?.totalPages} • Categories: ${currentSite?.totalCategories}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DASHBOARD WIDGETS (${editableWidgets.count { it.isEnabled }} OF ${editableWidgets.size} ACTIVE)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Widgets List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(editableWidgets, key = { _, w -> w.id }) { index, widget ->
                    WidgetCustomizerRow(
                        widget = widget,
                        isFirst = index == 0,
                        isLast = index == editableWidgets.size - 1,
                        onToggle = { isChecked ->
                            onToggleWidget(widget.id, isChecked)
                            val list = editableWidgets.toMutableList()
                            list[index] = widget.copy(isEnabled = isChecked)
                            editableWidgets = list
                            onSaveWidgets(list)
                        },
                        onMoveUp = { moveWidget(index, up = true) },
                        onMoveDown = { moveWidget(index, up = false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCustomizerRow(
    widget: DashboardWidgetEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val icon = when (widget.widgetKey) {
        "site_banner" -> Icons.Default.Web
        "quick_stats" -> Icons.Default.Analytics
        "fast_actions" -> Icons.Default.FlashOn
        "woo_sales" -> Icons.Default.TrendingUp
        "woo_orders" -> Icons.Default.ShoppingBag
        "quick_draft" -> Icons.Default.EditNote
        "recent_content" -> Icons.Default.Article
        "recent_comments" -> Icons.Default.ChatBubble
        "theme_overview" -> Icons.Default.Palette
        "plugins_health" -> Icons.Default.Extension
        "crm_inquiries" -> Icons.Default.ContactMail
        "telemetry_iot" -> Icons.Default.WaterDrop
        "rest_api" -> Icons.Default.Api
        else -> Icons.Default.Widgets
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (widget.isEnabled) Color.White else BgGradientEnd.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (widget.isEnabled) BorderSlate200 else BorderSlate200.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_row_${widget.widgetKey}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (widget.isEnabled) BadgeBackground else Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (widget.isEnabled) PrimaryIndigo else Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = widget.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (widget.isEnabled) FontWeight.Bold else FontWeight.Normal,
                        color = if (widget.isEnabled) TextDark else Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (widget.isWooCommerceOnly) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BadgeBackground
                        ) {
                            Text(
                                text = "Woo",
                                style = MaterialTheme.typography.labelSmall,
                                color = WooPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                if (widget.description.isNotBlank()) {
                    Text(
                        text = widget.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextBodyMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Reorder controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) PrimaryIndigo else Slate400.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (!isLast) PrimaryIndigo else Slate400.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Switch(
                    checked = widget.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("toggle_widget_${widget.widgetKey}")
                )
            }
        }
    }
}
