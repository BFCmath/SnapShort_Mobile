package com.example.snapshort_real.ui.detail

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.snapshort_real.data.Task
import com.example.snapshort_real.ui.tasks.TaskViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long? = null,
    imagePath: String? = null,
    allImagePaths: List<String> = emptyList(),
    initialIndex: Int = 0,
    onBack: () -> Unit,
    onNavigateToImage: ((String) -> Unit)? = null,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentTask by viewModel.currentTask.collectAsState()
    
    // Current image index for navigation
    var currentIndex by remember { mutableStateOf(initialIndex) }
    
    // Local state for editing to avoid constant DB updates
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var viewedImagePath by remember { mutableStateOf<String?>(null) }
    
    // FocusRequester for swipe up to focus title
    val titleFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    
    // Swipe gesture state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    
    // Track swipe direction for correct animation (true = forward/next, false = backward/previous)
    var isSwipingForward by remember { mutableStateOf(true) }

    // Determine current image path based on navigation
    val effectiveImagePath = if (allImagePaths.isNotEmpty() && currentIndex in allImagePaths.indices) {
        allImagePaths[currentIndex]
    } else {
        imagePath
    }

    // Load task or init with image path
    LaunchedEffect(taskId, effectiveImagePath) {
        if (taskId != null && taskId != -1L) {
            viewModel.getTaskById(taskId)
        } else if (effectiveImagePath != null) {
            // Check if task exists for this image, otherwise it's a new task context
            viewModel.getTaskByPath(effectiveImagePath)
        }
    }

    LaunchedEffect(currentTask, effectiveImagePath) {
        currentTask?.let {
            title = it.title
            description = it.description ?: ""
            dueDate = it.dueDate
            viewedImagePath = it.imagePath
        } ?: run {
             // If no task found but we have an image path, init for new task
             if (effectiveImagePath != null) {
                 viewedImagePath = effectiveImagePath
                 title = ""
                 description = ""
                 dueDate = null
             }
        }
    }

    // Save on exit
    fun saveTask() {
        if (viewedImagePath != null) {
            if (title.isNotEmpty() || dueDate != null) {
                val task = currentTask?.copy(
                    title = title.ifEmpty { "Untitled Snap" },
                    description = description,
                    dueDate = dueDate
                ) ?: Task(
                    imagePath = viewedImagePath!!,
                    title = title.ifEmpty { "Untitled Snap" },
                    description = description,
                    dueDate = dueDate
                )
                viewModel.saveTask(task)
            } else if (currentTask != null) {
                // Was a task, but now empty -> Demote to Snap (delete from DB, keep image)
                viewModel.removeTaskOnly(currentTask!!)
            }
        }
    }
    
    // Navigation functions
    fun navigateToNext() {
        if (allImagePaths.isNotEmpty() && currentIndex < allImagePaths.size - 1) {
            currentIndex++
            // Reset task state for new image
            viewModel.clearCurrentTask()
        }
    }
    
    fun navigateToPrevious() {
        if (allImagePaths.isNotEmpty() && currentIndex > 0) {
            currentIndex--
            // Reset task state for new image
            viewModel.clearCurrentTask()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    // Show image position indicator if in gallery mode
                    if (allImagePaths.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1} / ${allImagePaths.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // saveTask() // Remove auto-save
                            onBack() 
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentTask != null) {
                                // Delete task and its image
                                viewModel.deleteTask(currentTask!!)
                            } else if (viewedImagePath != null) {
                                // Delete just the image file (no task associated)
                                viewModel.deleteImageOnly(viewedImagePath!!)
                            }
                            onBack()
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .pointerInput(allImagePaths) {
                    detectDragGestures(
                        onDragStart = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            dragOffsetY += dragAmount.y
                        },
                        onDragEnd = {
                            val swipeThreshold = 100f
                            val absX = abs(dragOffsetX)
                            val absY = abs(dragOffsetY)
                            
                            // Determine dominant direction
                            if (absX > absY && absX > swipeThreshold) {
                                // Horizontal swipe
                                if (dragOffsetX < 0) {
                                    // Swipe left -> next image
                                    isSwipingForward = true
                                    navigateToNext()
                                } else {
                                    // Swipe right -> previous image
                                    isSwipingForward = false
                                    navigateToPrevious()
                                }
                            } else if (absY > absX && absY > swipeThreshold) {
                                // Vertical swipe
                                if (dragOffsetY > 0) {
                                    // Swipe down -> go back
                                    onBack()
                                } else {
                                    // Swipe up -> focus title field
                                    titleFocusRequester.requestFocus()
                                }
                            }
                            
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        }
                    )
                }
        ) {
            // Full Screen Image with animation
            AnimatedContent(
                targetState = viewedImagePath,
                transitionSpec = {
                    if (isSwipingForward) {
                        // Swipe left (next): new image comes from right
                        slideInHorizontally { width -> width } togetherWith
                            slideOutHorizontally { width -> -width }
                    } else {
                        // Swipe right (previous): new image comes from left
                        slideInHorizontally { width -> -width } togetherWith
                            slideOutHorizontally { width -> width }
                    }
                },
                label = "ImageTransition"
            ) { path ->
                path?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(it))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Bottom Sheet for Editing
            // Gradient to make text readable
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
                        .padding(top = paddingValues.calculateTopPadding())
                        .padding(bottom = WindowInsets.ime.union(WindowInsets.navigationBars).asPaddingValues().calculateBottomPadding())
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Input
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.focusRequester(titleFocusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { descriptionFocusRequester.requestFocus() }
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
                        modifier = Modifier.focusRequester(descriptionFocusRequester),
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
                    
                    // Due Date Picker and Save Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dueDate != null) {
                                    SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(dueDate!!))
                                } else {
                                    "Set Due Date"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                saveTask()
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Save")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
