package com.example.snapshort_real.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

@Composable
fun EditScreenshotScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Load bitmap efficiently
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    bitmap?.let { btm ->
        EditImageContent(
            originalBitmap = btm,
            onBack = onBack,
            onDelete = onDelete,
            onSave = { result ->
                scope.launch {
                    val savedUri = saveBitmapToUri(context, result, imageUri)
                    onSave(savedUri)
                }
            }
        )
    }
}

@Composable
fun EditImageContent(
    originalBitmap: Bitmap,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableStateOf(IntSize.Zero) }
    
    // Crop rectangle state (in screen coordinates relative to the image view)
    var cropRectStart by remember { mutableStateOf<Offset?>(null) }
    var cropRectEnd by remember { mutableStateOf<Offset?>(null) }
    var isCropping by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Image display with Zoom/Pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boxSizePx = it.size }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop
                        var event = awaitPointerEvent()
                        do {
                            if (event.changes.size < 2) {
                                event = awaitPointerEvent()
                                continue
                            }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (!pastTouchSlop) {
                                val zoomMotion = abs(1f - zoomChange) * 100f
                                if (zoomMotion > touchSlop || panChange.getDistance() > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }
                            if (pastTouchSlop) {
                                scale = max(0.5f, min(3f, scale * zoomChange))
                                offset += panChange
                                event.changes.forEach { it.consume() }
                            }
                            event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    }
                }
                // Separate pointer input for drawing the crop rect
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            if (!isCropping) {
                                cropRectStart = start
                                cropRectEnd = start
                                isCropping = true
                            }
                        },
                        onDrag = { change, _ ->
                            if (isCropping) {
                                cropRectEnd = change.position
                            }
                        },
                        onDragEnd = {
                           isCropping = false
                           // Keep the rect
                        },
                        onDragCancel = {
                           isCropping = false
                        }
                    )
                }
        ) {
            Image(
                bitmap = originalBitmap.asImageBitmap(),
                contentDescription = "Editing Image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = TransformOrigin(0f, 0f)
                    ),
                contentScale = ContentScale.Fit
            )
            
            // Draw Crop Overlay
            if (cropRectStart != null && cropRectEnd != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = cropRectStart!!
                    val end = cropRectEnd!!
                    
                    val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
                    val size = Size(kotlin.math.abs(start.x - end.x), kotlin.math.abs(start.y - end.y))
                    
                    // Dim area outside selection
                    val canvasSize = this.size
                    val outerPath = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, canvasSize.width, canvasSize.height))
                    }
                    val cropPath = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(topLeft, size))
                    }
                    val dimmedPath = Path.combine(PathOperation.Difference, outerPath, cropPath)
                    
                    drawPath(
                        path = dimmedPath,
                        color = Color.Black.copy(alpha = 0.7f),
                        style = Fill
                    )

                    // Draw border
                    drawRect(
                        color = Color.White,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw L-shaped corner handles
                    val handleLen = 20.dp.toPx()
                    val handleStroke = 3.dp.toPx()
                    
                    // Top-Left
                    drawLine(Color.White, topLeft, topLeft + Offset(handleLen, 0f), handleStroke)
                    drawLine(Color.White, topLeft, topLeft + Offset(0f, handleLen), handleStroke)
                    
                    // Top-Right
                    val topRight = topLeft + Offset(size.width, 0f)
                    drawLine(Color.White, topRight, topRight + Offset(-handleLen, 0f), handleStroke)
                    drawLine(Color.White, topRight, topRight + Offset(0f, handleLen), handleStroke)
                    
                    // Bottom-Left
                    val bottomLeft = topLeft + Offset(0f, size.height)
                    drawLine(Color.White, bottomLeft, bottomLeft + Offset(handleLen, 0f), handleStroke)
                    drawLine(Color.White, bottomLeft, bottomLeft + Offset(0f, -handleLen), handleStroke)
                    
                    // Bottom-Right
                    val bottomRight = topLeft + Offset(size.width, size.height)
                    drawLine(Color.White, bottomRight, bottomRight + Offset(-handleLen, 0f), handleStroke)
                    drawLine(Color.White, bottomRight, bottomRight + Offset(0f, -handleLen), handleStroke)
                }
            }
        }

        // Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
            
            if (cropRectStart != null) {
                IconButton(onClick = {
                    cropRectStart = null
                    cropRectEnd = null
                    isCropping = false
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Crop", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val cropped = cropBitmapFromSelection(
                        originalBitmap = originalBitmap,
                        cropRectStart = cropRectStart,
                        cropRectEnd = cropRectEnd,
                        boxSizePx = boxSizePx,
                        scale = scale,
                        offset = offset
                    )
                    onSave(cropped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        }
    }
}

private fun cropBitmapFromSelection(
    originalBitmap: Bitmap,
    cropRectStart: Offset?,
    cropRectEnd: Offset?,
    boxSizePx: IntSize,
    scale: Float,
    offset: Offset
): Bitmap {
    if (cropRectStart == null || cropRectEnd == null) return originalBitmap
    if (boxSizePx.width == 0 || boxSizePx.height == 0) return originalBitmap

    val bw = originalBitmap.width.toFloat()
    val bh = originalBitmap.height.toFloat()
    val boxW = boxSizePx.width.toFloat()
    val boxH = boxSizePx.height.toFloat()

    val baseScale = min(boxW / bw, boxH / bh)
    val drawnW = bw * baseScale
    val drawnH = bh * baseScale
    val baseTopLeft = Offset((boxW - drawnW) / 2f, (boxH - drawnH) / 2f)
    val totalScale = baseScale * scale

    val start = cropRectStart
    val end = cropRectEnd
    val leftScreen = min(start.x, end.x)
    val topScreen = min(start.y, end.y)
    val rightScreen = max(start.x, end.x)
    val bottomScreen = max(start.y, end.y)

    val left = ((leftScreen - baseTopLeft.x - offset.x) / totalScale).coerceIn(0f, bw)
    val top = ((topScreen - baseTopLeft.y - offset.y) / totalScale).coerceIn(0f, bh)
    val right = ((rightScreen - baseTopLeft.x - offset.x) / totalScale).coerceIn(0f, bw)
    val bottom = ((bottomScreen - baseTopLeft.y - offset.y) / totalScale).coerceIn(0f, bh)

    val cropW = (right - left).toInt()
    val cropH = (bottom - top).toInt()
    if (cropW < 10 || cropH < 10) return originalBitmap

    val safeLeft = left.toInt().coerceIn(0, originalBitmap.width - 1)
    val safeTop = top.toInt().coerceIn(0, originalBitmap.height - 1)
    val safeWidth = cropW.coerceAtMost(originalBitmap.width - safeLeft)
    val safeHeight = cropH.coerceAtMost(originalBitmap.height - safeTop)

    return Bitmap.createBitmap(originalBitmap, safeLeft, safeTop, safeWidth, safeHeight)
}

suspend fun saveBitmapToUri(context: Context, bitmap: Bitmap, originalUri: Uri): Uri {
    return withContext(Dispatchers.IO) {
        val originalPath = originalUri.path
        if (originalUri.scheme == "file" && originalPath != null) {
            try {
                val file = File(originalPath)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                return@withContext originalUri
            } catch (_: Exception) {
                // fall through to cache
            }
        }

        val file = File(context.cacheDir, "edited_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    }
}
