package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.PostEntity
import com.example.ui.theme.EmeraldSuccess

data class GutenbergBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var blockName: String, // "core/heading", "core/paragraph", "core/image", "core/list", "core/quote", "core/code", "core/button"
    var content: String,
    var level: Int = 2, // 1..6 for headings
    var alignment: String = "left", // "left", "center", "right"
    var imageUrl: String = "",
    var imageCaption: String = "",
    var buttonUrl: String = "",
    var quoteAuthor: String = ""
)

enum class EditorViewMode {
    VISUAL_BLOCKS,
    RAW_HTML_INSPECTOR,
    PUBLISHING_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GutenbergVisualEditorSheet(
    initialPost: PostEntity?,
    defaultType: String = "post",
    onDismiss: () -> Unit,
    onSavePost: (
        title: String,
        excerpt: String,
        blocksJson: String,
        status: String,
        postType: String,
        category: String,
        featuredImageUrl: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var excerpt by remember { mutableStateOf(initialPost?.excerpt ?: "") }
    var postType by remember { mutableStateOf(initialPost?.postType ?: defaultType) }
    var category by remember { mutableStateOf(initialPost?.category ?: "Announcements") }
    var status by remember { mutableStateOf(initialPost?.status ?: "published") }
    var featuredImageUrl by remember { mutableStateOf(initialPost?.featuredImageUrl ?: "") }
    var allowComments by remember { mutableStateOf(true) }

    var viewMode by remember { mutableStateOf(EditorViewMode.VISUAL_BLOCKS) }

    // Parse initial content or generate Gutenberg default blocks
    val blocks = remember {
        mutableStateListOf<GutenbergBlock>().apply {
            if (initialPost != null && initialPost.content.isNotBlank()) {
                val raw = initialPost.content
                if (raw.contains("""{"type":""")) {
                    try {
                        val matches = Regex("""\{"type":"([^"]+)","text":"([^"]+)"\}""").findAll(raw)
                        matches.forEach { match ->
                            val type = match.groupValues[1]
                            val text = match.groupValues[2].replace("\\n", "\n")
                            val name = when (type) {
                                "heading" -> "core/heading"
                                "image" -> "core/image"
                                "quote" -> "core/quote"
                                "list" -> "core/list"
                                "code" -> "core/code"
                                "button" -> "core/button"
                                else -> "core/paragraph"
                            }
                            add(GutenbergBlock(blockName = name, content = text))
                        }
                    } catch (e: Exception) {
                        add(GutenbergBlock(blockName = "core/paragraph", content = initialPost.content))
                    }
                } else if (raw.contains("<!-- wp:")) {
                    // Raw Gutenberg comment parsing
                    val pMatches = Regex("""<!-- wp:paragraph -->\s*<p>(.*?)</p>\s*<!-- /wp:paragraph -->""", RegexOption.DOT_MATCHES_ALL).findAll(raw)
                    pMatches.forEach { m ->
                        add(GutenbergBlock(blockName = "core/paragraph", content = m.groupValues[1].trim()))
                    }
                    if (isEmpty()) {
                        add(GutenbergBlock(blockName = "core/paragraph", content = raw.replace(Regex("<[^>]*>"), "")))
                    }
                } else {
                    add(GutenbergBlock(blockName = "core/paragraph", content = initialPost.content))
                }
            }
            if (isEmpty()) {
                add(GutenbergBlock(blockName = "core/heading", content = "Welcome to Our Gutenberg Post", level = 2))
                add(GutenbergBlock(blockName = "core/paragraph", content = "Build rich, beautiful articles with native Gutenberg mobile blocks, images, quotes, and buttons."))
                add(GutenbergBlock(blockName = "core/quote", content = "The future of WordPress content editing is block-based.", quoteAuthor = "WordPress Core"))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (initialPost == null) "New $postType (Gutenberg)" else "Edit $postType",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gutenberg Block Editor v6.6 • WP REST v2",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewMode = when (viewMode) {
                                    EditorViewMode.VISUAL_BLOCKS -> EditorViewMode.RAW_HTML_INSPECTOR
                                    EditorViewMode.RAW_HTML_INSPECTOR -> EditorViewMode.VISUAL_BLOCKS
                                    EditorViewMode.PUBLISHING_SETTINGS -> EditorViewMode.VISUAL_BLOCKS
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == EditorViewMode.RAW_HTML_INSPECTOR) Icons.Default.ViewAgenda else Icons.Default.Code,
                                contentDescription = "Toggle Inspector",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == EditorViewMode.PUBLISHING_SETTINGS) EditorViewMode.VISUAL_BLOCKS else EditorViewMode.PUBLISHING_SETTINGS
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Publishing Settings",
                                tint = if (viewMode == EditorViewMode.PUBLISHING_SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val jsonBuilder = StringBuilder("[")
                                blocks.forEachIndexed { index, block ->
                                    val escaped = block.content.replace("\"", "\\\"").replace("\n", "\\n")
                                    val typeShort = when (block.blockName) {
                                        "core/heading" -> "heading"
                                        "core/image" -> "image"
                                        "core/quote" -> "quote"
                                        "core/list" -> "list"
                                        "core/code" -> "code"
                                        "core/button" -> "button"
                                        else -> "paragraph"
                                    }
                                    jsonBuilder.append("""{"type":"$typeShort","text":"$escaped"}""")
                                    if (index < blocks.size - 1) jsonBuilder.append(",")
                                }
                                jsonBuilder.append("]")

                                onSavePost(
                                    title.ifBlank { "Untitled Post" },
                                    excerpt,
                                    jsonBuilder.toString(),
                                    status,
                                    postType,
                                    category,
                                    featuredImageUrl.ifBlank { null }
                                )
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("gutenberg_save_publish_button")
                        ) {
                            Icon(
                                imageVector = if (status == "published") Icons.Default.CloudUpload else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (status == "published") "Publish" else "Save")
                        }
                    }
                )
            },
            bottomBar = {
                if (viewMode == EditorViewMode.VISUAL_BLOCKS) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Insert Gutenberg Block:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = { blocks.add(GutenbergBlock(blockName = "core/heading", content = "New Heading", level = 2)) },
                                    label = { Text("Heading") },
                                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("add_gutenberg_heading")
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { blocks.add(GutenbergBlock(blockName = "core/paragraph", content = "Write paragraph text...")) },
                                    label = { Text("Paragraph") },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("add_gutenberg_paragraph")
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { blocks.add(GutenbergBlock(blockName = "core/image", content = "Image Caption", imageUrl = "https://picsum.photos/800/400")) },
                                    label = { Text("Image") },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { blocks.add(GutenbergBlock(blockName = "core/quote", content = "Inspiring quote text", quoteAuthor = "Author Name")) },
                                    label = { Text("Quote") },
                                    leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
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
                when (viewMode) {
                    EditorViewMode.VISUAL_BLOCKS -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                // Document Title
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Post Title") },
                                    placeholder = { Text("Enter post title...") },
                                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gutenberg_post_title_input")
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gutenberg Blocks (${blocks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                blocks.add(GutenbergBlock(blockName = "core/code", content = "<?php\n// WordPress custom hook\nadd_action('init', 'my_custom_init');"))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Code, contentDescription = "Add Code Block", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                blocks.add(GutenbergBlock(blockName = "core/button", content = "Explore Store", buttonUrl = "https://example.com"))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.TouchApp, contentDescription = "Add Button Block", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }

                            itemsIndexed(blocks, key = { _, b -> b.id }) { index, block ->
                                GutenbergBlockCard(
                                    block = block,
                                    index = index,
                                    totalBlocks = blocks.size,
                                    onUpdate = { updated -> blocks[index] = updated },
                                    onMoveUp = {
                                        if (index > 0) {
                                            val item = blocks.removeAt(index)
                                            blocks.add(index - 1, item)
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < blocks.size - 1) {
                                            val item = blocks.removeAt(index)
                                            blocks.add(index + 1, item)
                                        }
                                    },
                                    onDuplicate = {
                                        blocks.add(index + 1, block.copy(id = java.util.UUID.randomUUID().toString()))
                                    },
                                    onDelete = {
                                        if (blocks.size > 1) {
                                            blocks.removeAt(index)
                                        }
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(40.dp))
                            }
                        }
                    }

                    EditorViewMode.RAW_HTML_INSPECTOR -> {
                        val rawGutenbergHtml = remember(blocks) {
                            buildString {
                                blocks.forEach { b ->
                                    when (b.blockName) {
                                        "core/heading" -> appendLine("<!-- wp:heading {\"level\":${b.level}} -->\n<h${b.level}>${b.content}</h${b.level}>\n<!-- /wp:heading -->\n")
                                        "core/paragraph" -> appendLine("<!-- wp:paragraph -->\n<p>${b.content}</p>\n<!-- /wp:paragraph -->\n")
                                        "core/image" -> appendLine("<!-- wp:image {\"id\":101} -->\n<figure class=\"wp-block-image\"><img src=\"${b.imageUrl}\" alt=\"\"/><figcaption>${b.imageCaption}</figcaption></figure>\n<!-- /wp:image -->\n")
                                        "core/quote" -> appendLine("<!-- wp:quote -->\n<blockquote class=\"wp-block-quote\"><p>${b.content}</p><cite>${b.quoteAuthor}</cite></blockquote>\n<!-- /wp:quote -->\n")
                                        "core/code" -> appendLine("<!-- wp:code -->\n<pre class=\"wp-block-code\"><code>${b.content}</code></pre>\n<!-- /wp:code -->\n")
                                        "core/button" -> appendLine("<!-- wp:button -->\n<div class=\"wp-block-button\"><a class=\"wp-block-button__link\" href=\"${b.buttonUrl}\">${b.content}</a></div>\n<!-- /wp:button -->\n")
                                        else -> appendLine("<!-- wp:paragraph -->\n<p>${b.content}</p>\n<!-- /wp:paragraph -->\n")
                                    }
                                }
                            }
                        }

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
                                Text(
                                    text = "Gutenberg Comment HTML Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "WP REST v2 Compatible",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                OutlinedTextField(
                                    value = rawGutenbergHtml,
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    EditorViewMode.PUBLISHING_SETTINGS -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Publishing & Document Settings",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    label = { Text("Primary Category") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                var expandedStatus by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = status.replaceFirstChar { it.uppercase() },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Post Visibility / Status") },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedStatus = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = expandedStatus,
                                        onDismissRequest = { expandedStatus = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Published (Live on Website)") },
                                            onClick = { status = "published"; expandedStatus = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Draft (Saved locally)") },
                                            onClick = { status = "draft"; expandedStatus = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Scheduled") },
                                            onClick = { status = "scheduled"; expandedStatus = false }
                                        )
                                    }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = excerpt,
                                    onValueChange = { excerpt = it },
                                    label = { Text("SEO Excerpt Summary") },
                                    placeholder = { Text("Summary for search engines...") },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = featuredImageUrl,
                                    onValueChange = { featuredImageUrl = it },
                                    label = { Text("Featured Image URL") },
                                    placeholder = { Text("https://example.com/wp-content/uploads/hero.jpg") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (featuredImageUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    ) {
                                        AsyncImage(
                                            model = featuredImageUrl,
                                            contentDescription = "Featured Image Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Allow Visitor Comments",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Enable discussion and moderation on this post",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = allowComments,
                                            onCheckedChange = { allowComments = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GutenbergBlockCard(
    block: GutenbergBlock,
    index: Int,
    totalBlocks: Int,
    onUpdate: (GutenbergBlock) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gutenberg_block_card_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Block Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (block.blockName) {
                        "core/heading" -> Icons.Default.Title
                        "core/image" -> Icons.Default.Image
                        "core/quote" -> Icons.Default.FormatQuote
                        "core/list" -> Icons.Default.FormatListBulleted
                        "core/code" -> Icons.Default.Code
                        "core/button" -> Icons.Default.TouchApp
                        else -> Icons.Default.FormatAlignLeft
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = block.blockName.removePrefix("core/").replaceFirstChar { it.uppercase() } + " Block",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    }
                    if (index > 0) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < totalBlocks - 1) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Block", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specific Block Editors
            when (block.blockName) {
                "core/heading" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Heading Level:", style = MaterialTheme.typography.labelSmall)
                            (1..4).forEach { lvl ->
                                FilterChip(
                                    selected = block.level == lvl,
                                    onClick = { onUpdate(block.copy(level = lvl)) },
                                    label = { Text("H$lvl") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            textStyle = when (block.level) {
                                1 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                "core/image" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.imageUrl,
                            onValueChange = { onUpdate(block.copy(imageUrl = it)) },
                            label = { Text("Image URL") },
                            placeholder = { Text("https://example.com/photo.jpg") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (block.imageUrl.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                AsyncImage(
                                    model = block.imageUrl,
                                    contentDescription = "Block Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            label = { Text("Image Caption") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                "core/quote" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            label = { Text("Quote Content") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = block.quoteAuthor,
                            onValueChange = { onUpdate(block.copy(quoteAuthor = it)) },
                            label = { Text("Author / Citation") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                "core/code" -> {
                    OutlinedTextField(
                        value = block.content,
                        onValueChange = { onUpdate(block.copy(content = it)) },
                        label = { Text("Code / Syntax Snippet") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "core/button" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = block.content,
                            onValueChange = { onUpdate(block.copy(content = it)) },
                            label = { Text("Button Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = block.buttonUrl,
                            onValueChange = { onUpdate(block.copy(buttonUrl = it)) },
                            label = { Text("Target Link URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = block.content,
                        onValueChange = { onUpdate(block.copy(content = it)) },
                        label = { Text("Paragraph Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
