package com.example.snapshort_real.ui.edit

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.snapshort_real.ui.theme.Snapshort_realTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString == null) {
            Toast.makeText(this, "Error: No image found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val imageUri = Uri.parse(imageUriString)

        setContent {
            Snapshort_realTheme {
                EditScreenshotScreen(
                    imageUri = imageUri,
                    onBack = { finish() },
                    onDelete = {
                        val deleted = when (imageUri.scheme) {
                            "file" -> {
                                val path = imageUri.path
                                if (path != null) java.io.File(path).delete() else false
                            }
                            else -> {
                                try {
                                    contentResolver.delete(imageUri, null, null) > 0
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        }
                        if (!deleted) {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    },
                    onSave = { savedUri ->
                        val result = android.content.Intent().apply {
                            putExtra("EDITED_URI", savedUri.toString())
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    }
                )
            }
        }
    }
}
