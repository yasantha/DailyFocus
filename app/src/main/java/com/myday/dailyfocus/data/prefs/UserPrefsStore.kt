package com.myday.dailyfocus.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class UserPrefs(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 20,
    val autoStartNextSession: Boolean = false,
    val showFoko: Boolean = true,
    val currentStreak: Int = 0,
    val lastCompletedDate: String = "",
    val totalSessionsAllTime: Int = 0,
    val totalTasksCompletedAllTime: Int = 0,
    val bestDayFocusSeconds: Long = 0,
    val dailyGoalMinutes: Int = 120,
    val streakThresholdMinutes: Int = 25,
    val hasCompletedOnboarding: Boolean = false
)

class UserPrefsStore(private val context: Context) {

    private object Keys {
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val BREAK_MINUTES = intPreferencesKey("break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val AUTO_START_NEXT_SESSION = booleanPreferencesKey("auto_start_next_session")
        val SHOW_FOKO = booleanPreferencesKey("show_foko")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LAST_COMPLETED_DATE = stringPreferencesKey("last_completed_date")
        val TOTAL_SESSIONS = intPreferencesKey("total_sessions_all_time")
        val TOTAL_TASKS_COMPLETED = intPreferencesKey("total_tasks_completed_all_time")
        val BEST_DAY_FOCUS_SECONDS = longPreferencesKey("best_day_focus_seconds")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val STREAK_THRESHOLD_MINUTES = intPreferencesKey("streak_threshold_minutes")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val userPrefs: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        UserPrefs(
            focusMinutes = prefs[Keys.FOCUS_MINUTES] ?: 25,
            breakMinutes = prefs[Keys.BREAK_MINUTES] ?: 5,
            longBreakMinutes = prefs[Keys.LONG_BREAK_MINUTES] ?: 20,
            autoStartNextSession = prefs[Keys.AUTO_START_NEXT_SESSION] ?: false,
            showFoko = prefs[Keys.SHOW_FOKO] ?: true,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            lastCompletedDate = prefs[Keys.LAST_COMPLETED_DATE] ?: "",
            totalSessionsAllTime = prefs[Keys.TOTAL_SESSIONS] ?: 0,
            totalTasksCompletedAllTime = prefs[Keys.TOTAL_TASKS_COMPLETED] ?: 0,
            bestDayFocusSeconds = prefs[Keys.BEST_DAY_FOCUS_SECONDS] ?: 0,
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINUTES] ?: 120,
            streakThresholdMinutes = prefs[Keys.STREAK_THRESHOLD_MINUTES] ?: 25,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
        )
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL_MINUTES] = minutes }
    }

    suspend fun setStreakThresholdMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.STREAK_THRESHOLD_MINUTES] = minutes }
    }

    suspend fun setFocusMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_MINUTES] = minutes }
    }

    suspend fun setBreakMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.BREAK_MINUTES] = minutes }
    }

    suspend fun setLongBreakMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.LONG_BREAK_MINUTES] = minutes }
    }

    suspend fun setAutoStartNextSession(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_NEXT_SESSION] = enabled }
    }

    suspend fun setShowFoko(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FOKO] = enabled }
    }

    suspend fun incrementTotalSessions() {
        context.dataStore.edit { it[Keys.TOTAL_SESSIONS] = (it[Keys.TOTAL_SESSIONS] ?: 0) + 1 }
    }

    suspend fun incrementTotalTasksCompleted() {
        context.dataStore.edit { it[Keys.TOTAL_TASKS_COMPLETED] = (it[Keys.TOTAL_TASKS_COMPLETED] ?: 0) + 1 }
    }

    suspend fun updateBestDayFocusSeconds(focusSeconds: Long) {
        context.dataStore.edit {
            val best = it[Keys.BEST_DAY_FOCUS_SECONDS] ?: 0
            if (focusSeconds > best) it[Keys.BEST_DAY_FOCUS_SECONDS] = focusSeconds
        }
    }

    suspend fun setStreak(streak: Int, completedDate: String) {
        context.dataStore.edit {
            it[Keys.CURRENT_STREAK] = streak
            it[Keys.LAST_COMPLETED_DATE] = completedDate
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { it[Keys.CURRENT_STREAK] = 0 }
    }
}
