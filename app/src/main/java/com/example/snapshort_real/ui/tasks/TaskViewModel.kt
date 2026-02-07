package com.example.snapshort_real.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapshort_real.data.GeminiRepository
import com.example.snapshort_real.data.ScreenshotRepository
import com.example.snapshort_real.data.Task
import com.example.snapshort_real.data.TaskRepository
import com.example.snapshort_real.data.TaskSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val screenshotRepository: ScreenshotRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow(TaskFilterType.ALL)
    val filterType: StateFlow<TaskFilterType> = _filterType.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        taskRepository.getAllTasks(),
        _filterType
    ) { tasks, filter ->
        when (filter) {
            TaskFilterType.ALL -> tasks
            TaskFilterType.DONE -> tasks.filter { it.isCompleted }
            TaskFilterType.NOTE -> tasks.filter { !it.isCompleted }
        }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setFilterType(type: TaskFilterType) {
        _filterType.value = type
    }

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    // AI Generation State
    private val _aiTaskSuggestion = MutableStateFlow<TaskSuggestion?>(null)
    val aiTaskSuggestion: StateFlow<TaskSuggestion?> = _aiTaskSuggestion.asStateFlow()

    private val _isGeneratingAI = MutableStateFlow(false)
    val isGeneratingAI: StateFlow<Boolean> = _isGeneratingAI.asStateFlow()

    fun generateTaskInfo(imagePath: String) {
        viewModelScope.launch {
            _isGeneratingAI.value = true
            try {
                _aiTaskSuggestion.value = geminiRepository.generateTaskInfo(imagePath)
            } finally {
                _isGeneratingAI.value = false
            }
        }
    }

    fun clearAISuggestion() {
        _aiTaskSuggestion.value = null
    }

    fun getTaskById(id: Long) {
        viewModelScope.launch {
            _currentTask.value = taskRepository.getTaskById(id)
        }
    }

    fun getTaskByPath(path: String) {
        viewModelScope.launch {
            _currentTask.value = taskRepository.getTaskByPath(path)
        }
    }
    
    fun clearCurrentTask() {
        _currentTask.value = null
    }

    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                taskRepository.insertTask(task)
            } else {
                taskRepository.updateTask(task)
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            // Delete the image file first
            val file = File(task.imagePath)
            if (file.exists()) {
                screenshotRepository.deleteScreenshot(file)
            }
            // Then delete the task record
            taskRepository.deleteTask(task)
        }
    }

    /**
     * Removes only the task record from DB, keeping the image file.
     * Use this when demoting a task back to a snap.
     */
    fun removeTaskOnly(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    /**
     * Deletes only the image file (for snaps that are not associated with a task).
     */
    fun deleteImageOnly(imagePath: String) {
        viewModelScope.launch {
            val file = File(imagePath)
            if (file.exists()) {
                screenshotRepository.deleteScreenshot(file)
            }
        }
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            // Get completed tasks first to delete their images
            val completedTasks = taskRepository.getCompletedTasks()
            
            // Delete the image files
            completedTasks.forEach { task ->
                val file = File(task.imagePath)
                if (file.exists()) {
                    screenshotRepository.deleteScreenshot(file)
                }
            }
            
            // Delete the task records from database
            taskRepository.deleteCompletedTasks()
        }
    }
}
