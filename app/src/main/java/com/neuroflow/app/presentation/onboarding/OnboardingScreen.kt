package com.neuroflow.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

data class OnboardingData(
    val wakeUpHour: Int = 7,
    val sleepHour: Int = 23,
    val manualChronotype: String = "MODERATE_MORNING",
    val firstTask: String = "",
    val identityLabel: String = "Focused Professional",
    val topGoal: String = ""
)

@Composable
fun OnboardingScreen(
    onComplete: (OnboardingData) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf(OnboardingData()) }
    val totalSteps = 3

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                repeat(totalSteps) { index ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(if (index == currentStep) 12.dp else 8.dp)
                    ) {}
                }
            }

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> WakeSleepTimeStep(
                        wakeHour = data.wakeUpHour,
                        sleepHour = data.sleepHour,
                        onWakeHourChange = { data = data.copy(wakeUpHour = it) },
                        onSleepHourChange = { data = data.copy(sleepHour = it) }
                    )
                    1 -> ChronotypeDiscoveryStep(
                        selectedChronotype = data.manualChronotype,
                        onSelect = { data = data.copy(manualChronotype = it) }
                    )
                    2 -> FirstTaskStep(
                        value = data.firstTask,
                        onValueChange = { data = data.copy(firstTask = it) }
                    )
                }
            }

            // Navigation buttons (>= 48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Back", fontSize = 16.sp)
                    }
                } else {
                    TextButton(
                        onClick = { onComplete(data) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Skip", fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onComplete(data)
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .semantics { role = Role.Button },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (currentStep == totalSteps - 1) "Enter Flow" else "Next",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeSleepTimeStep(
    wakeHour: Int,
    sleepHour: Int,
    onWakeHourChange: (Int) -> Unit,
    onSleepHourChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Circadian Anchors ⏰",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "When do you typically wake and sleep? This calibrates your Borbély Two-Process energy and sleep pressure model.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Wake Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌅 Wake Up Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatHour(wakeHour),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = wakeHour.toFloat(),
                    onValueChange = { onWakeHourChange(it.toInt()) },
                    valueRange = 4f..11f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sleep Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌙 Sleep Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatHour(sleepHour),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sleepHour.toFloat(),
                    onValueChange = { onSleepHourChange(it.toInt()) },
                    valueRange = 20f..26f, // 20:00 (8 PM) to 26:00 (2 AM)
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ChronotypeDiscoveryStep(
    selectedChronotype: String,
    onSelect: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Chronotype Discovery 🧬",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select when your cognitive focus naturally peaks. We'll protect high-demand tasks during this biological window.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        listOf(
            Triple("🌅 Morning Lark", "MODERATE_MORNING", "Peak focus: 9 AM – 12 PM · Optimal for analytical sprints"),
            Triple("☀️ Intermediate Bear", "INTERMEDIATE", "Peak focus: 12 PM – 5 PM · Steady afternoon cognitive stamina"),
            Triple("🌙 Night Owl", "MODERATE_EVENING", "Peak focus: 5 PM – 10 PM · Peak creative flow in late evening")
        ).forEach { (label, value, subtitle) ->
            val isSelected = selectedChronotype == value
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                onClick = { onSelect(value) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstTaskStep(value: String, onValueChange: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            "Immediate Next Action 🐸",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Eliminate startup friction right now. What is the single highest-leverage task or frog you want to conquer first?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("First Action / Frog") },
            placeholder = { Text("e.g. Finish quarterly project proposal 45m #work") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "💡 Pro tip: Add duration (e.g. 45m) and tags (@work) — our smart engine will auto-schedule it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatHour(hour: Int): String {
    val h = if (hour >= 24) hour - 24 else hour
    return when {
        h == 0 -> "12:00 AM"
        h < 12 -> "$h:00 AM"
        h == 12 -> "12:00 PM"
        else -> "${h - 12}:00 PM"
    }
}
