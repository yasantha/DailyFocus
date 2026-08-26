package com.myday.dailyfocus.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.R
import com.myday.dailyfocus.ads.BannerAdView
import com.myday.dailyfocus.ui.components.AnimatedBackground
import com.myday.dailyfocus.ui.components.TaskCard
import com.myday.dailyfocus.ui.components.TimerRing
import com.myday.dailyfocus.ui.components.foko.FokoCharacter
import com.myday.dailyfocus.ui.components.foko.FokoMessageCue
import com.myday.dailyfocus.ui.components.foko.SpeechBubble
import com.myday.dailyfocus.ui.components.foko.resolveFokoMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(app.repository, app.userPrefsStore, app)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fokoMessageCue by viewModel.fokoMessageCue.collectAsStateWithLifecycle()

    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showAddMainSheet by rememberSaveable { mutableStateOf(false) }
    var showAddSecondarySheet by rememberSaveable { mutableStateOf(false) }
    var hasNavigatedToComplete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.allTasksDone) {
        if (state.allTasksDone && !hasNavigatedToComplete) {
            hasNavigatedToComplete = true
            navController.navigate("day_complete")
        }
        if (!state.allTasksDone) {
            hasNavigatedToComplete = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(
            isBreakMode = state.timer.mode == TimerMode.BREAK,
            isTimerRunning = state.timer.status == TimerStatus.RUNNING,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeHeader(
                    dateLabel = state.dateLabel,
                    streak = state.streak,
                    onStatsClick = { navController.navigate("summary") }
                )
            }

            item {
                ProgressSection(percent = state.completionPercent)
            }

            item {
                TimerSection(
                    state = state,
                    fokoMessageCue = fokoMessageCue,
                    onStart = viewModel::startTimer,
                    onPause = viewModel::pauseTimer,
                    onResume = viewModel::startTimer,
                    onReset = viewModel::resetTimer,
                    onSettingsClick = { showSettingsSheet = true },
                    onNewSession = viewModel::startNewFocusSession,
                    onTakeBreak = viewModel::takeABreak
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_main_focus),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
                    TaskCard(
                        task = main,
                        onToggle = { viewModel.toggleTask(main) },
                        onDelete = { viewModel.deleteTask(main) }
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
                        text = stringResource(R.string.home_secondary_tasks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(state.secondaryTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }

            if (state.secondaryTasks.size < 3) {
                item {
                    DashedActionButton(
                        label = stringResource(R.string.home_add_task_left, 3 - state.secondaryTasks.size),
                        onClick = { showAddSecondarySheet = true }
                    )
                }
            }

            item {
                BottomStatsRow(focusSeconds = state.focusSecondsToday, sessions = state.timer.sessionsToday)
            }

            item {
                BannerAdView(modifier = Modifier.fillMaxWidth())
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
            onDismiss = { showSettingsSheet = false },
            onFocusChange = viewModel::updateFocusDuration,
            onBreakChange = viewModel::updateBreakDuration,
            onLongBreakChange = viewModel::updateLongBreakDuration,
            onAutoStartChange = viewModel::setAutoStartNextSession,
            onShowFokoChange = viewModel::setShowFoko
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
private fun HomeHeader(dateLabel: String, streak: Int, onStatsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🍅", style = MaterialTheme.typography.titleLarge)
            }
            Column {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StreakBadge(streak = streak)
            OutlinedButton(onClick = onStatsClick, shape = RoundedCornerShape(20.dp)) {
                Text(stringResource(R.string.home_stats))
            }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "🔥")
        Text(text = "$streak", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ProgressSection(percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.home_today_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "$percent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TimerSection(
    state: HomeUiState,
    fokoMessageCue: FokoMessageCue?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSettingsClick: () -> Unit,
    onNewSession: () -> Unit,
    onTakeBreak: () -> Unit
) {
    val timer = state.timer
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60
    val timeLabel = "%02d:%02d".format(minutes, seconds)
    val modeLabel = when {
        timer.mode == TimerMode.FOCUS -> stringResource(R.string.timer_mode_focus)
        timer.isCurrentBreakLong -> stringResource(R.string.timer_mode_long_break)
        else -> stringResource(R.string.timer_mode_break)
    }
    val progress = if (timer.totalSeconds == 0) 0f else 1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())

    Box(modifier = Modifier.fillMaxWidth()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSettingsClick) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.timer_settings_title))
                }
            }

            TimerRing(progress = progress, timeLabel = timeLabel, modeLabel = modeLabel)

            if (timer.status == TimerStatus.FINISHED) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNewSession) { Text(stringResource(R.string.home_new_session)) }
                    Button(onClick = onTakeBreak) {
                        Text(
                            if (timer.nextBreakIsLong) stringResource(R.string.home_take_long_break)
                            else stringResource(R.string.home_take_break)
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onReset) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_reset))
                    }
                    when (timer.status) {
                        TimerStatus.RUNNING -> Button(onClick = onPause) {
                            Text("⏸")
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_pause))
                        }
                        TimerStatus.PAUSED -> Button(onClick = onResume) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_resume))
                        }
                        else -> Button(onClick = onStart) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_start))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_sessions_today, timer.sessionsToday),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.showFoko) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .padding(end = 12.dp)
        ) {
            if (fokoMessageCue != null) {
                val message = resolveFokoMessage(fokoMessageCue)
                SpeechBubble(
                    message = message,
                    cueId = fokoMessageCue.id,
                    maxWidth = 140.dp,
                    modifier = Modifier.padding(bottom = 44.dp, end = 4.dp)
                )
            }
            FokoCharacter(state = state.fokoState, size = 48.dp)
        }
    }
    }
}

@Composable
private fun DashedActionButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BottomStatsRow(focusSeconds: Long, sessions: Int) {
    val minutes = focusSeconds / 60
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoPill(
            modifier = Modifier.weight(1f),
            emoji = "⏳",
            label = stringResource(R.string.home_focus_time_label),
            value = stringResource(R.string.home_minutes_suffix, minutes)
        )
        InfoPill(
            modifier = Modifier.weight(1f),
            emoji = "✅",
            label = stringResource(R.string.home_completed_label),
            value = stringResource(R.string.home_sessions_suffix, sessions)
        )
    }
}

@Composable
private fun InfoPill(modifier: Modifier = Modifier, emoji: String, label: String, value: String) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji)
        Column {
            Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    onDismiss: () -> Unit,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onLongBreakChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onShowFokoChange: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val focusPresets = remember { listOf(15, 25, 45, 60) }
    val breakPresets = remember { listOf(3, 5, 10, 15) }
    val longBreakPresets = remember { listOf(15, 20, 30, 45) }

    // Pre-fill each custom field with the currently active duration whenever it isn't one of the
    // presets, so reopening Settings shows what's actually set instead of a blank box with no
    // preset highlighted either. Keyed on the prop so it re-syncs after a commit round-trips
    // through prefs, but won't clobber text the user is mid-way through typing.
    var customFocus by remember(focusMinutes) {
        mutableStateOf(if (focusMinutes in focusPresets) "" else focusMinutes.toString())
    }
    var customBreak by remember(breakMinutes) {
        mutableStateOf(if (breakMinutes in breakPresets) "" else breakMinutes.toString())
    }
    var customLongBreak by remember(longBreakMinutes) {
        mutableStateOf(if (longBreakMinutes in longBreakPresets) "" else longBreakMinutes.toString())
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
    fun commitAllPending() {
        if (customFocus.isNotBlank()) commitCustomFocus()
        if (customBreak.isNotBlank()) commitCustomBreak()
        if (customLongBreak.isNotBlank()) commitCustomLongBreak()
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
