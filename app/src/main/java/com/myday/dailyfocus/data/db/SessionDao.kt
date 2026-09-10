package com.myday.dailyfocus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.myday.dailyfocus.data.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY completedAtMillis ASC")
    fun getSessionsForDate(date: String): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE date IN (:dates) ORDER BY completedAtMillis ASC")
    fun getSessionsForDates(dates: List<String>): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE taskId = :taskId ORDER BY completedAtMillis ASC")
    fun getSessionsForTask(taskId: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY date ASC")
    fun getAllSessions(): Flow<List<Session>>
}
