package com.example.data.repository

import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import com.example.domain.sample.SampleDataGenerator
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksByDate(date)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteById(id)

    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
        val completedAt = if (newStatus == "COMPLETED") System.currentTimeMillis() else null
        taskDao.update(task.copy(status = newStatus, completedAt = completedAt))
    }

    suspend fun loadSyntheticSampleData() {
        val samples = SampleDataGenerator.getSampleTasks()
        taskDao.deleteAll()
        taskDao.insertAll(samples)
    }

    suspend fun clearAll() = taskDao.deleteAll()

    suspend fun getTaskCount(): Int = taskDao.getCount()
}
