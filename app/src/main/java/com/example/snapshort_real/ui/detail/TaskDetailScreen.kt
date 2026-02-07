package com.example.snapshort_real.ui.detail

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.snapshort_real.data.Task
import com.example.snapshort_real.ui.tasks.TaskViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long? = null,
    imagePath: String? = null,
    onBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentTask by viewModel.currentTask.collectAsState()
    
    // Local state for editing to avoid constant DB updates
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var viewedImagePath by remember { mutableStateOf<String?>(null) }

    // Load task or init with image path
    LaunchedEffect(taskId, imagePath) {
        if (taskId != null && taskId != -1L) {
            viewModel.getTaskById(taskId)
        } else if (imagePath != null) {
            // Check if task exists for this image, otherwise it's a new task context
            viewModel.getTaskByPath(imagePath)
        }
    }

    LaunchedEffect(currentTask) {
        currentTask?.let {
            title = it.title
            description = it.description ?: ""
            dueDate = it.dueDate
            viewedImagePath = it.imagePath
        } ?: run {
             // If no task found but we have an image path, init for new task
             if (imagePath != null) {
                 viewedImagePath = imagePath
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

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Full Screen Image
            viewedImagePath?.let { path ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(path))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit, // Show full image logic
                    modifier = Modifier.fillMaxSize()
                )
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
                                Color.Black.copy(alpha = 0.6f), // Darker start
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
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
