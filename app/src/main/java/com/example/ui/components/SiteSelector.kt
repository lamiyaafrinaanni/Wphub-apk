package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SiteEntity
import com.example.ui.WPHubViewModel
import com.example.ui.theme.*

/**
 * SiteSelector composable that fetches and displays a list of connected WordPress sites,
 * leveraging Room Database for local caching of all site metadata.
 */
@Composable
fun SiteSelector(
    viewModel: WPHubViewModel,
    modifier: Modifier = Modifier,
    onAddNewSiteClick: () -> Unit = {},
    onOpenLoginClick: () -> Unit = {},
    onSiteSelected: ((SiteEntity) -> Unit)? = null
) {
    val sites by viewModel.allSites.collectAsStateWithLifecycle()
    val currentSite by viewModel.currentSite.collectAsStateWithLifecycle()

    SiteSelector(
        sites = sites,
        currentSite = currentSite,
        modifier = modifier,
        onSelectSite = { siteId ->
            viewModel.switchSite(siteId)
            val selected = sites.find { it.id == siteId }
            if (selected != null) {
                onSiteSelected?.invoke(selected)
            }
        },
        onRefreshSiteMetadata = { siteId ->
            viewModel.syncCurrentSite()
        },
        onAddNewSiteClick = onAddNewSiteClick,
        onOpenLoginClick = onOpenLoginClick
    )
}

/**
 * Stateless SiteSelector composable displaying cached WordPress sites metadata from Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSelector(
    sites: List<SiteEntity>,
    currentSite: SiteEntity?,
    modifier: Modifier = Modifier,
    onSelectSite: (String) -> Unit,
    onRefreshSiteMetadata: ((String) -> Unit)? = null,
    onAddNewSiteClick: () -> Unit = {},
    onOpenLoginClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterArchetype by remember { mutableStateOf<String?>(null) } // null = All, "ecommerce", "blog", "corporate"

    val filteredSites = remember(sites, searchQuery, filterArchetype) {
        var list = sites
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.url.lowercase().contains(q) ||
                it.activeTheme.lowercase().contains(q)
            }
        }
        if (filterArchetype != null) {
            list = list.filter {
                if (filterArchetype == "ecommerce") it.hasWooCommerce
                else it.siteType.equals(filterArchetype, ignoreCase = true)
            }
        }
        list
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200),
        modifier = modifier
            .fillMaxWidth()
            .testTag("site_selector_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title and Quick Connect Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BadgeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Connected WordPress Sites",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cached in local Room database (${sites.size} sites)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddNewSiteClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_site_selector")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Filter Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter sites by name, domain, theme...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSlate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_site_selector_search")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Archetype Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterArchetype == null,
                    onClick = { filterArchetype = null },
                    label = { Text("All (${sites.size})", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterArchetype == "ecommerce",
                    onClick = { filterArchetype = if (filterArchetype == "ecommerce") null else "ecommerce" },
                    label = { Text("WooCommerce", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterArchetype == "blog",
                    onClick = { filterArchetype = if (filterArchetype == "blog") null else "blog" },
                    label = { Text("Blogs / News", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = filterArchetype == "corporate",
                    onClick = { filterArchetype = if (filterArchetype == "corporate") null else "corporate" },
                    label = { Text("Corporate", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sites List
            if (filteredSites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sites match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredSites.forEach { site ->
                        val isSelected = site.id == currentSite?.id
                        SiteMetadataItemCard(
                            site = site,
                            isSelected = isSelected,
                            onSelect = { onSelectSite(site.id) },
                            onRefresh = { onRefreshSiteMetadata?.invoke(site.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenLoginClick,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Authenticate via WP Login", fontSize = 12.sp, color = PrimaryIndigo)
                }

                if (currentSite != null && onRefreshSiteMetadata != null) {
                    IconButton(
                        onClick = { onRefreshSiteMetadata(currentSite.id) },
                        modifier = Modifier.testTag("btn_sync_room_metadata")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync Room Metadata",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Site Metadata Card showing details cached in Room Database.
 */
@Composable
fun SiteMetadataItemCard(
    site: SiteEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderSlate200.copy(alpha = 0.6f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("site_card_${site.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Emoji, Name, Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = site.iconEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = site.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSuccess
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = site.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Radio-like selection indicator
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Room Metadata Pills: Archetype, WordPress Version, Theme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Archetype Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (site.hasWooCommerce) WooPurple.copy(alpha = 0.15f) else BadgeBackground
                ) {
                    Text(
                        text = if (site.hasWooCommerce) "🛍️ WooCommerce" else "📰 Content / News",
                        color = if (site.hasWooCommerce) WooPurple else PrimaryIndigo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // WP & PHP Version Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "WP ${site.wpVersion}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Theme Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = site.activeTheme,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Cached Counts (Sales / Orders / Posts) from Room
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (site.hasWooCommerce) {
                        Text(
                            text = "Orders: ${site.totalOrders}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Sales: $%.0f".format(site.totalSales),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "Posts: ${site.totalPosts}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Pages: ${site.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "Synced: ${site.lastSyncTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
