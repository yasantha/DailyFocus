package com.myday.dailyfocus.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myday.dailyfocus.R

enum class AchievementType(val emoji: String, @StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
    FIRST_FOCUS("🎯", R.string.achievement_first_focus_title, R.string.achievement_first_focus_desc),
    POWER_HOUR("⚡", R.string.achievement_power_hour_title, R.string.achievement_power_hour_desc),
    WEEK_WARRIOR("🏆", R.string.achievement_week_warrior_title, R.string.achievement_week_warrior_desc),
    TASKMASTER("✅", R.string.achievement_taskmaster_title, R.string.achievement_taskmaster_desc)
}

data class Achievement(
    val type: AchievementType,
    val unlocked: Boolean
)

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val backgroundColor = if (achievement.unlocked) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .alpha(if (achievement.unlocked) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = achievement.type.emoji, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(achievement.type.titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(achievement.type.descriptionRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
