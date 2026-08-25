package com.myday.dailyfocus.ui.home

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myday.dailyfocus.MainActivity
import com.myday.dailyfocus.R
import com.myday.dailyfocus.data.model.DailyStats
import com.myday.dailyfocus.data.model.Task
import com.myday.dailyfocus.data.prefs.UserPrefsStore
import com.myday.dailyfocus.data.repository.TaskRepository
import com.myday.dailyfocus.service.TimerForegroundService
import com.myday.dailyfocus.ui.components.foko.FokoMessageCategory
import com.myday.dailyfocus.ui.components.foko.FokoMessageCue
import com.myday.dailyfocus.ui.components.foko.FokoState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class TimerMode { FOCUS, BREAK }
enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class TimerUiState(
    val mode: TimerMode = TimerMode.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 20,
    val sessionsToday: Int = 0,
    // Set once a focus session count lands on a multiple of SESSIONS_PER_LONG_BREAK; consumed
    // (and cleared) the moment takeABreak() starts the next break.
    val nextBreakIsLong: Boolean = false,
    // Which duration the break currently loaded into totalSeconds/remainingSeconds actually is,
    // so pause/resume/reset within that same break keep using the right length.
    val isCurrentBreakLong: Boolean = false
) {
    fun idleDurationMinutes(): Int = when {
        mode == TimerMode.FOCUS -> focusMinutes
        isCurrentBreakLong -> longBreakMinutes
        else -> breakMinutes
    }
}

private const val SESSIONS_PER_LONG_BREAK = 4

data class HomeUiState(
    val dateLabel: String = "",
    val mainTask: Task? = null,
    val secondaryTasks: List<Task> = emptyList(),
    val completionPercent: Int = 0,
    val streak: Int = 0,
    val focusSecondsToday: Long = 0,
    val timer: TimerUiState = TimerUiState(),
    val allTasksDone: Boolean = false,
    val autoStartNextSession: Boolean = false,
    val showFoko: Boolean = true,
    val fokoState: FokoState = FokoState.Idle
)

class HomeViewModel(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore,
    private val appContext: Context
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    private val _timerState = MutableStateFlow(TimerUiState())
    private var timerJob: Job? = null

    private val _transientFokoState = MutableStateFlow<FokoState?>(null)
    private val _fokoMessageCue = MutableStateFlow<FokoMessageCue?>(null)
    private var fokoCueCounter = 0L
    private val fokoRandom = kotlin.random.Random(System.currentTimeMillis())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.tasksForDate(today),
        repository.statsForDate(today),
        prefsStore.userPrefs,
        _timerState,
        _transientFokoState
    ) { tasks, stats, prefs, timer, transientFoko ->
        val mainTask = tasks.firstOrNull { it.isMain }
        val secondaryTasks = tasks.filter { !it.isMain }
        val allTasks = listOfNotNull(mainTask) + secondaryTasks
        val doneCount = allTasks.count { it.isDone }
        val percent = if (allTasks.isEmpty()) 0 else (doneCount * 100) / allTasks.size
        val allDone = allTasks.isNotEmpty() && doneCount == allTasks.size

        val baseFokoState = when {
            allDone && mainTask != null -> FokoState.Celebrating
            timer.mode == TimerMode.BREAK && timer.status != TimerStatus.IDLE -> FokoState.BreakTime
            timer.mode == TimerMode.FOCUS && timer.status == TimerStatus.RUNNING -> FokoState.Focusing
            timer.mode == TimerMode.FOCUS && timer.status == TimerStatus.PAUSED -> FokoState.Focusing
            else -> FokoState.Idle
        }

        HomeUiState(
            dateLabel = formatDateLabel(today),
            mainTask = mainTask,
            secondaryTasks = secondaryTasks,
            completionPercent = percent,
            streak = prefs.currentStreak,
            focusSecondsToday = stats?.focusSeconds ?: 0,
            timer = timer.copy(sessionsToday = stats?.sessions ?: 0),
            allTasksDone = allDone,
            autoStartNextSession = prefs.autoStartNextSession,
            showFoko = prefs.showFoko,
            fokoState = transientFoko ?: baseFokoState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val fokoMessageCue: StateFlow<FokoMessageCue?> = _fokoMessageCue

    init {
        // _timerState is the source of truth startTimer()/resetTimer() read from, so it must
        // stay in sync with the persisted durations, not just the derived uiState shown on screen.
        viewModelScope.launch {
            prefsStore.userPrefs.collect { prefs ->
                _timerState.update { current ->
                    val withDurations = current.copy(
                        focusMinutes = prefs.focusMinutes,
                        breakMinutes = prefs.breakMinutes,
                        longBreakMinutes = prefs.longBreakMinutes
                    )
                    if (withDurations.status == TimerStatus.IDLE) {
                        val totalSeconds = withDurations.idleDurationMinutes() * 60
                        withDurations.copy(totalSeconds = totalSeconds, remainingSeconds = totalSeconds)
                    } else {
                        withDurations
                    }
                }
            }
        }
        viewModelScope.launch {
            var lastAwardedDate = ""
            uiState.collect { state ->
                if (state.allTasksDone && state.mainTask != null && lastAwardedDate != today) {
                    val prefs = prefsStore.userPrefs.first()
                    if (prefs.lastCompletedDate != today) {
                        val newStreak = if (isYesterday(prefs.lastCompletedDate)) prefs.currentStreak + 1 else 1
                        prefsStore.setStreak(newStreak, today)
                        prefsStore.updateBestDayFocusSeconds(state.focusSecondsToday)
                        queueFokoMessage(FokoMessageCategory.ALL_TASKS_DONE)
                        if (newStreak in setOf(3, 7, 14, 30)) {
                            queueFokoStreakMessage(newStreak)
                        }
                    }
                    lastAwardedDate = today
                }
            }
        }
        viewModelScope.launch {
            val initialTasks = repository.tasksForDate(today).first()
            queueFokoMessage(
                if (initialTasks.none { it.isMain }) FokoMessageCategory.NO_TASKS else FokoMessageCategory.RETURNING_USER
            )
        }
    }

    private fun queueFokoMessage(category: FokoMessageCategory) {
        fokoCueCounter++
        _fokoMessageCue.value = FokoMessageCue(category, fokoRandom.nextInt(4), fokoCueCounter)
    }

    private fun queueFokoStreakMessage(days: Int) {
        fokoCueCounter++
        _fokoMessageCue.value = FokoMessageCue(FokoMessageCategory.STREAK_MILESTONE, days, fokoCueCounter)
        triggerTransientFoko(FokoState.StreakAlert, 1500)
    }

    private fun triggerTransientFoko(state: FokoState, durationMs: Long) {
        _transientFokoState.value = state
        viewModelScope.launch {
            delay(durationMs)
            if (_transientFokoState.value == state) _transientFokoState.value = null
        }
    }

    fun setShowFoko(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setShowFoko(enabled) }
    }

    private fun isYesterday(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return false
        return try {
            LocalDate.parse(dateStr) == LocalDate.parse(today).minusDays(1)
        } catch (e: Exception) {
            false
        }
    }

    fun setMainTask(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            uiState.value.mainTask?.let { repository.deleteTask(it) }
            repository.addTask(text.trim(), isMain = true, date = today)
        }
    }

    fun addSecondaryTask(text: String) {
        if (text.isBlank()) return
        if (uiState.value.secondaryTasks.size >= 3) return
        viewModelScope.launch {
            repository.addTask(text.trim(), isMain = false, date = today)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val markingDone = !task.isDone
            repository.setTaskDone(task, markingDone)
            if (markingDone) {
                prefsStore.incrementTotalTasksCompleted()
                queueFokoMessage(FokoMessageCategory.TASK_COMPLETED)
                triggerTransientFoko(FokoState.TaskComplete, 700)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun startTimer() {
        val current = _timerState.value
        if (current.status == TimerStatus.RUNNING) return
        if (current.status == TimerStatus.IDLE && current.mode == TimerMode.FOCUS) {
            queueFokoMessage(FokoMessageCategory.TIMER_STARTED)
        }
        val remaining = if (current.status == TimerStatus.PAUSED) current.remainingSeconds else current.totalSeconds
        _timerState.update { it.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining) }
        runTicker()
        startTimerService(remaining, current.mode, current.isCurrentBreakLong)
    }

    fun pauseTimer() {
        timerJob?.cancel()
        val wasRunningFocus = _timerState.value.status == TimerStatus.RUNNING && _timerState.value.mode == TimerMode.FOCUS
        _timerState.update {
            if (it.status == TimerStatus.RUNNING) it.copy(status = TimerStatus.PAUSED) else it
        }
        stopTimerService()
        if (wasRunningFocus) queueFokoMessage(FokoMessageCategory.TIMER_PAUSED)
    }

    fun resetTimer() {
        timerJob?.cancel()
        stopTimerService()
        _timerState.update {
            val totalSeconds = it.idleDurationMinutes() * 60
            it.copy(status = TimerStatus.IDLE, totalSeconds = totalSeconds, remainingSeconds = totalSeconds)
        }
    }

    fun startNewFocusSession() {
        // No stopTimerService() here: this is only ever reached right after onTimerFinished(),
        // which already stopped the service unless auto-start is about to restart it immediately
        // (stopping then instantly restarting the same foreground service crashes the app).
        timerJob?.cancel()
        _timerState.update {
            val totalSeconds = it.focusMinutes * 60
            it.copy(
                mode = TimerMode.FOCUS,
                status = TimerStatus.IDLE,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isCurrentBreakLong = false
            )
        }
    }

    fun takeABreak() {
        // See startNewFocusSession() -- no stopTimerService() here for the same reason.
        timerJob?.cancel()
        _timerState.update {
            val useLong = it.nextBreakIsLong
            val totalSeconds = (if (useLong) it.longBreakMinutes else it.breakMinutes) * 60
            it.copy(
                mode = TimerMode.BREAK,
                status = TimerStatus.IDLE,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                isCurrentBreakLong = useLong,
                nextBreakIsLong = false
            )
        }
        queueFokoMessage(FokoMessageCategory.BREAK_STARTED)
    }

    fun updateFocusDuration(minutes: Int) {
        viewModelScope.launch { prefsStore.setFocusMinutes(minutes.coerceIn(1, 120)) }
    }

    fun updateBreakDuration(minutes: Int) {
        viewModelScope.launch { prefsStore.setBreakMinutes(minutes.coerceIn(1, 30)) }
    }

    fun updateLongBreakDuration(minutes: Int) {
        viewModelScope.launch { prefsStore.setLongBreakMinutes(minutes.coerceIn(1, 60)) }
    }

    fun setAutoStartNextSession(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setAutoStartNextSession(enabled) }
    }

    private fun runTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var secondsSinceLastEncouragement = 0
            while (_timerState.value.status == TimerStatus.RUNNING && _timerState.value.remainingSeconds > 0) {
                delay(1000)
                if (_timerState.value.status != TimerStatus.RUNNING) return@launch
                _timerState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
                if (_timerState.value.mode == TimerMode.FOCUS) {
                    secondsSinceLastEncouragement++
                    if (secondsSinceLastEncouragement >= 600) {
                        secondsSinceLastEncouragement = 0
                        queueFokoMessage(FokoMessageCategory.TIMER_RUNNING)
                    }
                }
            }
            if (_timerState.value.status == TimerStatus.RUNNING && _timerState.value.remainingSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    private fun onTimerFinished() {
        val finishedState = _timerState.value
        _timerState.update { it.copy(status = TimerStatus.FINISHED) }
        playCompletionAlert(finishedState.mode)

        if (finishedState.mode == TimerMode.FOCUS) {
            viewModelScope.launch {
                val existing = repository.statsForDate(today).first() ?: DailyStats(date = today)
                val newSessionCount = existing.sessions + 1
                repository.upsertStats(
                    existing.copy(
                        focusSeconds = existing.focusSeconds + finishedState.totalSeconds,
                        sessions = newSessionCount
                    )
                )
                prefsStore.incrementTotalSessions()
                val longBreakDue = newSessionCount % SESSIONS_PER_LONG_BREAK == 0
                _timerState.update { it.copy(nextBreakIsLong = longBreakDue) }
                maybeAutoStartNext(TimerMode.FOCUS)
            }
        } else {
            viewModelScope.launch { maybeAutoStartNext(TimerMode.BREAK) }
        }
    }

    /**
     * Stopping the foreground service and immediately calling startForegroundService() again
     * for the next phase is a real Android race (ForegroundServiceDidNotStartInTimeException) --
     * the OS can tear the service down mid-restart and crash the app. So when auto-start is on,
     * we skip the stop entirely and let the next startTimerService() call simply update the
     * still-running service in place; we only stop it when the timer is actually staying idle.
     */
    private suspend fun maybeAutoStartNext(finishedMode: TimerMode) {
        val autoStart = prefsStore.userPrefs.first().autoStartNextSession
        if (!autoStart) {
            stopTimerService()
            return
        }
        if (finishedMode == TimerMode.FOCUS) takeABreak() else startNewFocusSession()
        startTimer()
    }

    private fun startTimerService(remainingSeconds: Int, mode: TimerMode, isLongBreak: Boolean) {
        val endTimeMillis = System.currentTimeMillis() + remainingSeconds * 1000L
        val modeLabel = when {
            mode == TimerMode.FOCUS -> TimerForegroundService.MODE_FOCUS
            isLongBreak -> TimerForegroundService.MODE_LONG_BREAK
            else -> TimerForegroundService.MODE_BREAK
        }
        val intent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_END_TIME_MILLIS, endTimeMillis)
            putExtra(TimerForegroundService.EXTRA_MODE_LABEL, modeLabel)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    private fun stopTimerService() {
        val intent = Intent(appContext, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    private fun playCompletionAlert(mode: TimerMode) {
        vibrate()
        postCompletionNotification(mode)
    }

    private fun vibrate() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }

    private fun postCompletionNotification(mode: TimerMode) {
        ensureAlertChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = appContext.getString(if (mode == TimerMode.FOCUS) R.string.notif_focus_complete_title else R.string.notif_break_complete_title)
        val text = appContext.getString(if (mode == TimerMode.FOCUS) R.string.notif_focus_complete_text else R.string.notif_break_complete_text)

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 1, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID_ALERT, notification)
    }

    private fun ensureAlertChannel() {
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID_ALERTS) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID_ALERTS,
            appContext.getString(R.string.notif_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.notif_channel_alerts_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID_ALERTS = "timer_alerts"
        const val NOTIFICATION_ID_ALERT = 2002
    }
}

private fun formatDateLabel(dateStr: String): String {
    val date = LocalDate.parse(dateStr)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$weekday, $month ${date.dayOfMonth}"
}

class HomeViewModelFactory(
    private val repository: TaskRepository,
    private val prefsStore: UserPrefsStore,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, prefsStore, appContext) as T
    }
}
