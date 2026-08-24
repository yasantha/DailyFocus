package com.myday.dailyfocus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isDone: Boolean = false,
    val isMain: Boolean = false,
    val date: String
)
