package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PostEntity
import com.example.ui.theme.EmeraldSuccess

data class EditorBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    var type: String, // "heading", "paragraph", "list", "quote", "button"
    var content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorDialog(
    initialPost: PostEntity?,
    defaultType: String = "post",
    onDismiss: () -> Unit,
    onSave: (title: String, excerpt: String, blocksJson: String, status: String, postType: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var excerpt by remember { mutableStateOf(initialPost?.excerpt ?: "") }
    var postType by remember { mutableStateOf(initialPost?.postType ?: defaultType) }
    var category by remember { mutableStateOf(initialPost?.category ?: "Announcements") }
    var status by remember { mutableStateOf(initialPost?.status ?: "published") }

    // Parse blocks or initialize
    val blocks = remember {
        mutableStateListOf<EditorBlock>().apply {
            if (initialPost != null && initialPost.content.isNotBlank()) {
                // simple parser
                val raw = initialPost.content
                if (raw.contains("""{"type":""")) {
                    try {
                        val matches = Regex("""\{"type":"([^"]+)","text":"([^"]+)"\}""").findAll(raw)
                        matches.forEach { match ->
                            val type = match.groupValues[1]
                            val text = match.groupValues[2].replace("\\n", "\n")
                            add(EditorBlock(type = type, content = text))
                        }
                    } catch (e: Exception) {
                        add(EditorBlock(type = "paragraph", content = initialPost.content))
                    }
                } else {
                    add(EditorBlock(type = "paragraph", content = initialPost.content))
                }
            }
            if (isEmpty()) {
                add(EditorBlock(type = "heading", content = "Welcome to Our New Post"))
                add(EditorBlock(type = "paragraph", content = "Write your engaging story here with Gutenberg mobile blocks."))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
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
                                text = "Mobile Block Editor • WordPress v6.6",
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
                        Button(
                            onClick = {
                                val jsonBuilder = StringBuilder("[")
                                blocks.forEachIndexed { index, block ->
                                    val escaped = block.content.replace("\"", "\\\"").replace("\n", "\\n")
                                    jsonBuilder.append("""{"type":"${block.type}","text":"$escaped"}""")
                                    if (index < blocks.size - 1) jsonBuilder.append(",")
                                }
                                jsonBuilder.append("]")
                                onSave(title, excerpt, jsonBuilder.toString(), status, postType, category)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("publish_post_button")
                        ) {
                            Icon(
                                imageVector = if (status == "published") Icons.Default.CloudUpload else Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (status == "published") "Publish" else "Save Draft")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Add Gutenberg Block:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = { blocks.add(EditorBlock(type = "heading", content = "New Heading")) },
                                label = { Text("H2 Heading") },
                                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("add_heading_block")
                            )
                            FilterChip(
                                selected = false,
                                onClick = { blocks.add(EditorBlock(type = "paragraph", content = "New paragraph content...")) },
                                label = { Text("Paragraph") },
                                leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("add_paragraph_block")
                            )
                            FilterChip(
                                selected = false,
                                onClick = { blocks.add(EditorBlock(type = "list", content = "First bullet item\nSecond bullet item")) },
                                label = { Text("List") },
                                leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = { blocks.add(EditorBlock(type = "quote", content = "Inspirational quote from an author.")) },
                                label = { Text("Quote") },
                                leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Post Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        var expandedStatus by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = status.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
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
                                    text = { Text("Published (Live)") },
                                    onClick = { status = "published"; expandedStatus = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Draft") },
                                    onClick = { status = "draft"; expandedStatus = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Scheduled") },
                                    onClick = { status = "scheduled"; expandedStatus = false }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        placeholder = { Text("Add title...") },
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_title_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = excerpt,
                        onValueChange = { excerpt = it },
                        label = { Text("Post Excerpt (SEO Summary)") },
                        placeholder = { Text("Brief description for search snippets...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Document Blocks (${blocks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    BlockItemCard(
                        block = block,
                        index = index,
                        totalBlocks = blocks.size,
                        onUpdateContent = { newText ->
                            blocks[index] = block.copy(content = newText)
                        },
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
                        onDelete = {
                            if (blocks.size > 1) {
                                blocks.removeAt(index)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun BlockItemCard(
    block: EditorBlock,
    index: Int,
    totalBlocks: Int,
    onUpdateContent: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("block_card_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (block.type) {
                        "heading" -> Icons.Default.Title
                        "paragraph" -> Icons.Default.FormatAlignLeft
                        "list" -> Icons.Default.FormatListBulleted
                        "quote" -> Icons.Default.FormatQuote
                        "button" -> Icons.Default.TouchApp
                        else -> Icons.Default.TextFields
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = block.type.replaceFirstChar { it.uppercase() } + " Block",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

            when (block.type) {
                "heading" -> {
                    OutlinedTextField(
                        value = block.content,
                        onValueChange = onUpdateContent,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "quote" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        OutlinedTextField(
                            value = block.content,
                            onValueChange = onUpdateContent,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = block.content,
                        onValueChange = onUpdateContent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
