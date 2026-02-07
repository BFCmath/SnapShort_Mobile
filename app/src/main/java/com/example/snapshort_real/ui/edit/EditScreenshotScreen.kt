package com.example.snapshort_real.ui.edit

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.snapshort_real.data.Task
import com.example.snapshort_real.ui.tasks.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditScreenshotScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Uri) -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Load bitmap efficiently
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                if (imageUri.scheme == "file") {
                    val path = imageUri.path
                    if (path != null) {
                        bitmap = BitmapFactory.decodeFile(path)
                    }
                } else {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
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
            onSave = { result, title, description, dueDate ->
                scope.launch {
                    val savedUri = saveBitmapToUri(context, result, imageUri)
                    // Only create a task if user provides title or due date
                    if (title.isNotEmpty() || dueDate != null) {
                        val task = Task(
                            imagePath = savedUri.path ?: savedUri.toString(),
                            title = title.ifEmpty { "Untitled Snap" },
                            description = description,
                            dueDate = dueDate
                        )
                        viewModel.saveTask(task)
                    }
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
    onSave: (Bitmap, String, String, Long?) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSizePx by remember { mutableStateOf(IntSize.Zero) }
    
    // Crop rectangle state
    var cropRectStart by remember { mutableStateOf<Offset?>(null) }
    var cropRectEnd by remember { mutableStateOf<Offset?>(null) }
    var isCropping by remember { mutableStateOf(false) }

    // Task Details State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }

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

                    drawRect(
                        color = Color.White,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw handles (simplified for brevity)
                }
            }
        }

        // Bottom Sheet for Editing (Task Details + Toolbar)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
            ) {
                 val focusRequester = remember { FocusRequester() }

                // Title Input
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequester.requestFocus() }
                    ),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text("Task Name", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(Color.White)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description Input
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (description.isEmpty()) {
                            Text("Add a short description...", style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp))
                        }
                        innerTextField()
                    },
                    cursorBrush = SolidColor(Color.White)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Toolbar Row (DueDate, Delete, Crop, Save)
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Date and Delete
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Date Picker
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    dueDate?.let { calendar.timeInMillis = it }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selectedDate = Calendar.getInstance()
                                            selectedDate.set(year, month, day)
                                            dueDate = selectedDate.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            if (dueDate != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dueDate!!)),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

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
                    }

                    // Right Side: Save Button
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
                            onSave(cropped, title, description, dueDate)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Save")
                    }
                }
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
