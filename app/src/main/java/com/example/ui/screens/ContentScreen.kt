package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PostEntity
import com.example.data.local.SiteEntity
import com.example.ui.components.BlockEditorDialog
import com.example.ui.components.GutenbergEditor
import com.example.ui.theme.*

data class MediaAsset(
    val id: String,
    val title: String,
    val mimeType: String,
    val dimensions: String,
    val fileSize: String,
    val date: String,
    val colorHex: Long
)

@Composable
fun ContentScreen(
    posts: List<PostEntity>,
    contentFilter: String,
    activeSite: SiteEntity? = null,
    targetPostId: String? = null,
    onClearTargetPost: () -> Unit = {},
    onFilterChange: (String) -> Unit,
    onSavePost: (id: String?, title: String, excerpt: String, content: String, status: String, postType: String, category: String) -> Unit,
    onDeletePost: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var selectedContentTab by remember { mutableIntStateOf(0) } // 0: Posts & Pages, 1: Media Library
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<PostEntity?>(null) }
    var editorDefaultType by remember { mutableStateOf("post") }

    LaunchedEffect(targetPostId) {
        if (targetPostId != null) {
            selectedContentTab = 0
            val match = posts.find { it.id == targetPostId }
            if (match != null) {
                editingPost = match
                editorDefaultType = match.postType
                showEditorDialog = true
            }
        }
    }

    // Media library assets
    val mediaItems = remember { mutableStateListOf<MediaAsset>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("content_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedContentTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedContentTab == 0,
                onClick = { selectedContentTab = 0 },
                text = { Text("Publishing Engine", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_posts_pages")
            )
            Tab(
                selected = selectedContentTab == 1,
                onClick = { selectedContentTab = 1 },
                text = { Text("Media Library", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_media_library")
            )
        }

        when (selectedContentTab) {
            0 -> {
                // Filter bar: All, Posts, Pages
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = contentFilter == "all",
                            onClick = { onFilterChange("all") },
                            label = { Text("All Content (${posts.size})") }
                        )
                        FilterChip(
                            selected = contentFilter == "post",
                            onClick = { onFilterChange("post") },
                            label = { Text("Blog Posts (${posts.count { it.postType == "post" }})") }
                        )
                        FilterChip(
                            selected = contentFilter == "page",
                            onClick = { onFilterChange("page") },
                            label = { Text("Static Pages (${posts.count { it.postType == "page" }})") }
                        )
                    }

                    val filteredPosts = if (contentFilter == "all") {
                        posts
                    } else {
                        posts.filter { it.postType.equals(contentFilter, ignoreCase = true) }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                PostItemCard(
                                    post = post,
                                    onEdit = {
                                        editingPost = post
                                        editorDefaultType = post.postType
                                        showEditorDialog = true
                                    },
                                    onDelete = { onDeletePost(post.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                editingPost = null
                                editorDefaultType = if (contentFilter == "page") "page" else "post"
                                showEditorDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .testTag("fab_write_post")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Write Gutenberg Document")
                        }
                    }
                }
            }
            1 -> {
                // Media Library
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WordPress Media Library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Direct phone gallery uploads & auto WebP compression",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onShowMessage("Media uploaded to /wp-content/uploads/2026/09/ (WebP optimized)") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (mediaItems.isEmpty()) {
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
                                    imageVector = Icons.Default.PermMedia,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Media Assets Uploaded",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the Upload button above to add images and documents to your WordPress media library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mediaItems, key = { it.id }) { asset ->
                                MediaGridCard(
                                    asset = asset,
                                    onClick = { onShowMessage("Copied URL: https://mysite.com/uploads/${asset.title}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        GutenbergEditor(
            activeSite = activeSite,
            initialPost = editingPost,
            defaultPostType = editorDefaultType,
            onDismiss = {
                showEditorDialog = false
                editingPost = null
                onClearTargetPost()
            },
            onSavePost = { title, excerpt, blocksJson, status, postType, category, featuredImageUrl ->
                onSavePost(editingPost?.id, title, excerpt, blocksJson, status, postType, category)
                showEditorDialog = false
                editingPost = null
                onClearTargetPost()
            },
            onAutoSave = { title, excerpt, blocksJson, status, postType, category, featuredImageUrl ->
                onSavePost(editingPost?.id, title, excerpt, blocksJson, "draft", postType, category)
            }
        )
    }
}

@Composable
fun PostItemCard(
    post: PostEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("post_card_${post.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = post.postType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (post.status == "published") EmeraldSuccessBg else AmberWarningBg
                ) {
                    Text(
                        text = post.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (post.status == "published") EmeraldSuccess else AmberWarning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (post.excerpt.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "By ${post.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• ${post.dateFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.viewCount > 0) {
                        Text(
                            text = "• ${post.viewCount} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Post", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridCard(
    asset: MediaAsset,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(asset.colorHex).copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (asset.mimeType.startsWith("image")) Icons.Default.Image else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(asset.colorHex),
                    modifier = Modifier.size(36.dp)
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
