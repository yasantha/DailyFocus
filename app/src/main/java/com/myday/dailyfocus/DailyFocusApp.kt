package com.myday.dailyfocus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import com.myday.dailyfocus.ui.daycomplete.DayCompleteScreen
import com.myday.dailyfocus.ui.home.HomeScreen
import com.myday.dailyfocus.ui.onboarding.OnboardingScreen
import com.myday.dailyfocus.ui.session.SessionScreen
import com.myday.dailyfocus.ui.summary.SummaryScreen
import com.myday.dailyfocus.ui.theme.Redesign

@Composable
fun DailyFocusApp() {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val navController = rememberNavController()

    // The start destination must be decided once, up front -- reading it off a live Flow would
    // let a later emission flip startDestination after NavHost has already committed to one.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val prefs = app.userPrefsStore.userPrefs.first()
        startDestination = if (prefs.hasCompletedOnboarding) "home" else "onboarding"
    }

    val destination = startDestination
    if (destination == null) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(Redesign.PageBg1))
        return
    }

    NavHost(navController = navController, startDestination = destination) {
        composable("onboarding") { OnboardingScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("summary") { SummaryScreen(navController) }
        composable("day_complete") { DayCompleteScreen(navController) }
        composable("session") { SessionScreen(navController) }
    }
}
