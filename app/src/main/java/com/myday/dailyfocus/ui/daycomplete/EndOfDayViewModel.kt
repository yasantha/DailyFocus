package com.myday.dailyfocus.ui.daycomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myday.dailyfocus.data.prefs.UserPrefsStore
import com.myday.dailyfocus.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class DayTaskEntry(val taskId: Long, val name: String, val isDone: Boolean, val focusSeconds: Int)

data class EndOfDayUiState(
    val dateLabel: String = "",
    val totalFocusSeconds: Int = 0,
    val taskCount: Int = 0,
    val goalMet: Boolean = false,
    val streak: Int = 0,
    val entries: List<DayTaskEntry> = emptyList(),
    val incompleteTasks: List<DayTaskEntry> = emptyList(),
    val sessionsCount: Int = 0,
    val avgSessionSeconds: Int = 0,
    val bestWindowLabel: String = "—"
)

class EndOfDayViewModel(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val uiState: StateFlow<EndOfDayUiState> = combine(
        repository.tasksForDate(today),
        repository.sessionsForDate(today),
        prefsStore.userPrefs
    ) { tasks, sessions, prefs ->
        val byTaskId = sessions.groupBy { it.taskId }
        val entries = tasks.map { task ->
            val secs = byTaskId[task.id]?.sumOf { it.durationSeconds } ?: 0
            DayTaskEntry(task.id, task.text, task.isDone, secs)
        }
        val totalSeconds = sessions.sumOf { it.durationSeconds }
        val avgSession = if (sessions.isEmpty()) 0 else totalSeconds / sessions.size

        val bestWindow = if (sessions.isEmpty()) {
            "—"
        } else {
            val hourCounts = sessions.groupingBy {
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.completedAtMillis), ZoneId.systemDefault()).hour
            }.eachCount()
            val peakHour = hourCounts.maxByOrNull { it.value }?.key ?: 9
            "${formatHour(peakHour)}–${formatHour(peakHour + 2)}"
        }

        EndOfDayUiState(
            dateLabel = formatDateLabel(today),
            totalFocusSeconds = totalSeconds,
            taskCount = tasks.size,
            goalMet = totalSeconds >= prefs.dailyGoalMinutes * 60,
            streak = prefs.currentStreak,
            entries = entries,
            incompleteTasks = entries.filter { !it.isDone },
            sessionsCount = sessions.size,
            avgSessionSeconds = avgSession,
            bestWindowLabel = bestWindow
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EndOfDayUiState())

    fun carryOverToTomorrow(entry: DayTaskEntry) {
        viewModelScope.launch {
            val tomorrow = LocalDate.now().plusDays(1).toString()
            repository.addTask(entry.name, isMain = false, date = tomorrow)
        }
    }
}

private fun formatHour(hour24: Int): String {
    val h = ((hour24 % 24) + 24) % 24
    val period = if (h < 12) "am" else "pm"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$h12$period"
}

private fun formatDateLabel(dateStr: String): String {
    val date = LocalDate.parse(dateStr)
    val weekday = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
    return "$weekday, $month ${date.dayOfMonth}"
}

class EndOfDayViewModelFactory(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EndOfDayViewModel(repository, prefsStore) as T
    }
}
