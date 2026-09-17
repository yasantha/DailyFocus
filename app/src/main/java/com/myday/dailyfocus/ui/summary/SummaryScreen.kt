package com.myday.dailyfocus.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.ui.components.BottomNavBar
import com.myday.dailyfocus.ui.components.BottomNavTab
import com.myday.dailyfocus.ui.components.countLabel
import com.myday.dailyfocus.ui.theme.Redesign

@Composable
fun SummaryScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val viewModel: SummaryViewModel = viewModel(
        factory = SummaryViewModelFactory(app.repository, app.userPrefsStore)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Redesign.PageBg2)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Progress", color = Redesign.Ink, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            }

            item {
                RangeSegmentedControl(current = state.range, onSelect = viewModel::setRange)
            }

            item {
                SummaryCard(state)
            }

            item {
                ConsistencyCard(state)
            }

            if (state.taskBreakdown.isNotEmpty()) {
                item {
                    Text(
                        "Where the time went",
                        color = Redesign.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                items(state.taskBreakdown) { entry ->
                    TaskBreakdownRow(entry)
                }
            }
        }

        BottomNavBar(current = BottomNavTab.PROGRESS) { tab ->
            when (tab) {
                BottomNavTab.TODAY -> navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
                BottomNavTab.PROGRESS -> {}
                // No settings route of its own yet -- back to Today, where the Settings tab
                // opens the settings sheet directly.
                BottomNavTab.SETTINGS -> navController.navigate("home") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
    }
}

@Composable
private fun RangeSegmentedControl(current: ProgressRange, onSelect: (ProgressRange) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Redesign.LavenderFill3) {
        Row(modifier = Modifier.padding(4.dp)) {
            ProgressRange.values().forEach { range ->
                val selected = range == current
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (selected) Redesign.White else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp),
                    onClick = { onSelect(range) }
                ) {
                    Text(
                        text = range.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Redesign.Ink else Redesign.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: ProgressUiState) {
    Surface(shape = RoundedCornerShape(20.dp), color = Redesign.White, border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border2)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatHm(state.totalSeconds), color = Redesign.Ink, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                if (state.deltaVsPreviousSeconds != 0) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(10.dp))
                    val positive = state.deltaVsPreviousSeconds > 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (positive) Redesign.SuccessBg else Redesign.LavenderFill3
                    ) {
                        Text(
                            text = "${if (positive) "+" else ""}${formatHm(state.deltaVsPreviousSeconds)} vs last period",
                            color = if (positive) Redesign.SuccessText else Redesign.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = "${countLabel(state.sessionsCount, "session")} · ${state.daysMetGoal} of ${countLabel(state.totalDaysInRange, "day")} met the goal",
                color = Redesign.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (state.bars.isNotEmpty()) {
                val maxMinutes = (state.bars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(state.goalMinutes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .padding(top = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.bars.forEach { bar ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            val heightFraction = if (maxMinutes == 0) 0f else (bar.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((100 * heightFraction).dp.coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(if (bar.metGoal) Redesign.Purple else Redesign.LavenderFill4)
                            )
                            Text(
                                text = bar.label,
                                fontSize = 12.sp,
                                fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (bar.isToday) Redesign.Purple else Redesign.TextMuted,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = "Goal is ${formatGoalLabel(state.goalMinutes)}/day",
                color = Redesign.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ConsistencyCard(state: ProgressUiState) {
    Surface(shape = RoundedCornerShape(20.dp), color = Redesign.White, border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border2)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Consistency", color = Redesign.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Longest ${countLabel(state.longestStreakInWindow, "day")}", color = Redesign.TextSecondary, fontSize = 14.sp)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                gridItems(state.consistency) { cell ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(consistencyColor(cell.minutes, state.goalMinutes))
                    )
                }
            }
            Text(
                text = "A day counts once you focus for 25 minutes.",
                color = Redesign.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

private fun consistencyColor(minutes: Int, goalMinutes: Int): androidx.compose.ui.graphics.Color {
    if (minutes <= 0) return Redesign.LavenderFill3
    val fraction = (minutes.toFloat() / goalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    return when {
        fraction >= 0.9f -> Redesign.Purple
        fraction >= 0.5f -> Redesign.LavenderFill4
        else -> Redesign.LavenderFill2
    }
}

@Composable
private fun TaskBreakdownRow(entry: TaskBreakdown) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entry.name, color = Redesign.Ink, fontSize = 15.sp)
            Text(formatHm(entry.seconds), color = Redesign.TextSecondary, fontSize = 15.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Redesign.LavenderFill3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(entry.shareOfTotal.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Redesign.Purple)
            )
        }
    }
}

private fun formatHm(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
}

private fun formatGoalLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

