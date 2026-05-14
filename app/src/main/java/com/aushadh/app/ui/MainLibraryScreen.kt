package com.aushadh.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aushadh.app.data.MedicalRecord
import com.aushadh.app.util.BackupUtility
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLibraryScreen(
    records: List<MedicalRecord>,
    onRecordClick: (MedicalRecord) -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupResultPath by remember { mutableStateOf<String?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    // File Picker for Restore
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedRestoreUri = uri
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Aushadh",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showBackupDialog = true }) {
                            Icon(Icons.Default.Lock, contentDescription = "Security", tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            onSearch(it)
                        },
                        placeholder = { Text("Search records...", fontSize = 18.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(records) { record ->
                RecordCard(record = record, onClick = { onRecordClick(record) })
            }
        }

        // SECURITY DIALOG (Backup & Restore)
        if (showBackupDialog) {
            var password by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { 
                    showBackupDialog = false
                    selectedRestoreUri = null
                    backupResultPath = null
                },
                title = { Text(if (selectedRestoreUri == null) "Backup & Security" else "Restore Data", fontSize = 22.sp) },
                text = {
                    Column {
                        if (selectedRestoreUri == null && backupResultPath == null) {
                            Text("Secure your medical library.", fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Set Password for Backup") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text("CHOOSE FILE TO RESTORE")
                            }
                        } else if (selectedRestoreUri != null) {
                            Text("Decrypting: ${selectedRestoreUri?.lastPathSegment}")
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Enter Backup Password") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (backupResultPath != null) {
                            Text("✅ Backup Success!\nPath: $backupResultPath", color = Color(0xFF2E7D32))
                        }
                    }
                },
                confirmButton = {
                    val btnText = if (selectedRestoreUri != null) "CONFIRM RESTORE" else "BACKUP NOW"
                    if (backupResultPath == null) {
                        Button(onClick = {
                            if (selectedRestoreUri != null) {
                                // RESTORE LOGIC
                                val tempFile = File(context.cacheDir, "temp_restore.aushadh")
                                context.contentResolver.openInputStream(selectedRestoreUri!!)?.use { input ->
                                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                                }
                                if (BackupUtility.restoreBackup(context, password, tempFile)) {
                                    Toast.makeText(context, "Restore Success! Restarting...", Toast.LENGTH_LONG).show()
                                    showBackupDialog = false
                                } else {
                                    Toast.makeText(context, "Invalid Password or File", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // BACKUP LOGIC
                                val backupFile = File(context.getExternalFilesDir(null), "Aushadh_Backup.aushadh")
                                if (BackupUtility.exportBackup(context, password, backupFile)) {
                                    backupResultPath = backupFile.absolutePath
                                }
                            }
                        }) {
                            Text(btnText)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun RecordCard(record: MedicalRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.doctorName.isNotBlank()) "Dr. ${record.doctorName}" else "Unknown Doctor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.tags,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = record.fileType,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
