package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PostEntity
import com.example.data.local.SiteEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

sealed class NativeBlock {
    abstract val id: String
    data class Paragraph(override val id: String = UUID.randomUUID().toString(), var text: String) : NativeBlock()
    data class Heading(override val id: String = UUID.randomUUID().toString(), var text: String, var level: Int = 2) : NativeBlock()
    data class Image(override val id: String = UUID.randomUUID().toString(), var url: String, var caption: String = "", var cropSettings: MediaCropSettings = MediaCropSettings()) : NativeBlock()
    data class Code(override val id: String = UUID.randomUUID().toString(), var text: String, var language: String = "javascript") : NativeBlock()
    data class Quote(override val id: String = UUID.randomUUID().toString(), var text: String, var author: String = "") : NativeBlock()
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun GutenbergEditor(
    activeSite: SiteEntity?,
    initialPost: PostEntity? = null,
    defaultPostType: String = "post",
    onDismiss: () -> Unit,
    onSavePost: (
        title: String,
        excerpt: String,
        blocksJson: String,
        status: String,
        postType: String,
        category: String,
        featuredImageUrl: String?
    ) -> Unit,
    onAutoSave: ((
        title: String,
        excerpt: String,
        blocksJson: String,
        status: String,
        postType: String,
        category: String,
        featuredImageUrl: String?
    ) -> Unit)? = null
) {
    val context = LocalContext.current
    var postTitle by remember { mutableStateOf(initialPost?.title ?: "") }
    var postExcerpt by remember { mutableStateOf(initialPost?.excerpt ?: "") }
    var postStatus by remember { mutableStateOf(initialPost?.status ?: "published") }
    var postType by remember { mutableStateOf(initialPost?.postType ?: defaultPostType) }
    var category by remember { mutableStateOf(initialPost?.category ?: "Uncategorized") }
    var featuredImageUrl by remember { mutableStateOf(initialPost?.featuredImageUrl ?: "") }

    var showPublishSettingsSheet by remember { mutableStateOf(false) }
    var cropSettings by remember { mutableStateOf(MediaCropSettings()) }
    var showMediaCropDialog by remember { mutableStateOf(false) }
    var showMediaLibraryBrowser by remember { mutableStateOf(false) }
    var showSeoSettingsDrawer by remember { mutableStateOf(false) }
    var seoMetadata by remember { mutableStateOf(SEOMetadata()) }
    var autoSaveStatus by remember { mutableStateOf("") }

    // Parse initial content to NativeBlock elements
    val blocks = remember {
        mutableStateListOf<NativeBlock>().apply {
            val content = initialPost?.content ?: ""
            if (content.isBlank()) {
                add(NativeBlock.Paragraph(text = "Welcome to your new custom native post! Start writing content using rich blocks here..."))
            } else {
                // Simplified parser for existing HTML blocks
                val blocksList = parseHtmlToNativeBlocks(content)
                if (blocksList.isEmpty()) {
                    add(NativeBlock.Paragraph(text = content))
                } else {
                    addAll(blocksList)
                }
            }
        }
    }

    var selectedBlockIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Real-time conversion of blocks back into HTML
    val generatedHtmlString by remember {
        derivedStateOf {
            convertBlocksToHtml(blocks)
        }
    }

    // Debounced Local Room Saving whenever state changes
    LaunchedEffect(postTitle, generatedHtmlString) {
        if (postTitle.isNotBlank() || generatedHtmlString.isNotBlank()) {
            delay(5000) // Debounce: Wait for 5 seconds of idle state
            autoSaveStatus = "Saving draft..."
            onAutoSave?.invoke(
                postTitle.ifBlank { "Untitled Native Post" },
                postExcerpt,
                generatedHtmlString,
                "draft",
                postType,
                category,
                featuredImageUrl.ifBlank { null }
            )
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            autoSaveStatus = "Draft saved at ${formatter.format(Date())}"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialPost == null) "New $postType (Native)" else "Edit $postType",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeSite?.name ?: "WordPress Custom Block Editor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (autoSaveStatus.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• $autoSaveStatus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("gutenberg_editor_close_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close Editor")
                    }
                },
                actions = {
                    // SEO Suite
                    IconButton(
                        onClick = { showSeoSettingsDrawer = true },
                        modifier = Modifier.testTag("editor_action_seo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "SEO Suite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Publish settings
                    IconButton(
                        onClick = { showPublishSettingsSheet = true },
                        modifier = Modifier.testTag("editor_action_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Publish Settings")
                    }

                    Button(
                        onClick = {
                            onSavePost(
                                postTitle.ifBlank { "Untitled Post" },
                                postExcerpt,
                                generatedHtmlString,
                                postStatus,
                                postType,
                                category,
                                featuredImageUrl.ifBlank { null }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("gutenberg_editor_save_button")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (postStatus == "published") "Publish" else "Save")
                    }
                }
            )
        },
        bottomBar = {
            // Quick Block Inserter Bar / Bottom Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            blocks.add(NativeBlock.Paragraph(text = ""))
                            selectedBlockIndex = blocks.lastIndex
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paragraph", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            blocks.add(NativeBlock.Heading(text = "", level = 2))
                            selectedBlockIndex = blocks.lastIndex
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Heading", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            blocks.add(NativeBlock.Image(url = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800"))
                            selectedBlockIndex = blocks.lastIndex
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Image", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            blocks.add(NativeBlock.Code(text = ""))
                            selectedBlockIndex = blocks.lastIndex
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Code", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            blocks.add(NativeBlock.Quote(text = ""))
                            selectedBlockIndex = blocks.lastIndex
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quote", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Post Title Block
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        placeholder = { Text("Enter post title...", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                        textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("native_editor_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Render list of native blocks dynamically
                itemsIndexed(blocks) { index, block ->
                    val isSelected = selectedBlockIndex == index

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedBlockIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Block Header Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (block) {
                                            is NativeBlock.Paragraph -> Icons.Default.Notes
                                            is NativeBlock.Heading -> Icons.Default.Title
                                            is NativeBlock.Image -> Icons.Default.Image
                                            is NativeBlock.Code -> Icons.Default.Code
                                            is NativeBlock.Quote -> Icons.Default.FormatQuote
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (block) {
                                            is NativeBlock.Paragraph -> "Paragraph Block"
                                            is NativeBlock.Heading -> "Heading H${block.level} Block"
                                            is NativeBlock.Image -> "Image Block"
                                            is NativeBlock.Code -> "Code Block"
                                            is NativeBlock.Quote -> "Quote Block"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Move Up
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val temp = blocks[index]
                                                blocks[index] = blocks[index - 1]
                                                blocks[index - 1] = temp
                                                selectedBlockIndex = index - 1
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move block up", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Move Down
                                    if (index < blocks.lastIndex) {
                                        IconButton(
                                            onClick = {
                                                val temp = blocks[index]
                                                blocks[index] = blocks[index + 1]
                                                blocks[index + 1] = temp
                                                selectedBlockIndex = index + 1
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move block down", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Delete Block
                                    IconButton(
                                        onClick = {
                                            blocks.removeAt(index)
                                            selectedBlockIndex = -1
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Block", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Block Content Forms
                            when (block) {
                                is NativeBlock.Paragraph -> {
                                    OutlinedTextField(
                                        value = block.text,
                                        onValueChange = {
                                            block.text = it
                                            blocks[index] = block.copy(text = it)
                                        },
                                        placeholder = { Text("Start typing paragraph...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                                is NativeBlock.Heading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Header level chip selector
                                        listOf(1, 2, 3, 4).forEach { lvl ->
                                            FilterChip(
                                                selected = block.level == lvl,
                                                onClick = {
                                                    block.level = lvl
                                                    blocks[index] = block.copy(level = lvl)
                                                },
                                                label = { Text("H$lvl") }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = block.text,
                                        onValueChange = {
                                            block.text = it
                                            blocks[index] = block.copy(text = it)
                                        },
                                        placeholder = { Text("Heading content...") },
                                        textStyle = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = when (block.level) {
                                                1 -> 22.sp
                                                2 -> 20.sp
                                                3 -> 18.sp
                                                else -> 16.sp
                                            }
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                                is NativeBlock.Image -> {
                                    OutlinedTextField(
                                        value = block.url,
                                        onValueChange = {
                                            block.url = it
                                            blocks[index] = block.copy(url = it)
                                        },
                                        label = { Text("Image Link / File URL") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = block.caption,
                                        onValueChange = {
                                            block.caption = it
                                            blocks[index] = block.copy(caption = it)
                                        },
                                        label = { Text("Image Caption / Description") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (block.url.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val previewScale = when (block.cropSettings.scaleMode) {
                                                "Cover" -> ContentScale.Crop
                                                "Fit" -> ContentScale.Fit
                                                "Stretch" -> ContentScale.FillBounds
                                                else -> ContentScale.Crop
                                            }

                                            AsyncImage(
                                                model = block.url,
                                                contentDescription = block.caption.ifBlank { "Selected Image" },
                                                contentScale = previewScale,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(
                                                        scaleX = block.cropSettings.zoom,
                                                        scaleY = block.cropSettings.zoom,
                                                        translationX = block.cropSettings.offsetX,
                                                        translationY = block.cropSettings.offsetY
                                                    )
                                            )

                                            // Crop Optimizer settings button trigger
                                            IconButton(
                                                onClick = {
                                                    cropSettings = block.cropSettings
                                                    showMediaCropDialog = true
                                                    selectedBlockIndex = index
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = "Optimize Image Crop")
                                            }
                                        }
                                    }
                                }
                                is NativeBlock.Code -> {
                                    OutlinedTextField(
                                        value = block.text,
                                        onValueChange = {
                                            block.text = it
                                            blocks[index] = block.copy(text = it)
                                        },
                                        placeholder = { Text("Write raw code here...") },
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                                is NativeBlock.Quote -> {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(100.dp)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            OutlinedTextField(
                                                value = block.text,
                                                onValueChange = {
                                                    block.text = it
                                                    blocks[index] = block.copy(text = it)
                                                },
                                                placeholder = { Text("Quote citation...") },
                                                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent
                                                )
                                            )
                                            OutlinedTextField(
                                                value = block.author,
                                                onValueChange = {
                                                    block.author = it
                                                    blocks[index] = block.copy(author = it)
                                                },
                                                placeholder = { Text("- Author citation") },
                                                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Text Toolbar (Floating context-sensitive overlay on currently selected index)
            if (selectedBlockIndex != -1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    BlockToolbar(
                        isVisible = true,
                        onBoldClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "**${current.text}**"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            } else if (current is NativeBlock.Heading) {
                                val updatedText = "**${current.text}**"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onItalicClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "*${current.text}*"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            } else if (current is NativeBlock.Heading) {
                                val updatedText = "*${current.text}*"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onUnderlineClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "<u>${current.text}</u>"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onStrikethroughClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "<s>${current.text}</s>"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onLinkClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "${current.text} [link](https://)"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onCodeClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "`${current.text}`"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onHighlightClick = {
                            val current = blocks[selectedBlockIndex]
                            if (current is NativeBlock.Paragraph) {
                                val updatedText = "<span style=\"background-color:#ffff00\">${current.text}</span>"
                                blocks[selectedBlockIndex] = current.copy(text = updatedText)
                            }
                        },
                        onInsertMediaClick = {
                            showMediaLibraryBrowser = true
                        }
                    )
                }
            }
        }
    }

    // SEO Settings Drawer call
    if (showSeoSettingsDrawer) {
        SEOSettingsDrawer(
            initialSEO = seoMetadata,
            onDismiss = { showSeoSettingsDrawer = false },
            onSaveSEO = { updatedSeo ->
                seoMetadata = updatedSeo
                showSeoSettingsDrawer = false
            }
        )
    }

    // WordPress REST library browser trigger
    if (showMediaLibraryBrowser) {
        MediaLibraryBrowser(
            activeSite = activeSite,
            onDismiss = { showMediaLibraryBrowser = false },
            onMediaSelected = { url ->
                val current = selectedBlockIndex
                if (current != -1 && blocks[current] is NativeBlock.Image) {
                    val img = blocks[current] as NativeBlock.Image
                    blocks[current] = img.copy(url = url)
                } else {
                    blocks.add(NativeBlock.Image(url = url))
                    selectedBlockIndex = blocks.lastIndex
                }
                showMediaLibraryBrowser = false
            }
        )
    }

    // Image Settings optimizer dialog
    if (showMediaCropDialog) {
        val selectedImageUrl = (blocks.getOrNull(selectedBlockIndex) as? NativeBlock.Image)?.url ?: ""
        MediaSettingsDialog(
            imageUrl = selectedImageUrl,
            initialSettings = cropSettings,
            onDismiss = { showMediaCropDialog = false },
            onApplySettings = { updatedCrop ->
                val index = selectedBlockIndex
                if (index != -1 && blocks[index] is NativeBlock.Image) {
                    val img = blocks[index] as NativeBlock.Image
                    blocks[index] = img.copy(cropSettings = updatedCrop)
                }
                showMediaCropDialog = false
            }
        )
    }

    // Publish Settings Drawer
    if (showPublishSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPublishSettingsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Post Publishing Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = postExcerpt,
                    onValueChange = { postExcerpt = it },
                    label = { Text("SEO Excerpt / Summary") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = featuredImageUrl,
                    onValueChange = { featuredImageUrl = it },
                    label = { Text("Featured Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            selectedBlockIndex = -1
                            showMediaLibraryBrowser = true
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Open library")
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visibility / Status:", fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = postStatus == "published",
                        onClick = { postStatus = if (postStatus == "published") "draft" else "published" },
                        label = { Text(if (postStatus == "published") "Published (Live)" else "Draft") }
                    )
                }

                Button(
                    onClick = { showPublishSettingsSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Settings")
                }
            }
        }
    }
}

// Convert HTML String input back to native blocks
fun parseHtmlToNativeBlocks(html: String): List<NativeBlock> {
    val blocksList = mutableListOf<NativeBlock>()
    try {
        // Simplified raw tokenizer of HTML blocks
        val tagRegex = "<(p|h1|h2|h3|h4|pre|blockquote)[^>]*>(.*?)</\\1>".toRegex(RegexOption.IGNORE_CASE)
        val matches = tagRegex.findAll(html)
        for (match in matches) {
            val tag = match.groupValues[1].lowercase()
            val text = match.groupValues[2].replace(Regex("<[^>]*>"), "") // strip nested formatting for simpliciy
            when (tag) {
                "p" -> blocksList.add(NativeBlock.Paragraph(text = text))
                "h1", "h2", "h3", "h4" -> {
                    val level = tag.substring(1).toIntOrNull() ?: 2
                    blocksList.add(NativeBlock.Heading(text = text, level = level))
                }
                "pre" -> blocksList.add(NativeBlock.Code(text = text))
                "blockquote" -> blocksList.add(NativeBlock.Quote(text = text))
            }
        }

        // Parse images specifically
        val imageRegex = "<img[^>]+src=\"([^\"]+)\"".toRegex()
        val imgMatches = imageRegex.findAll(html)
        for (imgMatch in imgMatches) {
            val src = imgMatch.groupValues[1]
            blocksList.add(NativeBlock.Image(url = src))
        }
    } catch (e: Exception) {
        blocksList.add(NativeBlock.Paragraph(text = html))
    }
    return blocksList
}

// Convert NativeBlock list back to standard WordPress HTML
fun convertBlocksToHtml(blocks: List<NativeBlock>): String {
    val sb = StringBuilder()
    for (block in blocks) {
        when (block) {
            is NativeBlock.Paragraph -> {
                sb.append("<p>${block.text}</p>\n")
            }
            is NativeBlock.Heading -> {
                sb.append("<h${block.level}>${block.text}</h${block.level}>\n")
            }
            is NativeBlock.Image -> {
                sb.append("<figure class=\"wp-block-image\"><img src=\"${block.url}\" alt=\"${block.caption}\"/><figcaption>${block.caption}</figcaption></figure>\n")
            }
            is NativeBlock.Code -> {
                sb.append("<pre class=\"wp-block-code\"><code>${block.text}</code></pre>\n")
            }
            is NativeBlock.Quote -> {
                sb.append("<blockquote class=\"wp-block-quote\"><p>${block.text}</p><cite>${block.author}</cite></blockquote>\n")
            }
        }
    }
    return sb.toString()
}
