package com.myday.dailyfocus.ui.daycomplete

import android.app.Activity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.R
import com.myday.dailyfocus.ui.components.TimerRing
import com.myday.dailyfocus.ui.components.foko.FokoCharacter
import com.myday.dailyfocus.ui.components.foko.FokoState
import com.myday.dailyfocus.ui.summary.SummaryViewModel
import com.myday.dailyfocus.ui.summary.SummaryViewModelFactory
import com.myday.dailyfocus.ui.theme.PastelBlue
import com.myday.dailyfocus.ui.theme.PastelYellow

@Composable
fun DayCompleteScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val viewModel: SummaryViewModel = viewModel(
        factory = SummaryViewModelFactory(app.repository, app.userPrefsStore)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null) {
            app.adManager.showInterstitial(activity)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiLayer(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 24.dp, vertical = 32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (state.showFoko) {
                FokoCharacter(state = FokoState.Celebrating, size = 72.dp)
            }

            val scale by rememberBounceScale()
            TimerRing(
                progress = 1f,
                timeLabel = "🏆",
                modeLabel = "",
                size = 180.dp,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )

            Text(
                text = stringResource(R.string.daycomplete_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.daycomplete_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.daycomplete_summary_line, state.focusSecondsToday / 60, state.sessionsToday),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    emoji = "✅",
                    label = stringResource(R.string.daycomplete_stat_tasks),
                    value = "${state.tasksDoneToday}",
                    background = PastelBlue
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    label = stringResource(R.string.daycomplete_stat_streak),
                    value = "${state.streak}",
                    background = PastelYellow
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { navController.navigate("summary") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.daycomplete_view_summary))
            }
            Button(
                onClick = {
                    navController.popBackStack("home", inclusive = false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.daycomplete_plan_tomorrow))
            }
        }
    }
}

@Composable
private fun rememberBounceScale(): androidx.compose.runtime.State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "trophyBounce")
    return infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyScale"
    )
}

@Composable
private fun StatChip(modifier: Modifier = Modifier, emoji: String, label: String, value: String, background: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class ConfettiPiece(val emoji: String, val xFraction: Float, val delayMillis: Int, val durationMillis: Int)

private val confettiPieces = listOf(
    ConfettiPiece("🎉", 0.08f, 0, 2600),
    ConfettiPiece("🎊", 0.22f, 200, 2200),
    ConfettiPiece("✨", 0.38f, 400, 2800),
    ConfettiPiece("🎉", 0.55f, 100, 2400),
    ConfettiPiece("🎊", 0.70f, 300, 2600),
    ConfettiPiece("✨", 0.85f, 150, 2300),
    ConfettiPiece("🎉", 0.95f, 350, 2500)
)

@Composable
private fun ConfettiLayer(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        confettiPieces.forEach { piece ->
            ConfettiParticle(piece, containerWidthPx = widthPx)
        }
    }
}

@Composable
private fun ConfettiParticle(piece: ConfettiPiece, containerWidthPx: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(piece.durationMillis, delayMillis = piece.delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiFall"
    )

    Text(
        text = piece.emoji,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .graphicsLayer {
                translationX = containerWidthPx * piece.xFraction
                translationY = progress * 1400f
                alpha = 1f - progress
                rotationZ = progress * 180f
            }
    )
}
