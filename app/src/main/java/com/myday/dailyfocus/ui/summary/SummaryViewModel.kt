package com.myday.dailyfocus.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myday.dailyfocus.data.model.Task
import com.myday.dailyfocus.data.prefs.UserPrefsStore
import com.myday.dailyfocus.data.repository.TaskRepository
import com.myday.dailyfocus.ui.components.Achievement
import com.myday.dailyfocus.ui.components.AchievementType
import com.myday.dailyfocus.ui.components.WeekDayFocus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class SummaryUiState(
    val completionPercent: Int = 0,
    val focusSecondsToday: Long = 0,
    val sessionsToday: Int = 0,
    val tasksDoneToday: Int = 0,
    val streak: Int = 0,
    val tasks: List<Task> = emptyList(),
    val weekDays: List<WeekDayFocus> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val showFoko: Boolean = true
)

class SummaryViewModel(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore
) : ViewModel() {

    private val today: String = LocalDate.now().toString()
    private val weekDates: List<LocalDate> = run {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        repository.tasksForDate(today),
        repository.statsForDate(today),
        repository.statsForDates(weekDates.map { it.toString() }),
        prefsStore.userPrefs
    ) { tasks, todayStats, weekStats, prefs ->
        val doneCount = tasks.count { it.isDone }
        val percent = if (tasks.isEmpty()) 0 else (doneCount * 100) / tasks.size

        val statsByDate = weekStats.associateBy { it.date }
        val weekDays = weekDates.map { date ->
            val dateStr = date.toString()
            WeekDayFocus(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                minutes = ((statsByDate[dateStr]?.focusSeconds ?: 0) / 60).toInt(),
                isToday = dateStr == today
            )
        }

        val achievements = listOf(
            Achievement(AchievementType.FIRST_FOCUS, unlocked = prefs.totalSessionsAllTime >= 1),
            Achievement(AchievementType.POWER_HOUR, unlocked = prefs.bestDayFocusSeconds >= 3600),
            Achievement(AchievementType.WEEK_WARRIOR, unlocked = prefs.currentStreak >= 7),
            Achievement(AchievementType.TASKMASTER, unlocked = prefs.totalTasksCompletedAllTime >= 20)
        )

        SummaryUiState(
            completionPercent = percent,
            focusSecondsToday = todayStats?.focusSeconds ?: 0,
            sessionsToday = todayStats?.sessions ?: 0,
            tasksDoneToday = doneCount,
            streak = prefs.currentStreak,
            tasks = tasks,
            weekDays = weekDays,
            achievements = achievements,
            showFoko = prefs.showFoko
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SummaryUiState())
}

class SummaryViewModelFactory(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SummaryViewModel(repository, prefsStore) as T
    }
}
