package com.myday.dailyfocus.ui.components.foko

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.myday.dailyfocus.ui.components.rememberIsAppForeground
import com.myday.dailyfocus.ui.components.rememberReducedMotionEnabled

data class FokoTransform(
    val scale: Float = 1f,
    val rotationZ: Float = 0f,
    val translationYDp: Float = 0f
)

/** Body-level scale/rotation/bob driven by the current [FokoState]. */
@Composable
fun rememberFokoTransform(state: FokoState): FokoTransform {
    val reducedMotion = rememberReducedMotionEnabled()
    val isForeground by rememberIsAppForeground()
    val animate = !reducedMotion && isForeground

    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(state, animate) {
        if (state is FokoState.Idle && animate) {
            while (true) {
                translationY.animateTo(2f, tween(1500, easing = LinearEasing))
                translationY.animateTo(-2f, tween(1500, easing = LinearEasing))
            }
        } else {
            translationY.animateTo(0f, tween(300))
        }
    }

    LaunchedEffect(state, animate) {
        if (state is FokoState.Celebrating && animate) {
            while (true) {
                rotation.animateTo(5f, tween(300, easing = LinearEasing))
                rotation.animateTo(-5f, tween(300, easing = LinearEasing))
            }
        } else {
            rotation.animateTo(0f, tween(300))
        }
    }

    LaunchedEffect(state) {
        if (state is FokoState.TaskComplete) {
            scale.snapTo(1f)
            if (animate) {
                scale.animateTo(1.15f, tween(250, easing = LinearEasing))
                scale.animateTo(1f, tween(250, easing = LinearEasing))
            }
        }
    }

    LaunchedEffect(state) {
        if (state is FokoState.StreakAlert) {
            scale.snapTo(1f)
            if (animate) {
                repeat(2) {
                    scale.animateTo(1.1f, tween(250, easing = LinearEasing))
                    scale.animateTo(1f, tween(250, easing = LinearEasing))
                }
            }
        }
    }

    return FokoTransform(scale.value, rotation.value, translationY.value)
}

/** A single 0f..1f driver, looping every ~2.4s, used to animate accent decorations (energy
 * lines, zzz, stars, confetti, hearts) without spinning up a separate transition per accent. */
@Composable
fun rememberFokoAccentTime(): Float {
    val reducedMotion = rememberReducedMotionEnabled()
    val isForeground by rememberIsAppForeground()
    val animate = !reducedMotion && isForeground

    val infiniteTransition = rememberInfiniteTransition(label = "fokoAccent")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fokoAccentTime"
    )
    return time
}
