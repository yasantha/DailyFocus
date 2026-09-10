package com.myday.dailyfocus.ui.daycomplete

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.ui.theme.Redesign

@Composable
fun DayCompleteScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val viewModel: EndOfDayViewModel = viewModel(
        factory = EndOfDayViewModelFactory(app.repository, app.userPrefsStore)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null) {
            app.adManager.showInterstitial(activity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Redesign.Purple)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text(
                        text = state.dateLabel.uppercase(),
                        color = Redesign.LavenderFill2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "${formatHm(state.totalFocusSeconds)} focused across ${state.taskCount} ${if (state.taskCount == 1) "task" else "tasks"}",
                        color = Redesign.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = if (state.goalMet) "Goal met. Your streak is now ${state.streak} days." else "Keep going tomorrow -- streak is ${state.streak} days.",
                        color = Redesign.LavenderFill2,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            item {
                Surface(shape = RoundedCornerShape(22.dp), color = Redesign.White) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "THE DAY",
                            color = Redesign.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.6.sp
                        )
                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            state.entries.forEach { entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (entry.isDone) Redesign.Success else Color.Transparent)
                                            .border(2.dp, if (entry.isDone) Redesign.Success else Redesign.TextMuted, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = entry.name,
                                        color = if (entry.isDone) Redesign.Ink else Redesign.TextSecondary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = formatMinutesShort(entry.focusSeconds), color = Redesign.TextSecondary, fontSize = 15.sp)
                                }
                            }
                        }
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Redesign.Border2
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DayStat(modifier = Modifier.weight(1f), value = "${state.sessionsCount}", label = "Sessions")
                            DayStat(modifier = Modifier.weight(1f), value = formatMinutesShort(state.avgSessionSeconds), label = "Avg session")
                            DayStat(modifier = Modifier.weight(1f), value = state.bestWindowLabel, label = "Best window")
                        }
                    }
                }
            }

            if (state.incompleteTasks.isNotEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = Redesign.PurpleLight2) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "CARRY OVER TO TOMORROW",
                                color = Redesign.LavenderFill1,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                letterSpacing = 0.6.sp
                            )
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                state.incompleteTasks.forEach { entry ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.16f),
                                        onClick = { viewModel.carryOverToTomorrow(entry) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = entry.name, color = Redesign.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                            Text(text = "+", color = Redesign.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { navController.popBackStack("home", inclusive = false) },
                colors = ButtonDefaults.buttonColors(containerColor = Redesign.White, contentColor = Redesign.Purple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Plan tomorrow", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { navController.navigate("summary") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Redesign.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Redesign.LavenderFill4),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("See full progress", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DayStat(modifier: Modifier = Modifier, value: String, label: String) {
    Column(modifier = modifier) {
        Text(text = value, color = Redesign.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = label, color = Redesign.TextSecondary, fontSize = 13.sp)
    }
}

private fun formatHm(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
}

private fun formatMinutesShort(totalSeconds: Int): String {
    val m = totalSeconds / 60
    return "${m}m"
}
