package com.aushadh.app

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aushadh.app.util.FileStorageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class StorageEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testFileCopyAndRename() {
        // 1. Create a dummy source file
        val tempFile = File(context.cacheDir, "test_input.pdf")
        tempFile.writeText("Dummy PDF Content")
        val uri = Uri.fromFile(tempFile)

        // 2. Run the copy logic
        val resultPath = FileStorageManager.copyFileToInternal(context, uri, "application/pdf")

        // 3. Verify renaming format: Aushadh_YYYYMMDD_HHMM.pdf
        assertNotNull("Result path should not be null", resultPath)
        val file = File(resultPath!!)
        assertTrue("File should exist in internal storage", file.exists())
        assertTrue("File should be in internal filesDir", file.absolutePath.contains(context.filesDir.absolutePath))
        
        val name = file.name
        assertTrue("Filename should start with Aushadh_", name.startsWith("Aushadh_"))
        assertTrue("Filename should end with .pdf", name.endsWith(".pdf"))
        
        // Regex check for Aushadh_YYYYMMDD_HHMM.pdf
        val regex = Regex("Aushadh_\\d{8}_\\d{4}\\.pdf")
        assertTrue("Filename $name should match format Aushadh_YYYYMMDD_HHMM.pdf", regex.matches(name))
        
        // Cleanup
        file.delete()
        tempFile.delete()
    }
}
