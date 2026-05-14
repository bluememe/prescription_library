package com.aushadh.app.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aushadh.app.data.MedicalRecord
import java.io.File

@Composable
fun RecordDetailScreen(
    record: MedicalRecord,
    onBack: () -> Unit,
    onUpdateTags: (String) -> Unit
) {
    var editedTags by remember { mutableStateOf(record.tags) }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("BACK", fontSize = 18.sp) }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { 
                    if (isEditing) onUpdateTags(editedTags)
                    isEditing = !isEditing
                }) {
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

            // Zoomable Viewer Area
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                ZoomableBox {
                    if (record.fileType == "PDF") {
                        PdfView(record.filePath)
                    } else {
                        AsyncImage(
                            model = File(record.filePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableBox(content: @Composable () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f) // Allow up to 5x zoom
                    offset += pan
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
        content()
    }
}

@Composable
fun PdfView(filePath: String) {
    val context = LocalContext.current
    val bitmap = remember(filePath) {
        try {
            val file = File(filePath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888) // Higher res for zoom
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bmp
        } catch (e: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}
