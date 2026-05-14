package com.aushadh.app.util

import android.content.Context
import java.io.*
import java.util.zip.*
import javax.crypto.*
import javax.crypto.spec.*

object BackupUtility {
    private const val ALGO = "AES/CBC/PKCS5Padding"

    fun exportBackup(context: Context, password: String, outFile: File): Boolean {
        return try {
            val key = generateKey(password)
            val cipher = Cipher.getInstance(ALGO).apply {
                init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(ByteArray(16)))
            }
            CipherOutputStream(FileOutputStream(outFile), cipher).use { cos ->
                ZipOutputStream(cos).use { zos ->
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            zos.putNextEntry(ZipEntry("files/${file.name}"))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    val dbFile = context.getDatabasePath("aushadh-db")
                    if (dbFile.exists()) {
                        zos.putNextEntry(ZipEntry("database/aushadh-db"))
                        dbFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun restoreBackup(context: Context, password: String, backupFile: File): Boolean {
        return try {
            val key = generateKey(password)
            val cipher = Cipher.getInstance(ALGO).apply {
                init(Cipher.DECRYPT_MODE, key, IvParameterSpec(ByteArray(16)))
            }
            CipherInputStream(FileInputStream(backupFile), cipher).use { cis ->
                ZipInputStream(cis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val destFile = if (entry.name.startsWith("database/")) {
                            context.getDatabasePath("aushadh-db")
                        } else {
                            File(context.filesDir, entry.name.removePrefix("files/"))
                        }
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    private fun generateKey(password: String): SecretKey {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }
}
