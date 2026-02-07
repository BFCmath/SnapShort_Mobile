package com.example.snapshort_real.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun getTaskByPath(path: String): Task? = taskDao.getTaskByPath(path)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task): Int = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task): Int = taskDao.deleteTask(task)
}
