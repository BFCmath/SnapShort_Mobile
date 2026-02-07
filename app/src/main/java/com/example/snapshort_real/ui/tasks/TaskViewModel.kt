package com.example.snapshort_real.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapshort_real.data.Task
import com.example.snapshort_real.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow(TaskFilterType.ALL)
    val filterType: StateFlow<TaskFilterType> = _filterType.asStateFlow()

    val tasks: StateFlow<List<Task>> = kotlinx.coroutines.flow.combine(
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
            taskRepository.deleteTask(task)
        }
    }
}
