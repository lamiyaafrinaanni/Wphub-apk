package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SEOMetadata(
    val metaTitle: String = "",
    val metaDescription: String = "",
    val focusKeyword: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SEOSettingsDrawer(
    initialSEO: SEOMetadata,
    postUrl: String = "https://example.com/blog-post",
    onDismiss: () -> Unit,
    onSaveSEO: (SEOMetadata) -> Unit
) {
    var metaTitle by remember { mutableStateOf(initialSEO.metaTitle) }
    var metaDescription by remember { mutableStateOf(initialSEO.metaDescription) }
    var focusKeyword by remember { mutableStateOf(initialSEO.focusKeyword) }

    // SEO Scoring Logic
    val seoScore = remember(metaTitle, metaDescription, focusKeyword) {
        var score = 10
        if (metaTitle.isNotBlank()) {
            score += 20
            if (metaTitle.length in 40..60) score += 15
        }
        if (metaDescription.isNotBlank()) {
            score += 20
            if (metaDescription.length in 120..160) score += 15
        }
        if (focusKeyword.isNotBlank()) {
            score += 10
            if (metaTitle.lowercase().contains(focusKeyword.lowercase())) score += 5
            if (metaDescription.lowercase().contains(focusKeyword.lowercase())) score += 5
        }
        score.coerceAtMost(100)
    }

    val scoreColor = when {
        seoScore >= 80 -> Color(0xFF10B981) // Green
        seoScore >= 50 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("seo_settings_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SEO Optimization Suite",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "RankMath & Yoast Powered Snippet Optimization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close SEO panel")
                }
            }

            // SEO Score Meter
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scoreColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$seoScore",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                    Column {
                        Text(
                            text = if (seoScore >= 80) "SEO Score: Excellent" else if (seoScore >= 50) "SEO Score: Good (Needs Polish)" else "SEO Score: Poor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = "Optimize focus keyword placements to hit 100% score.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Google Snippet Preview Block
            Text(
                text = "Live Google Search Snippet Preview",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = postUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Text(
                        text = metaTitle.ifBlank { "Untitled Post | Meta Snippet Title Preview" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A0DAB) // Google blue link color
                        )
                    )
                    Text(
                        text = metaDescription.ifBlank { "Provide a meta description snippet below to control how search engine spiders index your page." },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4D5156), // Google grey snippet color
                        lineHeight = 16.sp
                    )
                }
            }

            // Keyword Configuration Input
            OutlinedTextField(
                value = focusKeyword,
                onValueChange = { focusKeyword = it },
                label = { Text("Focus Keyword") },
                placeholder = { Text("e.g. gutenberg block editor") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Meta Title Configuration Input with Length Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = metaTitle,
                    onValueChange = { metaTitle = it },
                    label = { Text("Meta Title") },
                    placeholder = { Text("Enter custom SEO Meta Title...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusColor = if (metaTitle.length in 40..60) Color(0xFF10B981) else Color(0xFFF59E0B)
                    Text(
                        text = if (metaTitle.length in 40..60) "Length optimal (40-60 chars)" else "Recommended: 40-60 chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Text(
                        text = "${metaTitle.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Meta Description Configuration Input with Length Meter
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = metaDescription,
                    onValueChange = { metaDescription = it },
                    label = { Text("Meta Description") },
                    placeholder = { Text("Enter meta description summary for SEO search index indexing...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusColor = if (metaDescription.length in 120..160) Color(0xFF10B981) else Color(0xFFF59E0B)
                    Text(
                        text = if (metaDescription.length in 120..160) "Length optimal (120-160 chars)" else "Recommended: 120-160 chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Text(
                        text = "${metaDescription.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // SEO Checks list
            Text(
                text = "SEO Analysis Checklist",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SEOCheckRow(
                    condition = metaTitle.isNotBlank() && metaTitle.length in 40..60,
                    text = "Meta title length is in optimal range"
                )
                SEOCheckRow(
                    condition = metaDescription.isNotBlank() && metaDescription.length in 120..160,
                    text = "Meta description length is in optimal range"
                )
                SEOCheckRow(
                    condition = focusKeyword.isNotBlank() && metaTitle.lowercase().contains(focusKeyword.lowercase()),
                    text = "Focus keyword matches post title"
                )
                SEOCheckRow(
                    condition = focusKeyword.isNotBlank() && metaDescription.lowercase().contains(focusKeyword.lowercase()),
                    text = "Focus keyword used in Meta Description"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save SEO button
            Button(
                onClick = {
                    onSaveSEO(
                        SEOMetadata(
                            metaTitle = metaTitle,
                            metaDescription = metaDescription,
                            focusKeyword = focusKeyword
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_seo_settings_btn")
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save SEO Settings")
            }
        }
    }
}

@Composable
fun SEOCheckRow(condition: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (condition) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (condition) Color(0xFF10B981) else Color(0xFFF59E0B),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (condition) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
