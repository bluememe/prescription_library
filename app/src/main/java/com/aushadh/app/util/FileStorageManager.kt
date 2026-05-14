package com.aushadh.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileStorageManager {
    fun copyFileToInternal(context: Context, uri: Uri, mimeType: String?): String? {
        val extension = if (mimeType == "application/pdf") "pdf" else "jpg"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "Aushadh_$timestamp.$extension"
        val destinationFile = File(context.filesDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output -> input.copyTo(output) }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
