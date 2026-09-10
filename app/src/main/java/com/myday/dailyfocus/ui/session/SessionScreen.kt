package com.myday.dailyfocus.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.ui.home.HomeViewModel
import com.myday.dailyfocus.ui.home.HomeViewModelFactory
import com.myday.dailyfocus.ui.home.TimerMode
import com.myday.dailyfocus.ui.home.TimerStatus
import com.myday.dailyfocus.ui.theme.Redesign

@Composable
fun SessionScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    // Must resolve the same HomeViewModel instance as HomeScreen (the timer this screen displays
    // was started there) -- scope to the Activity rather than this nav back-stack entry.
    val viewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = context as androidx.activity.ComponentActivity,
        factory = HomeViewModelFactory(app.repository, app.userPrefsStore, app)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timer = state.timer

    // If the timer goes back to idle (reset from elsewhere) or there's nothing to focus on,
    // this screen has nothing meaningful to show -- back out to Today.
    androidx.compose.runtime.LaunchedEffect(timer.status, timer.activeTaskId) {
        if (timer.status == TimerStatus.IDLE && timer.activeTaskId == null) {
            navController.popBackStack()
        }
    }

    // Overtime: a FOCUS session whose planned block has run out (remainingSeconds <= 0) but that
    // hasn't been wrapped up yet -- the ticker keeps counting negative instead of auto-stopping.
    val isOvertime = timer.mode == TimerMode.FOCUS && timer.remainingSeconds <= 0
    val overtimeSeconds = if (isOvertime) -timer.remainingSeconds else 0
    val displaySeconds = if (isOvertime) overtimeSeconds else timer.remainingSeconds
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    // Overtime has no natural "total" to measure against, so the ring just pulses through a
    // repeating 5-minute cycle -- a visual "still ticking," not a literal fraction of anything.
    val progress = when {
        isOvertime -> (overtimeSeconds % 300) / 300f
        timer.totalSeconds == 0 -> 0f
        else -> 1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds)
    }
    val elapsedSeconds = timer.totalSeconds - timer.remainingSeconds

    var showFinishConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Overtime already means "I chose to keep going past the plan" -- no need for a second
    // confirm step there, "Wrap up" / "Take a break" are the confirm.
    androidx.compose.runtime.LaunchedEffect(isOvertime) {
        if (isOvertime) showFinishConfirm = false
    }

    val focusingLabel = when {
        isOvertime -> "RUNNING OVER"
        timer.mode == TimerMode.FOCUS -> "FOCUSING ON"
        else -> "ON A BREAK"
    }
    val taskLabel: String = if (timer.mode == TimerMode.FOCUS) {
        if (timer.activeTaskName.isBlank()) "Focus session" else timer.activeTaskName
    } else {
        "Back to it after this"
    }
    val activeTaskSessionCount = timer.activeTaskId?.let { id -> state.taskFocusSummaries[id]?.sessionCount } ?: 0
    val sessionCaption = when {
        isOvertime -> "Planned ${timer.focusMinutes} min, still going"
        timer.mode == TimerMode.FOCUS -> "Session ${activeTaskSessionCount + 1} of ${state.estimatedSessionsForGoal} · ${timer.focusMinutes} min"
        else -> "${timer.idleDurationMinutes()} min break"
    }
    val ringColor = if (isOvertime) Redesign.AmberDot else Redesign.Purple
    val ringTrackColor = if (isOvertime) Redesign.AmberTrack else Redesign.LavenderFill4
    val timeTextColor = if (isOvertime) Redesign.AmberDot else Redesign.Ink

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Redesign.LavenderFill1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Redesign.White) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Text("‹", fontSize = 22.sp, color = Redesign.Ink)
                }
            }
            Surface(shape = RoundedCornerShape(999.dp), color = Redesign.White) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Redesign.Purple)
                    )
                    Text("Focus mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Redesign.Ink)
                }
            }
            Surface(shape = CircleShape, color = Redesign.White) {
                IconButton(onClick = { }) {
                    Text("⋮", fontSize = 18.sp, color = Redesign.Ink)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isOvertime) "$focusingLabel · ${taskLabel.uppercase()}" else focusingLabel,
                color = ringColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = if (isOvertime) "Session ${activeTaskSessionCount + 1} of ${state.estimatedSessionsForGoal}" else taskLabel,
                color = Redesign.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = sessionCaption,
                color = Redesign.TextSecondary2,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(272.dp)) {
                val strokeWidth = 20.dp.toPx()
                drawArc(
                    color = ringTrackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (if (isOvertime) "+" else "") + "%02d:%02d".format(minutes, seconds),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeTextColor
                )
                Text(
                    text = if (isOvertime) "over the planned time" else "left in this session",
                    fontSize = 15.sp,
                    color = Redesign.TextSecondary2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Finishing the actual work before the planned block runs out shouldn't force sitting
        // out the rest of the timer -- this logs the real elapsed time as a session (unlocking
        // the task's checkmark) instead of the full planned duration. Hidden until enough time
        // has genuinely passed to count as a real attempt, not an instant bail-out.
        val canFinishEarly = timer.mode == TimerMode.FOCUS && elapsedSeconds >= 60

        when {
            isOvertime -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Redesign.AmberDot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                viewModel.wrapUpOvertime()
                                navController.popBackStack()
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Wrap up — log ${formatElapsedLog(elapsedSeconds)}",
                                color = Redesign.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Redesign.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Redesign.Border3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                viewModel.wrapUpOvertimeAndTakeBreak()
                                navController.popBackStack()
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Take a break now",
                                color = Redesign.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            showFinishConfirm -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Redesign.Purple,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                viewModel.finishFocusEarly()
                                navController.popBackStack()
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Done — log ${formatElapsedLog(elapsedSeconds)}",
                                color = Redesign.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Redesign.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Redesign.Border3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                showFinishConfirm = false
                                viewModel.startTimer()
                            }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep going instead",
                                color = Redesign.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(22.dp), color = Redesign.White, modifier = Modifier.size(64.dp)) {
                        IconButton(onClick = { viewModel.resetTimer() }) {
                            Text("↺", fontSize = 20.sp, color = Redesign.Ink)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Redesign.Purple,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .clickable { if (timer.status == TimerStatus.RUNNING) viewModel.pauseTimer() else viewModel.startTimer() },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (timer.status == TimerStatus.RUNNING) {
                                Text("❚❚", color = Redesign.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Redesign.White)
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (timer.status == TimerStatus.RUNNING) "Pause" else "Resume",
                                color = Redesign.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        }
                    }
                    Surface(shape = RoundedCornerShape(22.dp), color = Redesign.White, modifier = Modifier.size(64.dp)) {
                        IconButton(onClick = {
                            if (timer.mode == TimerMode.FOCUS) viewModel.takeABreak() else viewModel.startNewFocusSession()
                        }) {
                            Text("⏭", fontSize = 18.sp, color = Redesign.Ink)
                        }
                    }
                }

                if (timer.mode == TimerMode.FOCUS) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Redesign.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Redesign.Border3),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { viewModel.extendFocusSession(5) }
                    ) {
                        Text(
                            text = "+5 min",
                            color = Redesign.Purple,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                Text(
                    text = if (timer.mode == TimerMode.FOCUS) "Break of ${timer.breakMinutes}m starts automatically" else "Focus resumes automatically",
                    color = Redesign.TextSecondary2,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                if (canFinishEarly) {
                    Text(
                        text = "Finished early? Log this session",
                        color = Redesign.Purple,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clickable {
                                viewModel.pauseTimer()
                                showFinishConfirm = true
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Redesign.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Focused today", color = Redesign.TextSecondary, fontSize = 15.sp)
                    Text(
                        text = "${formatHm(state.focusSecondsToday.toInt())} of ${formatHm(state.goalSeconds)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Redesign.Ink
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Redesign.LavenderFill2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.goalPercent.coerceIn(0, 100) / 100f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Redesign.Purple)
                    )
                }
                Text(
                    text = if (state.goalPercent >= 100) "You hit today's goal" else "Finish this session and you're closer to today's goal",
                    color = Redesign.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

private fun formatHm(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m}m"
}

private fun formatElapsedLog(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return if (s == 0) "${m}m" else "${m}m ${s}s"
}
