package com.aushadh.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportDialog(
    initialDoctorName: String = "",
    onConfirm: (doctorName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var doctorName by remember { mutableStateOf(initialDoctorName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Doctor/Record Name") },
        text = {
            Column {
                Text("Please provide a name for this record.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor/Record Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(doctorName) },
                enabled = doctorName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
