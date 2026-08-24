package com.myday.dailyfocus.data.repository

import com.myday.dailyfocus.data.db.DailyStatsDao
import com.myday.dailyfocus.data.db.TaskDao
import com.myday.dailyfocus.data.model.DailyStats
import com.myday.dailyfocus.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val dailyStatsDao: DailyStatsDao
) {
    fun tasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    suspend fun addTask(text: String, isMain: Boolean, date: String): Long =
        taskDao.insert(Task(text = text, isMain = isMain, date = date))

    suspend fun setTaskDone(task: Task, done: Boolean) = taskDao.update(task.copy(isDone = done))

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    fun statsForDate(date: String): Flow<DailyStats?> = dailyStatsDao.getStats(date)

    fun statsForDates(dates: List<String>): Flow<List<DailyStats>> = dailyStatsDao.getStatsForDates(dates)

    suspend fun upsertStats(stats: DailyStats) = dailyStatsDao.upsert(stats)
}
