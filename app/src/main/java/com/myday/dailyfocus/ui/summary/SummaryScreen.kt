package com.myday.dailyfocus.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.R
import com.myday.dailyfocus.data.model.Task
import com.myday.dailyfocus.ui.components.Achievement
import com.myday.dailyfocus.ui.components.AchievementCard
import com.myday.dailyfocus.ui.components.StatCard
import com.myday.dailyfocus.ui.components.TimerRing
import com.myday.dailyfocus.ui.components.WeekChart
import com.myday.dailyfocus.ui.components.foko.FokoCharacter
import com.myday.dailyfocus.ui.components.foko.FokoState
import com.myday.dailyfocus.ui.theme.PastelBlue
import com.myday.dailyfocus.ui.theme.PastelOrange
import com.myday.dailyfocus.ui.theme.PastelPink
import com.myday.dailyfocus.ui.theme.PastelYellow
import java.time.LocalDate

@Composable
fun SummaryScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val viewModel: SummaryViewModel = viewModel(
        factory = SummaryViewModelFactory(app.repository, app.userPrefsStore)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val motivationalQuotes = stringArrayResource(R.array.motivational_quotes)
    val quote = motivationalQuotes[LocalDate.now().dayOfYear % motivationalQuotes.size]

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    text = stringResource(R.string.summary_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.showFoko) {
                    FokoCharacter(
                        state = if (state.completionPercent >= 100) FokoState.Celebrating else FokoState.Idle,
                        size = 36.dp
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimerRing(
                    progress = state.completionPercent / 100f,
                    timeLabel = "${state.completionPercent}%",
                    modeLabel = stringResource(R.string.summary_completed_today),
                    size = 200.dp
                )
                Text(
                    text = "“$quote”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        item {
            val statItems = listOf(
                Triple("⏳", stringResource(R.string.summary_stat_focus_time), "${state.focusSecondsToday / 60}m") to PastelBlue,
                Triple("🍅", stringResource(R.string.summary_stat_sessions), "${state.sessionsToday}") to PastelOrange,
                Triple("✅", stringResource(R.string.summary_stat_tasks_done), "${state.tasksDoneToday}") to PastelPink,
                Triple("🔥", stringResource(R.string.summary_stat_day_streak), "${state.streak}") to PastelYellow
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                statItems.chunked(2).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { (info, color) ->
                            StatCard(
                                emoji = info.first,
                                label = info.second,
                                value = info.third,
                                backgroundColor = color,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (state.tasks.isNotEmpty()) {
            item {
                Text(text = stringResource(R.string.summary_task_breakdown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(state.tasks, key = { it.id }) { task ->
                TaskBreakdownRow(task)
            }
        }

        item {
            Text(text = stringResource(R.string.summary_this_week), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                WeekChart(days = state.weekDays, modifier = Modifier.padding(20.dp))
            }
        }

        item {
            Text(text = stringResource(R.string.summary_achievements), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.achievements.chunked(2).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { achievement: Achievement ->
                            AchievementCard(achievement = achievement, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskBreakdownRow(task: Task) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isMain) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(done = task.isDone)
            }
            LinearProgressIndicator(
                progress = { if (task.isDone) 1f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusBadge(done: Boolean) {
    val backgroundColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val label = if (done) stringResource(R.string.summary_status_done) else stringResource(R.string.summary_status_pending)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
