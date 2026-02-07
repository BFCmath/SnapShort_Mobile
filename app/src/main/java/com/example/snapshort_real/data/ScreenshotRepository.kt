package com.example.snapshort_real.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.FileObserver
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ScreenshotRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "ScreenshotRepository"
        private const val SCREENSHOTS_DIR = "screenshots"
    }
    
    private val screenshotsDir: File by lazy {
        File(context.filesDir, SCREENSHOTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    fun getScreenshots(): List<File> = listScreenshots()

    fun observeScreenshots(): Flow<List<File>> = callbackFlow {
        trySend(listScreenshots())

        val observer = object : FileObserver(
            screenshotsDir.absolutePath,
            FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM or FileObserver.CLOSE_WRITE
        ) {
            override fun onEvent(event: Int, path: String?) {
                trySend(listScreenshots())
            }
        }

        observer.startWatching()
        awaitClose { observer.stopWatching() }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    
    fun copyScreenshotToInternal(sourceFile: File): Boolean {
        return try {
            val destFile = File(screenshotsDir, "screenshot_${System.currentTimeMillis()}.png")
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Screenshot saved: ${destFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy screenshot", e)
            false
        }
    }
    
    fun saveScreenshot(bitmap: Bitmap): File? {
        Log.d(TAG, "saveScreenshot: Saving bitmap to file...")
        return try {
            val file = File(screenshotsDir, "screenshot_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            Log.d(TAG, "Screenshot saved: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save screenshot", e)
            null
        }
    }
    
    suspend fun loadBitmap(file: File): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap", e)
            null
        }
    }
    
    fun deleteScreenshot(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete screenshot", e)
            false
        }
    }
    
    fun deleteAllScreenshots() {
        screenshotsDir.listFiles()?.forEach { it.delete() }
    }

    private fun listScreenshots(): List<File> {
        return screenshotsDir.listFiles()
            ?.filter { it.isFile && it.extension in listOf("png", "jpg", "jpeg") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
