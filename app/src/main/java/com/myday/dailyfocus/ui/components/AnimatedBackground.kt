package com.myday.dailyfocus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val FocusTopColor = Color(0xFFF5F3FF)
private val FocusBottomColor = Color(0xFFFAFAFE)
private val BreakTopColor = Color(0xFFE6FAF5)
private val BreakBottomColor = Color(0xFFFAFAFE)
private val BlobColor = Color(0xFF6C5CE7)

private data class BlobSpec(
    val originX: Float,
    val originY: Float,
    val radiusDp: Float,
    val driftRangeX: Float,
    val driftRangeY: Float,
    val phase: Float,
    val alpha: Float
)

private fun randomBlobs(count: Int): List<BlobSpec> {
    val random = Random(20260821)
    return List(count) {
        BlobSpec(
            originX = random.nextFloat(),
            originY = random.nextFloat(),
            radiusDp = 20f + random.nextFloat() * 60f,
            driftRangeX = 0.04f + random.nextFloat() * 0.06f,
            driftRangeY = 0.04f + random.nextFloat() * 0.06f,
            phase = random.nextFloat(),
            alpha = 0.04f + random.nextFloat() * 0.02f
        )
    }
}

/**
 * A very low-opacity, slow-drifting field of blobs behind the app content. Purely decorative:
 * never intercepts touch, never announced to accessibility services.
 */
@Composable
fun AnimatedBackground(
    isBreakMode: Boolean,
    isTimerRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isForeground by rememberIsAppForeground()
    val blobs = remember { randomBlobs(7) }

    val topColor by animateColorAsState(if (isBreakMode) BreakTopColor else FocusTopColor, label = "bgTop")
    val bottomColor by animateColorAsState(if (isBreakMode) BreakBottomColor else FocusBottomColor, label = "bgBottom")

    val animateAmbient = !reducedMotion && isForeground
    val driftDurationMs = if (isTimerRunning) 12_000 else 18_000

    val infiniteTransition = rememberInfiniteTransition(label = "backgroundBlobs")
    val driftTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateAmbient) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(driftDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )
    val pulseTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateAmbient) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(brush = Brush.verticalGradient(listOf(topColor, bottomColor)))

        if (reducedMotion) return@Canvas

        blobs.forEach { blob ->
            val driftX = sin((driftTime + blob.phase) * 2f * PI.toFloat()) * blob.driftRangeX * size.width
            val driftY = cos((driftTime + blob.phase * 1.3f) * 2f * PI.toFloat()) * blob.driftRangeY * size.height
            val pulseScale = 1f + sin((pulseTime + blob.phase) * 2f * PI.toFloat()) * 0.05f

            drawCircle(
                color = BlobColor.copy(alpha = blob.alpha),
                radius = blob.radiusDp.dp.toPx() * pulseScale,
                center = Offset(
                    x = blob.originX * size.width + driftX,
                    y = blob.originY * size.height + driftY
                )
            )
        }
    }
}
