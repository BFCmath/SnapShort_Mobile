package com.example.snapshort_real.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.net.Uri
import com.example.snapshort_real.data.ScreenshotRepository
import com.example.snapshort_real.ui.PreviewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class ScreenshotAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ScreenshotAccessibility"
        const val ACTION_TAKE_SCREENSHOT = "com.example.snapshort_real.ACTION_TAKE_SCREENSHOT"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: ScreenshotRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = ScreenshotRepository(this)

        Log.d(TAG, "Accessibility Service connected.")
        performScreenshot()
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        performScreenshot()
        super.onDestroy()
        // serviceScope.cancel()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    /**
     * The core screenshot logic
     */
    private fun performScreenshot() {
        Toast.makeText(this, "Taking Screenshot...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Taking screenshot now...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ (Android 11) Native Screenshot API
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                getMainExecutor(),
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        Log.d(TAG, "Screenshot captured successfully")

                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )

                        if (hardwareBitmap != null) {
                            processAndSaveBitmap(hardwareBitmap, screenshot)
                        } else {
                            Log.e(TAG, "Failed to wrap hardware buffer")
                            screenshot.hardwareBuffer.close()
                            showToast("Screenshot capture failed (No Bitmap)")
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Screenshot failed with error code: $errorCode")
                        val msg = when (errorCode) {
                            1 -> "Blocked by secure content"
                            2 -> "Accessibility permission issue"
                            else -> "Screenshot failed (Error $errorCode)"
                        }
                        showToast(msg)
                    }
                }
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28-29 (Android 9-10) Fallback
            val result = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (result) {
                showToast("Screenshot taken (Saved to Gallery)")
            } else {
                showToast("System failed to take screenshot")
            }
        } else {
            showToast("Screenshot requires Android 9+")
        }
    }

    /**
     * Handles the heavy lifting of saving the bitmap on a background thread
     */
    private fun processAndSaveBitmap(hardwareBitmap: Bitmap, screenshot: AccessibilityService.ScreenshotResult) {
        serviceScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting bitmap save process...")
            try {
                // Hardware bitmaps cannot be saved directly to file, must copy to software
                val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)

                // Clean up hardware resources immediately
                hardwareBitmap.recycle()
                screenshot.hardwareBuffer.close()

                if (softwareBitmap != null) {
                    // Save to internal storage via your Repository
                    val savedFile = repository.saveScreenshot(softwareBitmap)
                    softwareBitmap.recycle()

                    withContext(Dispatchers.Main) {
                        if (savedFile != null) {
                            Log.d(TAG, "Saved screenshot: ${savedFile.absolutePath}")
                            openPreview(savedFile)
                        } else {
                            showToast("Failed to save screenshot file")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to process image")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing screenshot", e)
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }
        }
    }

    private fun openPreview(file: java.io.File) {
        try {
            val intent = Intent(this, PreviewActivity::class.java).apply {
                putExtra("IMAGE_URI", Uri.fromFile(file).toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            Log.d(TAG, "Starting PreviewActivity...")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PreviewActivity", e)
            showToast("Failed to open preview")
        }
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}