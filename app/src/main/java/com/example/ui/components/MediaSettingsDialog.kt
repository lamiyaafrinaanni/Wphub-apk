package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter

data class MediaCropSettings(
    val aspectRatio: String = "16:9", // "16:9", "4:3", "1:1", "21:9", "Original"
    val scaleMode: String = "Cover", // "Cover", "Fit", "Stretch", "Manual"
    val zoom: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSettingsDialog(
    imageUrl: String,
    initialSettings: MediaCropSettings,
    onDismiss: () -> Unit,
    onApplySettings: (MediaCropSettings) -> Unit
) {
    var aspectRatio by remember { mutableStateOf(initialSettings.aspectRatio) }
    var scaleMode by remember { mutableStateOf(initialSettings.scaleMode) }
    var zoom by remember { mutableFloatStateOf(initialSettings.zoom) }
    var offsetX by remember { mutableFloatStateOf(initialSettings.offsetX) }
    var offsetY by remember { mutableFloatStateOf(initialSettings.offsetY) }

    val aspectRatios = listOf("16:9", "4:3", "1:1", "21:9", "Original")
    val scaleModes = listOf("Cover", "Fit", "Stretch")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Featured Image Optimizer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configure dimension aspect ratios, zoom & focal fit",
                                    style = MaterialTheme.typography.bodySmall,
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
                                    onApplySettings(
                                        MediaCropSettings(
                                            aspectRatio = aspectRatio,
                                            scaleMode = scaleMode,
                                            zoom = zoom,
                                            offsetX = offsetX,
                                            offsetY = offsetY
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("apply_media_settings_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // LIVE PREVIEW BLOCK
                    Text(
                        text = "Live Responsive Canvas Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val previewAspectRatio = when (aspectRatio) {
                        "16:9" -> 1.77f
                        "4:3" -> 1.33f
                        "1:1" -> 1.0f
                        "21:9" -> 2.33f
                        else -> 1.77f // Default ratio
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(previewAspectRatio)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val painter = rememberAsyncImagePainter(model = imageUrl)
                        val contentScale = when (scaleMode) {
                            "Cover" -> ContentScale.Crop
                            "Fit" -> ContentScale.Fit
                            "Stretch" -> ContentScale.FillBounds
                            else -> ContentScale.Crop
                        }

                        Image(
                            painter = painter,
                            contentDescription = "Live Preview Canvas",
                            contentScale = contentScale,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoom,
                                    scaleY = zoom,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }

                    // CONTROL PANEL
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Aspect Ratio Section
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                aspectRatios.forEach { ratio ->
                                    val isSelected = aspectRatio == ratio
                                    ElevatedFilterChip(
                                        selected = isSelected,
                                        onClick = { aspectRatio = ratio },
                                        label = { Text(ratio) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Scale Mode Section
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Object Alignment Fit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scaleModes.forEach { mode ->
                                    val isSelected = scaleMode == mode
                                    ElevatedFilterChip(
                                        selected = isSelected,
                                        onClick = { scaleMode = mode },
                                        label = { Text(mode) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Zoom Slider Section
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Focal Zoom Level: ${"%.1f".format(zoom)}x", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            Slider(
                                value = zoom,
                                onValueChange = { zoom = it },
                                valueRange = 1.0f..3.0f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Manual Offset Controls (Only relevant with zoom or custom crop)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Horizontal Adjust", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${offsetX.toInt()} px", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = offsetX,
                                onValueChange = { offsetX = it },
                                valueRange = -300f..300f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Vertical Adjust", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${offsetY.toInt()} px", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = offsetY,
                                onValueChange = { offsetY = it },
                                valueRange = -300f..300f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    zoom = 1.0f
                                    offsetX = 0.0f
                                    offsetY = 0.0f
                                    aspectRatio = "16:9"
                                    scaleMode = "Cover"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset to Standard Defaults")
                            }
                        }
                    }
                }
            }
        }
    }
}
