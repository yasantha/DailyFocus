package com.myday.dailyfocus.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class WeekDayFocus(
    val label: String,
    val minutes: Int,
    val isToday: Boolean
)

@Composable
fun WeekChart(
    days: List<WeekDayFocus>,
    modifier: Modifier = Modifier,
    maxBarHeight: androidx.compose.ui.unit.Dp = 120.dp
) {
    val maxMinutes = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (day.minutes > 0) "${day.minutes}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .height(maxBarHeight)
                        .width(28.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val targetFraction = day.minutes.toFloat() / maxMinutes.toFloat()
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction.coerceIn(0f, 1f),
                        animationSpec = tween(600),
                        label = "barHeight"
                    )
                    val barColor = if (day.isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = animatedFraction.coerceAtLeast(0.04f))
                            .background(barColor, RoundedCornerShape(8.dp))
                    )
                }
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
