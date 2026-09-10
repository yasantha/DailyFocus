package com.myday.dailyfocus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.R
import com.myday.dailyfocus.ads.BannerAdView
import com.myday.dailyfocus.data.model.Task
import com.myday.dailyfocus.ui.components.BottomNavBar
import com.myday.dailyfocus.ui.components.BottomNavTab
import com.myday.dailyfocus.ui.theme.Redesign
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    // Scoped to the Activity, not this nav back-stack entry, so the Session screen (which reads
    // and controls the same running timer) resolves the identical HomeViewModel instance instead
    // of getting its own with a stale default TimerUiState.
    val viewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = context as androidx.activity.ComponentActivity,
        factory = HomeViewModelFactory(app.repository, app.userPrefsStore, app)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showAddMainSheet by rememberSaveable { mutableStateOf(false) }
    var showAddSecondarySheet by rememberSaveable { mutableStateOf(false) }
    var hasNavigatedToComplete by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Deleting is immediate (no confirm dialog) with a short undo window instead -- re-adding on
    // undo creates a new row since Task ids auto-generate, which is fine since nothing else in
    // the UI depends on the deleted row's original id surviving. Secondary rows only: the main
    // focus task has no equivalent "add back" path that doesn't risk clobbering a different task.
    fun deleteSecondaryWithUndo(task: Task) {
        viewModel.deleteTask(task)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${task.text}\"",
                actionLabel = "Undo",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.addSecondaryTask(task.text)
            }
        }
    }

    LaunchedEffect(state.allTasksDone) {
        if (state.allTasksDone && !hasNavigatedToComplete) {
            hasNavigatedToComplete = true
            navController.navigate("day_complete")
        }
        if (!state.allTasksDone) {
            hasNavigatedToComplete = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Redesign.PageBg2,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Redesign.Ink,
                    contentColor = Redesign.White,
                    actionColor = Redesign.PurpleLight2
                )
            }
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
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
                TodayHeader(dateLabel = state.dateLabel, streak = state.streak)
            }

            item {
                FocusSummaryCard(state = state)
            }

            item {
                Text(
                    text = stringResource(R.string.home_main_focus).uppercase(),
                    color = Redesign.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp
                )
            }

            item {
                val main = state.mainTask
                if (main == null) {
                    DashedActionButton(
                        label = stringResource(R.string.home_set_main_focus),
                        onClick = { showAddMainSheet = true }
                    )
                } else {
                    MainFocusCard(
                        task = main,
                        summary = state.taskFocusSummaries[main.id],
                        isActive = state.timer.activeTaskId == main.id,
                        onStartFocus = {
                            viewModel.startFocusOn(main)
                            navController.navigate("session")
                        },
                        onToggleDone = { viewModel.toggleTask(main) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_secondary_tasks).uppercase(),
                        color = Redesign.TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.6.sp
                    )
                    val doneCount = state.secondaryTasks.count { it.isDone }
                    Text(
                        text = "$doneCount of ${state.secondaryTasks.size} done",
                        color = Redesign.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            items(state.secondaryTasks, key = { it.id }) { task ->
                SecondaryTaskRow(
                    task = task,
                    summary = state.taskFocusSummaries[task.id],
                    onToggle = { viewModel.toggleTask(task) },
                    onStartFocus = {
                        viewModel.startFocusOn(task)
                        navController.navigate("session")
                    },
                    onDelete = { deleteSecondaryWithUndo(task) }
                )
            }

            item {
                AddTaskRow(
                    slotsUsed = state.secondaryTasks.size,
                    slotsTotal = 3,
                    enabled = state.secondaryTasks.size < 3,
                    onClick = { showAddSecondarySheet = true }
                )
            }

            item {
                BannerAdView(modifier = Modifier.fillMaxWidth())
            }
        }

        BottomNavBar(current = BottomNavTab.TODAY) { tab ->
            when (tab) {
                BottomNavTab.TODAY -> {}
                BottomNavTab.PROGRESS -> navController.navigate("summary")
                BottomNavTab.SETTINGS -> showSettingsSheet = true
            }
        }
    }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            focusMinutes = state.timer.focusMinutes,
            breakMinutes = state.timer.breakMinutes,
            longBreakMinutes = state.timer.longBreakMinutes,
            autoStartNextSession = state.autoStartNextSession,
            showFoko = state.showFoko,
            dailyGoalMinutes = state.dailyGoalMinutes,
            onDismiss = { showSettingsSheet = false },
            onFocusChange = viewModel::updateFocusDuration,
            onBreakChange = viewModel::updateBreakDuration,
            onLongBreakChange = viewModel::updateLongBreakDuration,
            onAutoStartChange = viewModel::setAutoStartNextSession,
            onShowFokoChange = viewModel::setShowFoko,
            onDailyGoalChange = viewModel::setDailyGoalMinutes
        )
    }

    if (showAddMainSheet) {
        AddTaskBottomSheet(
            title = stringResource(R.string.home_set_main_focus),
            onDismiss = { showAddMainSheet = false },
            onSubmit = {
                viewModel.setMainTask(it)
                showAddMainSheet = false
            }
        )
    }

    if (showAddSecondarySheet) {
        AddTaskBottomSheet(
            title = stringResource(R.string.home_add_secondary_task_title),
            onDismiss = { showAddSecondarySheet = false },
            onSubmit = {
                viewModel.addSecondaryTask(it)
                showAddSecondarySheet = false
            }
        )
    }
}

@Composable
private fun TodayHeader(dateLabel: String, streak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = dateLabel.uppercase(),
                color = Redesign.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = stringResource(R.string.app_name).let { "Today" },
                color = Redesign.Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (streak > 0) {
            Surface(shape = RoundedCornerShape(999.dp), color = Redesign.AmberBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Redesign.AmberDot)
                    )
                    Text(
                        text = "$streak-day streak",
                        color = Redesign.AmberText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusSummaryCard(state: HomeUiState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Redesign.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border2)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.home_today_progress), color = Redesign.TextSecondary, fontSize = 15.sp)
                Text("Goal ${formatGoalLabel(state.dailyGoalMinutes)}", color = Redesign.TextSecondary, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = formatHm(state.focusSecondsToday.toInt()),
                    color = Redesign.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${state.goalPercent}%",
                    color = Redesign.Purple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            val segments = state.estimatedSessionsForGoal.coerceIn(1, 8)
            val filledFraction = (state.goalPercent / 100f).coerceIn(0f, 1f) * segments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until segments) {
                    val fill = (filledFraction - i).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Redesign.LavenderFill2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fill)
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Redesign.Purple)
                        )
                    }
                }
            }
            val minutesToGo = state.goalRemainingSeconds / 60
            Text(
                text = "${state.timer.sessionsToday} of $segments sessions" + if (minutesToGo > 0) " · ${minutesToGo}m to go" else " · Goal met",
                color = Redesign.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun MainFocusCard(
    task: Task,
    summary: TaskFocusSummary?,
    isActive: Boolean,
    onStartFocus: () -> Unit,
    onToggleDone: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Redesign.White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Redesign.LavenderFill4)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (task.isDone) Redesign.Success else androidx.compose.ui.graphics.Color.Transparent)
                        .border(2.dp, if (task.isDone) Redesign.Success else Redesign.TextMuted, CircleShape)
                        .then(Modifier)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.text,
                        color = Redesign.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    val focusedMin = (summary?.focusSeconds ?: 0) / 60
                    Text(
                        text = "${focusedMin}m focused · ${summary?.sessionCount ?: 0} sessions",
                        color = Redesign.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartFocus,
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Redesign.Purple),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Redesign.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isActive) "Continue focus" else "Start focus", color = Redesign.White, fontWeight = FontWeight.SemiBold)
                }
                // Completing a task with zero focus time on it would let the streak/goal
                // numbers be gamed by ticking boxes instead of doing the work -- so the
                // checkmark stays disabled until at least one session has actually run.
                // Un-marking a done task is always allowed.
                val canMarkDone = task.isDone || (summary?.sessionCount ?: 0) > 0
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border1),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onToggleDone, enabled = canMarkDone) {
                        Text(
                            if (task.isDone) "↺" else "✓",
                            color = if (canMarkDone) Redesign.TextSecondary else Redesign.Border1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryTaskRow(
    task: Task,
    summary: TaskFocusSummary?,
    onToggle: () -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Redesign.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Same rule as the main-focus card: no marking done with zero logged focus time.
            val canMarkDone = task.isDone || (summary?.sessionCount ?: 0) > 0
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (task.isDone) Redesign.Success else androidx.compose.ui.graphics.Color.Transparent)
                    .border(2.dp, if (task.isDone) Redesign.Success else if (canMarkDone) Redesign.TextMuted else Redesign.Border2, CircleShape)
                    .then(
                        if (canMarkDone) {
                            Modifier.clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = onToggle
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (task.isDone) {
                    Text("✓", color = Redesign.White, fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp, top = 1.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = task.text,
                color = if (task.isDone) Redesign.TextMuted else Redesign.Ink,
                fontSize = 16.sp,
                textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )
            if (task.isDone) {
                val minutes = (summary?.focusSeconds ?: 0) / 60
                Text(text = "${minutes}m", color = Redesign.TextMuted, fontSize = 14.sp)
            } else {
                Text(
                    text = "Start",
                    color = Redesign.Purple,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onStartFocus
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "✕",
                color = Redesign.TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDelete
                )
            )
        }

    }
}

@Composable
private fun AddTaskRow(slotsUsed: Int, slotsTotal: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Redesign.LavenderFill4),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Redesign.Purple)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_add_task_button),
                color = Redesign.Purple,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(text = "$slotsUsed of $slotsTotal slots used", color = Redesign.TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DashedActionButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                androidx.compose.foundation.BorderStroke(2.dp, Redesign.LavenderFill4),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Redesign.Purple)
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = Redesign.Purple, fontWeight = FontWeight.Medium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    focusMinutes: Int,
    breakMinutes: Int,
    longBreakMinutes: Int,
    autoStartNextSession: Boolean,
    showFoko: Boolean,
    dailyGoalMinutes: Int,
    onDismiss: () -> Unit,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onLongBreakChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onShowFokoChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val focusPresets = remember { listOf(15, 25, 45, 60) }
    val breakPresets = remember { listOf(3, 5, 10, 15) }
    val longBreakPresets = remember { listOf(15, 20, 30, 45) }
    val goalPresets = remember { listOf(60, 90, 120, 180) }

    var customFocus by remember(focusMinutes) {
        mutableStateOf(if (focusMinutes in focusPresets) "" else focusMinutes.toString())
    }
    var customBreak by remember(breakMinutes) {
        mutableStateOf(if (breakMinutes in breakPresets) "" else breakMinutes.toString())
    }
    var customLongBreak by remember(longBreakMinutes) {
        mutableStateOf(if (longBreakMinutes in longBreakPresets) "" else longBreakMinutes.toString())
    }
    var customGoal by remember(dailyGoalMinutes) {
        mutableStateOf(if (dailyGoalMinutes in goalPresets) "" else dailyGoalMinutes.toString())
    }

    fun commitCustomFocus() {
        val minutes = customFocus.toIntOrNull()?.coerceIn(1, 120) ?: return
        onFocusChange(minutes)
        customFocus = minutes.toString()
    }
    fun commitCustomBreak() {
        val minutes = customBreak.toIntOrNull()?.coerceIn(1, 30) ?: return
        onBreakChange(minutes)
        customBreak = minutes.toString()
    }
    fun commitCustomLongBreak() {
        val minutes = customLongBreak.toIntOrNull()?.coerceIn(1, 60) ?: return
        onLongBreakChange(minutes)
        customLongBreak = minutes.toString()
    }
    fun commitCustomGoal() {
        val minutes = customGoal.toIntOrNull()?.coerceIn(15, 960) ?: return
        onDailyGoalChange(minutes)
        customGoal = minutes.toString()
    }
    fun commitAllPending() {
        if (customFocus.isNotBlank()) commitCustomFocus()
        if (customBreak.isNotBlank()) commitCustomBreak()
        if (customLongBreak.isNotBlank()) commitCustomLongBreak()
        if (customGoal.isNotBlank()) commitCustomGoal()
    }

    ModalBottomSheet(
        onDismissRequest = {
            commitAllPending()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.timer_settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Text(text = "Daily focus goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DurationPresetRow(presets = goalPresets, selected = dailyGoalMinutes, onSelect = { customGoal = ""; onDailyGoalChange(it) })
            OutlinedTextField(
                value = customGoal,
                onValueChange = { customGoal = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("Custom minutes (15-960)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomGoal() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customGoal.isNotBlank()) {
                        IconButton(onClick = { commitCustomGoal() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_apply))
                        }
                    }
                }
            )

            Text(text = stringResource(R.string.timer_settings_focus_duration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DurationPresetRow(
                presets = focusPresets,
                selected = focusMinutes,
                onSelect = { customFocus = ""; onFocusChange(it) }
            )
            OutlinedTextField(
                value = customFocus,
                onValueChange = { customFocus = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text(stringResource(R.string.timer_settings_custom_focus)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customFocus.isNotBlank()) {
                        IconButton(onClick = { commitCustomFocus() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_apply))
                        }
                    }
                }
            )

            Text(text = stringResource(R.string.timer_settings_break_duration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DurationPresetRow(
                presets = breakPresets,
                selected = breakMinutes,
                onSelect = { customBreak = ""; onBreakChange(it) }
            )
            OutlinedTextField(
                value = customBreak,
                onValueChange = { customBreak = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.timer_settings_custom_break)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomBreak() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customBreak.isNotBlank()) {
                        IconButton(onClick = { commitCustomBreak() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_apply))
                        }
                    }
                }
            )

            Text(text = stringResource(R.string.timer_settings_long_break_duration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.timer_settings_long_break_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DurationPresetRow(
                presets = longBreakPresets,
                selected = longBreakMinutes,
                onSelect = { customLongBreak = ""; onLongBreakChange(it) }
            )
            OutlinedTextField(
                value = customLongBreak,
                onValueChange = { customLongBreak = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text(stringResource(R.string.timer_settings_custom_long_break)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitCustomLongBreak() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (customLongBreak.isNotBlank()) {
                        IconButton(onClick = { commitCustomLongBreak() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_apply))
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timer_settings_auto_start_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.timer_settings_auto_start_desc),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoStartNextSession, onCheckedChange = onAutoStartChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.foko_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.foko_settings_desc),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = showFoko, onCheckedChange = onShowFokoChange)
            }

            LanguageSelector()

            Button(
                onClick = {
                    commitAllPending()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.timer_settings_done))
            }
        }
    }
}

private data class LanguageOption(val tag: String?, val nativeName: String)

@Composable
private fun LanguageSelector() {
    val systemDefaultLabel = stringResource(R.string.settings_language_system_default)
    val options = remember(systemDefaultLabel) {
        listOf(
            LanguageOption(null, systemDefaultLabel),
            LanguageOption("en", "English"),
            LanguageOption("de", "Deutsch"),
            LanguageOption("es", "Español"),
            LanguageOption("fr", "Français")
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var selectedTag by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.language)
    }
    val selectedLabel = options.firstOrNull { it.tag == selectedTag }?.nativeName ?: systemDefaultLabel

    Column {
        Text(
            text = stringResource(R.string.settings_language_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.nativeName) },
                        onClick = {
                            expanded = false
                            selectedTag = option.tag
                            val locales = if (option.tag == null) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(option.tag)
                            }
                            AppCompatDelegate.setApplicationLocales(locales)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationPresetRow(presets: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.forEach { minutes ->
            FilterChip(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                label = { Text(stringResource(R.string.home_minutes_suffix, minutes)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.home_task_field_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (text.isNotBlank()) onSubmit(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.home_add_task_button))
            }
        }
    }
}
