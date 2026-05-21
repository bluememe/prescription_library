package com.aushadh.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.aushadh.app.data.MedicalRecord
import java.io.File
import kotlin.math.min

@Composable
fun RecordDetailScreen(
    record: MedicalRecord,
    onBack: () -> Unit,
    onUpdateTags: (String) -> Unit,
) {
    var editedTags by remember { mutableStateOf(record.tags) }
    var isEditing by remember { mutableStateOf(value = false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("BACK", fontSize = 18.sp) }
                Spacer(modifier = Modifier.weight(1f))
                
                // Share Button
                IconButton(
                    onClick = {
                        val file = File(record.filePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = if (record.fileType == "PDF") "application/pdf" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Prescription"))
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { 
                        if (isEditing) onUpdateTags(editedTags)
                        isEditing = !isEditing
                    }
                ) {
                    Text(if (isEditing) "SAVE" else "EDIT", fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedTags,
                    onValueChange = { editedTags = it },
                    label = { Text("Notes/Findings") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            val file = remember(record.filePath) { File(record.filePath) }
            
            if (!file.exists()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("File not found: ${file.name}")
                }
            } else if (record.fileType == "PDF") {
                PdfBookViewer(file)
            } else {
                ImageViewer(file)
            }
        }
    }
}

@Composable
fun PdfBookViewer(file: File) {
    val pageCount = remember(file) {
        var count = 0
        var fd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            count = renderer.pageCount
        } catch (e: Exception) {
            Log.e("PdfViewer", "Error reading PDF page count", e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { fd?.close() } catch (_: Exception) {}
        }
        count
    }

    if (pageCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not load PDF content")
        }
    } else {
        var currentPage by remember { mutableIntStateOf(0) }
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        // Reset zoom/pan when changing pages
        LaunchedEffect(currentPage) {
            scale = 1f
            offset = Offset.Zero
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Main Viewer Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentAlignment = Alignment.Center
            ) {
                PdfPageItem(file, currentPage)
            }

            // Navigation Controls (only show if not zoomed in or if at the edges)
            if (scale == 1f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Text(
                        "Page ${currentPage + 1} of $pageCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    IconButton(
                        onClick = { if (currentPage < (pageCount - 1)) currentPage++ },
                        enabled = currentPage < (pageCount - 1),
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ImageViewer(file: File) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offset += pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun PdfPageItem(file: File, pageIndex: Int) {
    val bitmap = remember(file, pageIndex) {
        var bmp: Bitmap? = null
        var fd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fd)
            if (pageIndex < renderer.pageCount) {
                page = renderer.openPage(pageIndex)
                
                val maxDimension = 2048
                val scale = min(
                    maxDimension.toFloat() / page.width,
                    maxDimension.toFloat() / page.height
                ).coerceAtMost(2.0f)

                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        } catch (e: Exception) {
            Log.e("PdfPageItem", "Error rendering PDF page $pageIndex", e)
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { fd?.close() } catch (_: Exception) {}
        }
        bmp
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Page ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    } ?: Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Error loading page")
    }
}
