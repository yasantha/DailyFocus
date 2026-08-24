package com.myday.dailyfocus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myday.dailyfocus.data.model.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun getStats(date: String): Flow<DailyStats?>

    @Query("SELECT * FROM daily_stats WHERE date IN (:dates) ORDER BY date ASC")
    fun getStatsForDates(dates: List<String>): Flow<List<DailyStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStats)
}
