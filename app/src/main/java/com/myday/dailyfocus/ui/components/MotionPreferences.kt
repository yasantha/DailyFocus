package com.myday.dailyfocus.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * True when the system's "remove animations" accessibility setting is on
 * (Settings.Global.ANIMATOR_DURATION_SCALE == 0), meaning ambient decorative motion
 * (background blobs, Foko's idle bob, etc.) should be skipped in favor of a static frame.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }
}

/**
 * True while the hosting Activity is at least STARTED, so ambient animations can pause
 * their coroutines rather than tick uselessly while the app is backgrounded.
 */
@Composable
fun rememberIsAppForeground(): State<Boolean> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            state.value = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}
