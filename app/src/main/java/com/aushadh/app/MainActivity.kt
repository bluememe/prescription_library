package com.aushadh.app

import android.net.Uri
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.room.Room.databaseBuilder
import com.aushadh.app.data.AppDatabase
import com.aushadh.app.data.MedicalRecord
import com.aushadh.app.ui.MainLibraryScreen
import com.aushadh.app.ui.RecordDetailScreen
import com.aushadh.app.ui.theme.AushadhTheme
import com.aushadh.app.util.FileStorageManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Corrected Kotlin syntax: AppDatabase::class.java
        db = databaseBuilder(applicationContext, AppDatabase::class.java, "aushadh-db").build()

        setContent {
            AushadhTheme {
                var records by remember { mutableStateOf(listOf<MedicalRecord>()) }
                var selectedRecord by remember { mutableStateOf<MedicalRecord?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    db.medicalRecordDao().getAllRecords().collect { records = it }
                }

                if (selectedRecord == null) {
                    MainLibraryScreen(
                        records = records,
                        onRecordClick = { selectedRecord = it },
                        onSearch = { query ->
                            scope.launch {
                                db.medicalRecordDao().searchRecords(query).collect { records = it }
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
            }
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            uri?.let {
                val mimeType = contentResolver.getType(it)
                val path = FileStorageManager.copyFileToInternal(this, it, mimeType)
                if (path != null) {
                    lifecycleScope.launch {
                        db.medicalRecordDao().insert(
                            MedicalRecord(
                                filePath = path,
                                timestamp = System.currentTimeMillis(),
                                fileType = if (mimeType == "application/pdf") "PDF" else "IMAGE",
                                tags = "",
                                doctorName = "Imported File"
                            )
                        )
                    }
                }
            }
        }
    }
}
