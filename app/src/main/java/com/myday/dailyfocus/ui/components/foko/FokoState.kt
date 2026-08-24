package com.myday.dailyfocus.ui.components.foko

sealed class FokoState {
    data object Idle : FokoState()
    data object Focusing : FokoState()
    data object TaskComplete : FokoState()
    data object BreakTime : FokoState()
    data object Celebrating : FokoState()
    data object StreakAlert : FokoState()
}
