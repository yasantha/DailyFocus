package com.myday.dailyfocus

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.myday.dailyfocus.ads.AdManager
import com.myday.dailyfocus.data.db.AppDatabase
import com.myday.dailyfocus.data.prefs.UserPrefsStore
import com.myday.dailyfocus.data.repository.TaskRepository
import com.myday.dailyfocus.service.DailyResetWorker

class DailyFocusApplication : Application(), Configuration.Provider {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { TaskRepository(database.taskDao(), database.dailyStatsDao(), database.sessionDao()) }
    val userPrefsStore by lazy { UserPrefsStore(this) }
    val adManager by lazy { AdManager(this) }

    override fun onCreate() {
        super.onCreate()
        adManager.initialize()
        DailyResetWorker.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
