package com.myday.dailyfocus.ui.components.foko

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.myday.dailyfocus.R

enum class FokoMessageCategory {
    NO_TASKS,
    TIMER_STARTED,
    TIMER_RUNNING,
    TIMER_PAUSED,
    TASK_COMPLETED,
    BREAK_STARTED,
    ALL_TASKS_DONE,
    RETURNING_USER,
    STREAK_MILESTONE
}

/**
 * A single "thing Foko should say right now". [variantIndex] picks which line from the category
 * (or, for [FokoMessageCategory.STREAK_MILESTONE], the streak length itself). [id] is a
 * monotonically increasing trigger id so the same category firing twice in a row still counts
 * as a new cue for the speech bubble and TalkBack.
 */
data class FokoMessageCue(
    val category: FokoMessageCategory,
    val variantIndex: Int,
    val id: Long
)

@Composable
fun resolveFokoMessage(cue: FokoMessageCue): String {
    if (cue.category == FokoMessageCategory.STREAK_MILESTONE) {
        val res = when (cue.variantIndex) {
            3 -> R.string.foko_streak_3
            7 -> R.string.foko_streak_7
            14 -> R.string.foko_streak_14
            30 -> R.string.foko_streak_30
            else -> null
        }
        return res?.let { stringResource(it) } ?: ""
    }

    val arrayRes = when (cue.category) {
        FokoMessageCategory.NO_TASKS -> R.array.foko_no_tasks
        FokoMessageCategory.TIMER_STARTED -> R.array.foko_timer_started
        FokoMessageCategory.TIMER_RUNNING -> R.array.foko_timer_running
        FokoMessageCategory.TIMER_PAUSED -> R.array.foko_timer_paused
        FokoMessageCategory.TASK_COMPLETED -> R.array.foko_task_completed
        FokoMessageCategory.BREAK_STARTED -> R.array.foko_break_started
        FokoMessageCategory.ALL_TASKS_DONE -> R.array.foko_all_tasks_done
        FokoMessageCategory.RETURNING_USER -> R.array.foko_returning_user
        FokoMessageCategory.STREAK_MILESTONE -> return ""
    }
    val options = androidx.compose.ui.res.stringArrayResource(arrayRes)
    if (options.isEmpty()) return ""
    return options[cue.variantIndex.mod(options.size)]
}
