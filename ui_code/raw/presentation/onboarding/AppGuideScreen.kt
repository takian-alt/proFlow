package com.neuroflow.app.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.common.theme.NeuroFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGuideScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("neuroFlow User Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeuroFlowColors.Purple.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚡", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Master your workflow with science-backed prioritization, auto-scheduling, and deep focus.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 1. Quick Start
            GuideSection(
                emoji = "🚀",
                title = "Start in 3 Minutes",
                color = NeuroFlowColors.Purple.copy(alpha = 0.08f),
                textColor = NeuroFlowColors.Purple,
                summary = "Quick setup to get productive immediately.",
                steps = listOf(
                    "Add 3-5 tasks only. Keep titles specific and actionable.",
                    "Set a deadline on at least one task that truly matters today.",
                    "Mark one hard-but-crucial task as 🐸 Frog.",
                    "Open Focus Mode and run one 25-minute sprint.",
                    "Ignore everything else until that sprint is completed."
                )
            )

            // 2. Daily Loop
            GuideSection(
                emoji = "🔁",
                title = "The Daily Loop",
                color = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                summary = "A repeatable 5-step daily routine.",
                steps = listOf(
                    "Morning: Check the Matrix and pick the top scored task in DO FIRST.",
                    "Timeline: Review your auto-scheduled day or lock in key focus blocks.",
                    "Sprint: Run Focus Mode for 25-45 minutes without switching apps.",
                    "Post-Sprint: Mark done, log time, or split remaining work into the next session.",
                    "Evening: Review completed wins and queue tomorrow's 🐸 Frog."
                )
            )

            // 3. Auto-Scheduling
            GuideSection(
                emoji = "🤖",
                title = "Auto-Scheduling & Smart Timeline",
                color = NeuroFlowColors.ScheduledCard,
                textColor = Color(0xFF1565C0),
                summary = "How the intelligent scheduler places tasks automatically.",
                steps = listOf(
                    "Automatic Placement: When enabled, active tasks with estimated durations are automatically scheduled into your calendar timeline.",
                    "Circadian & Energy Alignment: Analytical and high-effort tasks are placed during your peak energy hours; routine tasks during afternoon dips.",
                    "Automatic Rest Breaks: A 15-minute restorative break is automatically inserted after every 90 minutes of continuous cognitive load.",
                    "Protected Slots: Sleep hours and custom protected blocks (lunch, gym, winding down) are strictly respected.",
                    "🔒 Schedule Locking: Tap 'Lock Schedule' on any task to pin it to an exact time slot. Auto-scheduling will never displace locked tasks."
                )
            )

            // 4. Dependencies
            GuideSection(
                emoji = "🔗",
                title = "Task Dependencies & Unblocking",
                color = NeuroFlowColors.ScheduleBg,
                textColor = NeuroFlowColors.ScheduleText,
                summary = "Chaining tasks so work happens in the right order.",
                steps = listOf(
                    "Prerequisites: In the task editor, select 'Depends On' to link predecessor tasks.",
                    "Blocked State: Blocked tasks score 0 and won't crowd your DO FIRST quadrant until their prerequisites are finished.",
                    "Critical Path Multiplier: Tasks that unblock multiple other tasks receive an exponential priority boost (Critical Path Method).",
                    "Auto-Unblocking: When you complete a prerequisite task, all dependent tasks are instantly unblocked and scheduled."
                )
            )

            // 5. Multi-Session Task Splitting
            GuideSection(
                emoji = "✂️",
                title = "Multi-Session Task Splitting",
                color = NeuroFlowColors.DoFirstBg,
                textColor = NeuroFlowColors.DoFirstText,
                summary = "Breaking huge 2hr+ projects into manageable 45-minute sprints.",
                steps = listOf(
                    "Overcome Procrastination: Large tasks often trigger avoidance. Splitting them transforms overwhelming projects into doable chunks.",
                    "Chained Chunks: Splitting a 2-hour task creates sequential parts (e.g. Part 1/3, Part 2/3, Part 3/3), chained by dependencies.",
                    "Seamless Scheduling: Part 1 schedules first. As soon as you complete Part 1, Part 2 automatically unlocks and schedules next.",
                    "Progress Momentum: Each completed session earns XP and increases momentum scoring for subsequent sessions."
                )
            )

            // 6. Eisenhower Matrix
            GuideSection(
                emoji = "🗂",
                title = "Eisenhower Matrix Explained",
                color = NeuroFlowColors.Purple.copy(alpha = 0.08f),
                textColor = NeuroFlowColors.PurpleDark,
                summary = "The classic 4-quadrant decision framework.",
                steps = listOf(
                    "DO FIRST (Urgent & Important): High-impact tasks with pressing deadlines. Handle these first today.",
                    "SCHEDULE (Important, Not Urgent): Long-term value, strategy, and skill building. Guard time on your calendar for these.",
                    "DELEGATE (Urgent, Low Impact): Admin, quick emails, errands. Batch or handle quickly.",
                    "ON HOLD / ELIMINATE: Parked ideas or low-value items. Kept out of sight until needed.",
                    "The ✦ badge highlights the highest-scored task across your entire matrix."
                )
            )

            // 7. Priority Scoring & Custom Weights
            GuideSection(
                emoji = "⚡",
                title = "Priority Scoring & Strategy Tuning",
                color = NeuroFlowColors.DeadlineCard,
                textColor = NeuroFlowColors.DoFirstText,
                summary = "How the 20+ signal scoring engine ranks your tasks.",
                steps = listOf(
                    "Continuous Mathematical Score: Recomputed dynamically based on deadlines, chronotype, time-of-day, habits, and momentum.",
                    "Why This Score?: In Focus Mode, tap the score breakdown to see the exact point contributions for every heuristic.",
                    "Custom Scoring Weights: Go to Settings → Priority Weights & Strategy to adjust sliders or pick a 1-click strategy preset.",
                    "Presets: Switch between 'Balanced', 'Deadline Driven', 'Momentum / ADHD Friendly', or 'Pure Eisenhower' depending on your current mode."
                )
            )

            // 8. Focus Mode & Time Tracking
            GuideSection(
                emoji = "🎯",
                title = "Focus Mode & Deep Work",
                color = NeuroFlowColors.ScheduleBg,
                textColor = NeuroFlowColors.ScheduleText,
                summary = "Stay in the flow and protect your attention.",
                steps = listOf(
                    "Live Timer: Track exact time spent per task. Pauses and resumes cleanly across app restarts.",
                    "🍅 Pomodoro: Built-in 25-minute sprints with customizable break intervals.",
                    "WOOP Method: Clarify your Wish, Outcome, Obstacle, and Plan before starting to double follow-through rates.",
                    "Interruption Shield: Gentle 3-second breathing interstitial when attempting to leave an active focus sprint.",
                    "Manual Logging: If you did work away from your phone, enter manual minutes on task completion to maintain accurate XP and estimation stats."
                )
            )

            // 9. Time Blocking & Protected Slots
            GuideSection(
                emoji = "📅",
                title = "Time Blocking & Protected Time",
                color = NeuroFlowColors.ScheduledCard,
                textColor = Color(0xFF1565C0),
                summary = "Guard your calendar from overload.",
                steps = listOf(
                    "Timeline Drag & Tap: Tap any open hour slot in the Calendar view to assign a task.",
                    "Protected Time Slots: Configure recurring gym, meal, or recovery blocks in Settings. The scheduler will route tasks around them.",
                    "Chronotype Sync: Morning larks get peak morning slots; night owls get late afternoon/evening focus windows.",
                    "Day Transitions: Easily navigate between days using the arrow buttons at the top of the Timeline."
                )
            )

            // 10. Analytics & Insights
            GuideSection(
                emoji = "📊",
                title = "Analytics & Performance Insights",
                color = NeuroFlowColors.DeadlineCard,
                textColor = NeuroFlowColors.DoFirstText,
                summary = "Understand your working habits and estimation accuracy.",
                steps = listOf(
                    "XP & Impact Points: Earn points scaled by task impact and duration to gamify productivity.",
                    "Estimation Accuracy (MAPE): Compares estimated vs. actual tracked minutes so you calibrate planning over time.",
                    "Procrastination Radar: Highlights tasks that have been repeatedly postponed or rescheduled.",
                    "Energy & Circadian Curves: Visualizes your daily energy rhythm and detected peak performance windows."
                )
            )

            // 11. Cognitive Science
            GuideSection(
                emoji = "🧠",
                title = "Science & Cognitive Frameworks",
                color = MaterialTheme.colorScheme.surface,
                textColor = MaterialTheme.colorScheme.onSurface,
                summary = "The scientific principles powering neuroFlow.",
                steps = listOf(
                    "Temporal Motivation Theory (Steel): Hyperbolic discounting where urgency escalates exponentially near deadlines.",
                    "Eat the Frog (Tracy): Morning-weighted willpower boosts for high-effort, dreaded tasks.",
                    "Implementation Intentions (Gollwitzer): If-Then structured action plans dramatically boost completion rates.",
                    "Zeigarnik Effect: Incomplete tasks create cognitive tension that resolves upon execution.",
                    "Progress Principle (Amabile): Making small visible progress in meaningful work is the single strongest human motivator.",
                    "Circadian Rhythm Matching (Anderson et al.): Harmonizing analytical tasks with biological temperature peaks."
                )
            )

            // 12. Troubleshooting
            GuideSection(
                emoji = "💡",
                title = "When You Feel Overwhelmed or Stuck",
                color = NeuroFlowColors.PurpleLight,
                textColor = NeuroFlowColors.PurpleDark,
                summary = "Quick reset sequence for low-motivation days.",
                steps = listOf(
                    "1. Switch Preset: Go to Settings → Priority Weights and select 'Momentum / ADHD Friendly'.",
                    "2. Chunk It: Split your biggest task into 30-min or 45-min sessions.",
                    "3. 5-Minute Rule: Open Focus Mode and commit to working for just 5 minutes.",
                    "4. If still blocked, do a low-effort administrative task first to build momentum.",
                    "5. Remember: Consistency beats intensity. Doing one focused sprint per day builds unstoppable momentum."
                )
            )

            Spacer(Modifier.height(16.dp))

            // Footer tip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeuroFlowColors.Purple.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Do less, but do it daily. Consistency beats intensity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeuroFlowColors.Purple
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideSection(
    emoji: String,
    title: String,
    color: Color,
    textColor: Color,
    summary: String,
    steps: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header — always visible, tap to expand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
                    Text(summary, fontSize = 13.sp, color = textColor.copy(alpha = 0.75f))
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable steps
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                    Spacer(Modifier.height(4.dp))
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(textColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                step,
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.9f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


