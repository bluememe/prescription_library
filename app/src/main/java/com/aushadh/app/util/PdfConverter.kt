package com.aushadh.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object PdfConverter {
    fun imagesToPdf(imagePaths: List<String>, outputPdfFile: File): Boolean {
        if (imagePaths.isEmpty()) return false
        
        val pdfDocument = PdfDocument()
        try {
            imagePaths.forEachIndexed { index, path ->
                val originalBitmap = BitmapFactory.decodeFile(path) ?: return@forEachIndexed
                
                // Fix orientation based on EXIF
                val exif = ExifInterface(path)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                
                val bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                if (bitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                // Standard A4 size or similar logic could be used, but here we use image size
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }
            pdfDocument.writeTo(FileOutputStream(outputPdfFile))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            pdfDocument.close()
        }
    }
}
