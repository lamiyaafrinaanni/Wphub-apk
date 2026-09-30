package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.SiteEntity

data class WordPressMediaAsset(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val mimeType: String = "image/jpeg",
    val dimensions: String = "1200x800",
    val fileSize: String = "142 KB"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryBrowser(
    activeSite: SiteEntity?,
    onDismiss: () -> Unit,
    onMediaSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Pre-seeded stunning high-resolution WordPress media catalog library assets
    val mockMediaAssets = remember {
        listOf(
            WordPressMediaAsset(
                id = "101",
                title = "gutenberg-hero-banner.jpg",
                sourceUrl = "https://images.unsplash.com/photo-1542831371-29b0f74f9713?q=80&w=1200",
                dimensions = "1200x800",
                fileSize = "284 KB"
            ),
            WordPressMediaAsset(
                id = "102",
                title = "ecommerce-product-splash.jpg",
                sourceUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?q=80&w=1200",
                dimensions = "1200x900",
                fileSize = "312 KB"
            ),
            WordPressMediaAsset(
                id = "103",
                title = "office-productivity-setup.jpg",
                sourceUrl = "https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?q=80&w=1200",
                dimensions = "1200x800",
                fileSize = "194 KB"
            ),
            WordPressMediaAsset(
                id = "104",
                title = "mobile-wordpress-dashboard.png",
                sourceUrl = "https://images.unsplash.com/photo-1551288049-bebda4e38f71?q=80&w=1200",
                dimensions = "1080x1080",
                fileSize = "142 KB"
            ),
            WordPressMediaAsset(
                id = "105",
                title = "blog-lifestyle-flatlay.jpg",
                sourceUrl = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?q=80&w=1200",
                dimensions = "1600x1200",
                fileSize = "415 KB"
            ),
            WordPressMediaAsset(
                id = "106",
                title = "developer-code-syntax.jpg",
                sourceUrl = "https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=1200",
                dimensions = "1200x750",
                fileSize = "182 KB"
            )
        )
    }

    val filteredAssets = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            mockMediaAssets
        } else {
            mockMediaAssets.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "WordPress Media Library",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeSite?.name ?: "REST API Media Catalog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close Media Library")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isLoading = true }) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Sync with WP Cloud")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search WordPress assets, images...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wp_media_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (filteredAssets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Media Assets Found", fontWeight = FontWeight.Bold)
                                Text("Try searching with different keywords.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredAssets, key = { it.id }) { asset ->
                                MediaAssetCard(
                                    asset = asset,
                                    onSelect = {
                                        onMediaSelected(asset.sourceUrl)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaAssetCard(
    asset: WordPressMediaAsset,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("media_asset_card_${asset.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = asset.sourceUrl),
                    contentDescription = asset.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = asset.fileSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = asset.dimensions,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
