package com.example.snapshort_real.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.snapshort_real.ui.theme.Snapshort_realTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PreviewActivity : ComponentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        
        android.util.Log.d("PreviewActivity", "onCreate called. URI string: $imageUriString")
        
        if (imageUriString == null) {
            android.util.Log.e("PreviewActivity", "Image URI is null, finishing.")
            finish()
            return
        }
        val imageUri = Uri.parse(imageUriString)

        val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
        }

        setContent {
            Snapshort_realTheme {
                PreviewScreen(
                    imageUri = imageUri,
                    onDismiss = { finish() },
                    onEdit = {
                        try {
                            val currentUriString = intent.getStringExtra("IMAGE_URI") ?: imageUriString
                            val editIntent = android.content.Intent(this@PreviewActivity, com.example.snapshort_real.ui.edit.EditScreenshotActivity::class.java).apply {
                               putExtra("IMAGE_URI", currentUriString)
                            }
                            editLauncher.launch(editIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("PreviewActivity", "Failed to launch edit activity", e)
                            android.widget.Toast.makeText(this, "Error opening editor: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PreviewScreen(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Initial state: Hidden to the left (-screenWidth) or bottom. 
    // Let's do a slide UP and IN from bottom-left.
    val offsetX = remember { Animatable(-screenWidthPx) } // Start hidden left
    val scope = rememberCoroutineScope()
    
    // State to track if user is interacting, to pause auto-dismiss
    var isUserInteracting by remember { mutableStateOf(false) }

    // Entrance Animation
    LaunchedEffect(Unit) {
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    // Auto-dismiss logic
    LaunchedEffect(isUserInteracting) {
        if (!isUserInteracting) {
            // Wait 2.5 seconds (matching snapshort logic)
            kotlinx.coroutines.delay(2500)
            // Animate off-screen to the left
            offsetX.animateTo(
                targetValue = -screenWidthPx,
                animationSpec = tween(durationMillis = 300)
            )
            onDismiss()
        }
    }

    val insets = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(insets) // Respect system bars
            .padding(start = 24.dp, bottom = 24.dp), // Extra breathing room
        contentAlignment = Alignment.BottomStart
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(120.dp, 200.dp) // Thumbnail size
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { 
                            isUserInteracting = true 
                        },
                        onDragEnd = { 
                            scope.launch {
                                // If dragged significantly to the left, dismiss
                                if (offsetX.value < -150f) {
                                    offsetX.animateTo(
                                        targetValue = -screenWidthPx,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                    onDismiss()
                                } else {
                                    // Snap back and resume auto-dismiss behavior
                                    offsetX.animateTo(0f)
                                    isUserInteracting = false 
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f)
                                isUserInteracting = false
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            // Apply drag, limiting how far right they can pull (resistance)
                            val newOffset = offsetX.value + dragAmount
                            if (newOffset <= 50f) { // Allow slight overdrag to right
                                offsetX.snapTo(newOffset)
                            }
                        }
                    }
                }
                .clickable { onEdit() },
            shape = RoundedCornerShape(16.dp), // Slightly rounder
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (imageUri.scheme == "file" && imageUri.path != null) java.io.File(imageUri.path!!) else imageUri)
                    .listener(
                        onStart = { android.util.Log.d("PreviewActivity", "Coil: onStart") },
                        onCancel = { android.util.Log.d("PreviewActivity", "Coil: onCancel") },
                        onError = { _, result -> android.util.Log.e("PreviewActivity", "Coil: onError - ${result.throwable.message}", result.throwable) },
                        onSuccess = { _, _ -> android.util.Log.d("PreviewActivity", "Coil: onSuccess") }
                    )
                    .crossfade(true)
                    .build()
            )

            Image(
                painter = painter,
                contentDescription = "Screenshot Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
