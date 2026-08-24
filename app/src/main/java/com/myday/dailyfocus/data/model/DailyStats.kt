package com.myday.dailyfocus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String,
    val focusSeconds: Long = 0,
    val sessions: Int = 0,
    val streak: Int = 0
)
