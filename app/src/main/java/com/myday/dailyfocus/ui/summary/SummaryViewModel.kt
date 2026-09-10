package com.myday.dailyfocus.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myday.dailyfocus.data.model.Session
import com.myday.dailyfocus.data.model.Task
import com.myday.dailyfocus.data.prefs.UserPrefs
import com.myday.dailyfocus.data.prefs.UserPrefsStore
import com.myday.dailyfocus.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class ProgressRange { WEEK, MONTH, YEAR }

data class DayBar(val label: String, val minutes: Int, val metGoal: Boolean, val isToday: Boolean)
data class ConsistencyCell(val date: String, val minutes: Int)
data class TaskBreakdown(val name: String, val seconds: Int, val shareOfTotal: Float)

data class ProgressUiState(
    val range: ProgressRange = ProgressRange.WEEK,
    val totalSeconds: Int = 0,
    val deltaVsPreviousSeconds: Int = 0,
    val sessionsCount: Int = 0,
    val daysMetGoal: Int = 0,
    val totalDaysInRange: Int = 7,
    val bars: List<DayBar> = emptyList(),
    val goalMinutes: Int = 120,
    val consistency: List<ConsistencyCell> = emptyList(),
    val longestStreakInWindow: Int = 0,
    val taskBreakdown: List<TaskBreakdown> = emptyList()
)

private const val CONSISTENCY_DAYS = 14

class SummaryViewModel(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val _range = MutableStateFlow(ProgressRange.WEEK)

    fun setRange(range: ProgressRange) {
        _range.update { range }
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        _range,
        prefsStore.userPrefs,
        repository.allSessions(),
        repository.tasksForDateRange(today.minusDays(370).toString(), today.toString())
    ) { range, prefs, sessions, tasks ->
        buildState(range, prefs, sessions, tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    private fun buildState(
        range: ProgressRange,
        prefs: UserPrefs,
        sessions: List<Session>,
        tasks: List<Task>
    ): ProgressUiState {
        val goalMinutes = prefs.dailyGoalMinutes
        val goalSeconds = goalMinutes * 60
        val byDate: Map<String, List<Session>> = sessions.groupBy { it.date }

        val (periodStart, periodEnd, previousStart, previousEnd) = when (range) {
            ProgressRange.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY)
                listOf(monday, monday.plusDays(6), monday.minusWeeks(1), monday.minusWeeks(1).plusDays(6))
            }
            ProgressRange.MONTH -> {
                val start = today.withDayOfMonth(1)
                val end = today.withDayOfMonth(today.lengthOfMonth())
                val prevMonth = start.minusMonths(1)
                listOf(start, end, prevMonth, prevMonth.withDayOfMonth(prevMonth.lengthOfMonth()))
            }
            ProgressRange.YEAR -> {
                val start = today.withDayOfYear(1)
                val end = today.withDayOfYear(today.lengthOfYear())
                val prevYear = start.minusYears(1)
                listOf(start, end, prevYear, prevYear.withDayOfYear(prevYear.lengthOfYear()))
            }
        }

        val periodDates = datesBetween(periodStart, periodEnd)
        val previousDates = datesBetween(previousStart, previousEnd)

        fun secondsFor(date: String) = byDate[date]?.sumOf { it.durationSeconds } ?: 0
        fun sessionsCountFor(dates: List<LocalDate>) = dates.sumOf { byDate[it.toString()]?.size ?: 0 }

        val totalSeconds = periodDates.sumOf { secondsFor(it.toString()) }
        val previousTotalSeconds = previousDates.sumOf { secondsFor(it.toString()) }
        val daysMetGoal = periodDates.count { secondsFor(it.toString()) >= goalSeconds }

        val bars: List<DayBar> = when (range) {
            ProgressRange.WEEK -> periodDates.map { date ->
                val secs = secondsFor(date.toString())
                DayBar(
                    label = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    minutes = secs / 60,
                    metGoal = secs >= goalSeconds,
                    isToday = date == today
                )
            }
            ProgressRange.MONTH -> {
                // Bucket the month into ~7 week-chunks so the bar chart stays readable.
                periodDates.chunked((periodDates.size / 7).coerceAtLeast(1)).mapIndexed { idx, chunk ->
                    val secs = chunk.sumOf { secondsFor(it.toString()) }
                    val avgSecs = secs / chunk.size
                    DayBar(
                        label = "W${idx + 1}",
                        minutes = secs / 60,
                        metGoal = avgSecs >= goalSeconds,
                        isToday = chunk.contains(today)
                    )
                }
            }
            ProgressRange.YEAR -> {
                periodDates.groupBy { it.month }.entries.sortedBy { it.key }.map { (month, days) ->
                    val secs = days.sumOf { secondsFor(it.toString()) }
                    DayBar(
                        label = month.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        minutes = secs / 60,
                        metGoal = (secs / days.size) >= goalSeconds,
                        isToday = days.contains(today)
                    )
                }
            }
        }

        val consistencyDates = (0 until CONSISTENCY_DAYS).map { today.minusDays((CONSISTENCY_DAYS - 1 - it).toLong()) }
        val consistency = consistencyDates.map { date ->
            ConsistencyCell(date = date.toString(), minutes = secondsFor(date.toString()) / 60)
        }
        var longestStreak = 0
        var current = 0
        for (date in consistencyDates) {
            if (secondsFor(date.toString()) >= goalSeconds) {
                current++
                longestStreak = maxOf(longestStreak, current)
            } else {
                current = 0
            }
        }

        val taskNameById = tasks.associate { it.id to it.text }
        val periodDateSet = periodDates.map { it.toString() }.toSet()
        val periodSessions = sessions.filter { it.date in periodDateSet }
        val breakdownByTaskId = periodSessions.groupBy { it.taskId }
            .mapValues { (_, list) -> list.sumOf { it.durationSeconds } }
        val breakdownTotal = breakdownByTaskId.values.sum().coerceAtLeast(1)
        val taskBreakdown = breakdownByTaskId.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { (taskId, secs) ->
                TaskBreakdown(
                    name = taskNameById[taskId] ?: "Deleted task",
                    seconds = secs,
                    shareOfTotal = secs.toFloat() / breakdownTotal
                )
            }

        return ProgressUiState(
            range = range,
            totalSeconds = totalSeconds,
            deltaVsPreviousSeconds = totalSeconds - previousTotalSeconds,
            sessionsCount = sessionsCountFor(periodDates),
            daysMetGoal = daysMetGoal,
            totalDaysInRange = periodDates.size,
            bars = bars,
            goalMinutes = goalMinutes,
            consistency = consistency,
            longestStreakInWindow = longestStreak,
            taskBreakdown = taskBreakdown
        )
    }

    private fun datesBetween(start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, endInclusive)
        if (days < 0) return emptyList()
        return (0..days).map { start.plusDays(it) }
    }
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
