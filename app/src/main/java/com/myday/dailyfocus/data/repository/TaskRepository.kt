package com.myday.dailyfocus.data.repository

import com.myday.dailyfocus.data.db.DailyStatsDao
import com.myday.dailyfocus.data.db.SessionDao
import com.myday.dailyfocus.data.db.TaskDao
import com.myday.dailyfocus.data.model.DailyStats
import com.myday.dailyfocus.data.model.Session
import com.myday.dailyfocus.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val dailyStatsDao: DailyStatsDao,
    private val sessionDao: SessionDao
) {
    fun tasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun tasksByIds(ids: List<Long>): Flow<List<Task>> = taskDao.getTasksByIds(ids)

    fun tasksForDateRange(startDate: String, endDate: String): Flow<List<Task>> =
        taskDao.getTasksForDateRange(startDate, endDate)

    suspend fun addTask(text: String, isMain: Boolean, date: String): Long =
        taskDao.insert(Task(text = text, isMain = isMain, date = date))

    suspend fun setTaskDone(task: Task, done: Boolean) = taskDao.update(task.copy(isDone = done))

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    fun statsForDate(date: String): Flow<DailyStats?> = dailyStatsDao.getStats(date)

    fun statsForDates(dates: List<String>): Flow<List<DailyStats>> = dailyStatsDao.getStatsForDates(dates)

    suspend fun upsertStats(stats: DailyStats) = dailyStatsDao.upsert(stats)

    suspend fun recordSession(taskId: Long, date: String, durationSeconds: Int): Long =
        sessionDao.insert(
            Session(
                taskId = taskId,
                date = date,
                durationSeconds = durationSeconds,
                completedAtMillis = System.currentTimeMillis()
            )
        )

    fun sessionsForDate(date: String): Flow<List<Session>> = sessionDao.getSessionsForDate(date)

    fun sessionsForDates(dates: List<String>): Flow<List<Session>> = sessionDao.getSessionsForDates(dates)

    fun sessionsForTask(taskId: Long): Flow<List<Session>> = sessionDao.getSessionsForTask(taskId)

    fun allSessions(): Flow<List<Session>> = sessionDao.getAllSessions()
}
