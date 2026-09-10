package com.myday.dailyfocus.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.ui.theme.Redesign
import kotlinx.coroutines.launch

private data class GoalPreset(val label: String, val minutes: Int, val recommended: Boolean = false)

private val presets = listOf(
    GoalPreset("1 hour", 60),
    GoalPreset("2 hours", 120, recommended = true),
    GoalPreset("4 hours", 240)
)

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as DailyFocusApplication
    val scope = rememberCoroutineScope()
    var selectedMinutes by remember { mutableIntStateOf(120) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Redesign.PageBg1)
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Text(
            text = "WELCOME TO DAILY FOCUS",
            color = Redesign.Purple,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.6.sp
        )
        Text(
            text = "How much focus time are you aiming for each day?",
            color = Redesign.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "This sets your daily goal ring and progress charts. You can change it anytime in Settings.",
            color = Redesign.TextSecondary2,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Column(
            modifier = Modifier.padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = selectedMinutes == preset.minutes
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) Redesign.LavenderFill1 else Redesign.White,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Redesign.Purple else Redesign.Border1
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMinutes = preset.minutes }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            text = preset.label,
                            color = Redesign.Ink,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        if (preset.recommended) {
                            Text(
                                text = "Recommended",
                                color = Redesign.Purple,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "You can edit this goal later. A day counts toward your streak once you've focused at least 25 minutes.",
            color = Redesign.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {}

        Button(
            onClick = {
                scope.launch {
                    app.userPrefsStore.setDailyGoalMinutes(selectedMinutes)
                    app.userPrefsStore.setHasCompletedOnboarding(true)
                }
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Redesign.Purple, contentColor = Redesign.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}
