package com.myday.dailyfocus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myday.dailyfocus.ui.theme.Redesign

enum class BottomNavTab(val route: String) {
    TODAY("home"),
    PROGRESS("summary"),
    SETTINGS("app_settings")
}

@Composable
fun BottomNavBar(current: BottomNavTab, onSelect: (BottomNavTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Redesign.White)
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        NavItem(
            label = "Today",
            selected = current == BottomNavTab.TODAY,
            onClick = { onSelect(BottomNavTab.TODAY) },
            modifier = Modifier.weight(1f)
        ) { tint -> TodayGlyph(tint) }
        NavItem(
            label = "Progress",
            selected = current == BottomNavTab.PROGRESS,
            onClick = { onSelect(BottomNavTab.PROGRESS) },
            modifier = Modifier.weight(1f)
        ) { tint -> ProgressGlyph(tint) }
        NavItem(
            label = "Settings",
            selected = current == BottomNavTab.SETTINGS,
            onClick = { onSelect(BottomNavTab.SETTINGS) },
            modifier = Modifier.weight(1f)
        ) { tint -> SettingsGlyph(tint) }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: @Composable (Color) -> Unit
) {
    val tint = if (selected) Redesign.Purple else Redesign.TextMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        glyph(tint)
        Text(
            text = label,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun TodayGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawRoundRect(
            color = tint,
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
    }
}

@Composable
private fun ProgressGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val barWidth = size.width / 5f
        val heights = listOf(0.4f, 0.75f, 0.55f)
        heights.forEachIndexed { index, h ->
            val barHeight = size.height * h
            drawRoundRect(
                color = tint,
                topLeft = Offset(index * barWidth * 1.6f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun SettingsGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawCircle(color = tint, radius = size.minDimension / 2.4f, style = Stroke(width = 2.dp.toPx()))
    }
}
