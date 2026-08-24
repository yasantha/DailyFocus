package com.myday.dailyfocus.ui.components.foko

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myday.dailyfocus.R
import com.myday.dailyfocus.ui.theme.PurplePrimary
import kotlinx.coroutines.delay

private val BubbleBackground = Color(0xFFEDE9FF)

@Composable
fun SpeechBubble(
    message: String,
    cueId: Long,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = 200.dp
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(cueId, message) {
        if (message.isBlank()) {
            visible = false
            return@LaunchedEffect
        }
        visible = true
        delay(4000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(500)),
        modifier = modifier
    ) {
        val speechBubblePrefix = stringResource(R.string.foko_cd_speech_bubble)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .background(BubbleBackground, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "$speechBubblePrefix: $message"
                    }
            ) {
                Text(
                    text = message,
                    color = PurplePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Canvas(modifier = Modifier.size(width = 14.dp, height = 7.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, size.height)
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path, color = BubbleBackground)
            }
        }
    }
}
