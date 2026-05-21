package com.aushadh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.room.Room.databaseBuilder
import com.aushadh.app.data.AppDatabase
import com.aushadh.app.data.MedicalRecord
import com.aushadh.app.ui.*
import com.aushadh.app.ui.theme.AushadhTheme
import com.aushadh.app.util.FileStorageManager
import com.aushadh.app.util.PdfConverter
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = databaseBuilder(applicationContext, AppDatabase::class.java, "aushadh-db").build()

        setContent {
            AushadhTheme {
                var searchQuery by remember { mutableStateOf("") }
                var records by remember { mutableStateOf(listOf<MedicalRecord>()) }
                var selectedRecord by remember { mutableStateOf<MedicalRecord?>(null) }
                val scope = rememberCoroutineScope()
                
                var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
                var showCamera by remember { mutableStateOf(false) }
                var showImportDialog by remember { mutableStateOf(false) }
                var capturedImagesForPdf by remember { mutableStateOf<List<String>?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) showCamera = true
                    else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }

                // Single source of truth for records, reacting to searchQuery
                LaunchedEffect(searchQuery) {
                    db.medicalRecordDao().searchRecords(searchQuery).collect { 
                        records = it 
                    }
                }

                if (showCamera) {
                    CameraCaptureScreen(
                        onImagesCaptured = { paths ->
                            showCamera = false
                            capturedImagesForPdf = paths
                            showImportDialog = true
                        },
                        onCancel = { showCamera = false }
                    )
                } else if (selectedRecord == null) {
                    MainLibraryScreen(
                        records = records,
                        searchQuery = searchQuery,
                        onRecordClick = { selectedRecord = it },
                        onSearch = { searchQuery = it },
                        onAddClick = {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCamera = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                } else {
                    RecordDetailScreen(
                        record = selectedRecord!!,
                        onBack = { selectedRecord = null },
                        onUpdateTags = { newTags ->
                            scope.launch {
                                db.medicalRecordDao().update(selectedRecord!!.copy(tags = newTags))
                            }
                        }
                    )
                }

                if (showImportDialog) {
                    ImportDialog(
                        onConfirm = { name ->
                            showImportDialog = false
                            scope.launch {
                                handleFinalImport(name, pendingImportUri, capturedImagesForPdf)
                                pendingImportUri = null
                                capturedImagesForPdf = null
                            }
                        },
                        onDismiss = {
                            showImportDialog = false
                            pendingImportUri = null
                            capturedImagesForPdf = null
                        }
                    )
                }

                // Handle Shared Intent
                LaunchedEffect(intent) {
                    if (intent?.action == Intent.ACTION_SEND) {
                        @Suppress("DEPRECATION")
                        pendingImportUri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM)
                        }
                        if (pendingImportUri != null) showImportDialog = true
                    }
                }
            }
        }
    }

    private suspend fun handleFinalImport(name: String, uri: Uri?, imagePaths: List<String>?) {
        var finalPath: String? = null
        var type = "IMAGE"

        if (uri != null) {
            val mimeType = contentResolver.getType(uri)
            finalPath = FileStorageManager.copyFileToInternal(this, uri, mimeType)
            type = if (mimeType == "application/pdf") "PDF" else "IMAGE"
        } else if (imagePaths != null) {
            val outPdf = File(filesDir, "Aushadh_${System.currentTimeMillis()}.pdf")
            if (PdfConverter.imagesToPdf(imagePaths, outPdf)) {
                finalPath = outPdf.absolutePath
                type = "PDF"
            }
        }

        finalPath?.let {
            db.medicalRecordDao().insert(
                MedicalRecord(
                    filePath = it,
                    timestamp = System.currentTimeMillis(),
                    fileType = type,
                    tags = "",
                    doctorName = name
                )
            )
        }
    }
}
