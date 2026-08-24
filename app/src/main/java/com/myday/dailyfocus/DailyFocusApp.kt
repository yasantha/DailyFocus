package com.myday.dailyfocus

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myday.dailyfocus.ui.daycomplete.DayCompleteScreen
import com.myday.dailyfocus.ui.home.HomeScreen
import com.myday.dailyfocus.ui.summary.SummaryScreen

@Composable
fun DailyFocusApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("summary") { SummaryScreen(navController) }
        composable("day_complete") { DayCompleteScreen(navController) }
    }
}
