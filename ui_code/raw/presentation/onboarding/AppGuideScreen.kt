package com.neuroflow.app.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

enum class GuideCategory(val label: String, val emoji: String) {
    ALL("All Topics", "🌐"),
    QUICK_START("Quick Start", "🚀"),
    CIRCADIAN_FLOW("Flow & Circadian", "⚡"),
    PHARMACOKINETICS("Stimulants & PK", "☕"),
    SCHEDULING_PLAN("Plan & Calendar", "🤖"),
    EXECUTIVE_ADHD("ADHD & Executive", "🧠"),
    FOCUS_DSP("Focus & Audio DSP", "🎯"),
    AI_NLP("AI & Smart Tasks", "🪄"),
    VAULT_CHRONO("Vault & Chronotype", "🗃️")
}

data class GuideSectionItem(
    val id: String,
    val category: GuideCategory,
    val emoji: String,
    val tag: String,
    val title: String,
    val summary: String,
    val accentColor: Color,
    val keyPoints: List<Pair<String, String>>,
    val proTip: String? = null,
    val scienceNote: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGuideScreen(onNavigateBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GuideCategory.ALL) }
    var expandedIds by remember { mutableStateOf(setOf("quick_start")) }

    val guideItems = remember {
        listOf(
            GuideSectionItem(
                id = "quick_start",
                category = GuideCategory.QUICK_START,
                emoji = "🚀",
                tag = "3-MINUTE PROTOCOL",
                title = "Quick Start: Zero-Friction Setup",
                summary = "The essential daily activation sequence to get productive in under 3 minutes.",
                accentColor = Color(0xFF6366F1),
                keyPoints = listOf(
                    "1. The Rule of 3-5 Tasks" to "Limit your active queue to 3–5 items maximum. Overflowing backlogs trigger executive overwhelm and freeze responses. Park all other ideas in the Vault Icebox until today's queue is cleared.",
                    "2. Mark Today's 🐸 Frog" to "Select one high-impact, high-resistance task as your daily Frog. Completing it early capitalizes on peak morning cognitive bandwidth before decision fatigue accumulates.",
                    "3. Read Biological Weather" to "Glance at the Circadian Hero Barometer on the Flow Cockpit. If you're in a Peak window, dive straight into deep work. If in a Dip, switch to low-friction organizing or admin.",
                    "4. Single-Task Sprint" to "Launch Focus Mode for 25–45 minutes with procedural background noise. The display is kept awake automatically so your micro-steps and timer remain in sight.",
                    "5. Celebrate, Log & Decompress" to "Mark the task done or record offline minutes. Rest for 5–15 minutes before switching to the next item to allow neurochemical replenishment."
                ),
                proTip = "If a task triggers avoidance or dread, commit to working for just 5 minutes, or tap 'Break Down' to generate a 2-minute doorway step.",
                scienceNote = "Overcoming the activation energy barrier (E_a) of the first micro-step is the single most effective intervention for executive dysfunction."
            ),
            GuideSectionItem(
                id = "circadian_cockpit",
                category = GuideCategory.CIRCADIAN_FLOW,
                emoji = "⚡",
                tag = "BORBÉLY TWO-PROCESS MODEL",
                title = "Flow Cockpit & Circadian Energy Barometer",
                summary = "Lab-grade biophysical energy modeling combining homeostatic sleep pressure and double-harmonic circadian rhythm.",
                accentColor = Color(0xFF10B981),
                keyPoints = listOf(
                    "Process S (Sleep Pressure)" to "Tracks adenosine accumulation from waking (S_0 -> 1.0). Discharges logarithmically during nocturnal sleep and daytime power naps (S_nap).",
                    "Process C (Circadian Wave)" to "Biological double-harmonic alertness wave (φ = t_wake + 4.5h) modeling your natural morning peak and afternoon post-prandial dip.",
                    "⚡ PEAK (75–100%)" to "Maximum synaptic plasticity. Strictly reserved for deep analytical thinking, strategic architecture, and creative breakthroughs.",
                    "📈 REBOUND (55–74%)" to "Secondary alertness wave in the late afternoon. Optimal for collaborative execution, meetings, and tactical progress.",
                    "☕ DIP (35–54%)" to "Biological circadian trough (13:30–15:30). Switch to light admin, organization, or take a 15–20 minute restorative power nap.",
                    "🧘 RECOVERY NEEDED (<35%)" to "Neural exhaustion detected. Take a 15m NSDR (Non-Sleep Deep Rest) session or a walk outside before resuming high-stakes work.",
                    "🌙 WIND-DOWN (<20% or >21:30)" to "Melatonin secretion rising. Execute Evening Shutdown to close open loops, archive completed wins, and queue tomorrow's frog.",
                    "Fast Kalman Circadian Learner" to "The app passively observes your actual completion timestamps to refine your personalized peak hours over time without manual quizzes."
                ),
                proTip = "Never schedule high-friction analytical tasks during your Dip (13:30–15:30). Protecting this window prevents chronic burnout.",
                scienceNote = "Based on Borbély's Two-Process Model of Sleep Regulation. Continuous non-threshold modeling eliminates artificial hourly drop-offs."
            ),
            GuideSectionItem(
                id = "stimulant_pk",
                category = GuideCategory.PHARMACOKINETICS,
                emoji = "☕",
                tag = "BATEMAN TWO-COMPARTMENT PK",
                title = "Stimulant Pharmacokinetics & Crash Guard",
                summary = "Two-compartment differential equation modeling for caffeine and prescription ADHD medications with crash warnings.",
                accentColor = Color(0xFFF59E0B),
                keyPoints = listOf(
                    "Bateman Differential Model" to "Computes absorption rate (k_a) and elimination half-life (t_half) to track real-time active plasma concentration and adenosine masking over 24 hours.",
                    "+ ☕ Coffee (100mg)" to "Standard caffeine pharmacokinetic curve (t_half = 5.0h, k_a = 2.1). Reaches peak alertness at ~45 minutes.",
                    "+ 🍵 Green Tea (50mg)" to "L-Theanine modulated caffeine (t_half = 3.5h, k_a = 1.6). Promotes relaxed alertness with smoother onset and reduced jitter.",
                    "+ 💊 Meds IR (10mg)" to "Rapid-acting prescription stimulant (t_half = 4.0h, k_a = 2.5). Sharp alertness rise with pronounced wear-off boundary.",
                    "+ 💊 Meds XR (20mg)" to "Extended-release dual-bead kinetic curve (t_half = 7.0h, k_a = 1.2). Sustained therapeutic plasma level across 8–10 hours.",
                    "Live Plasma & 4-Second Undo" to "Displays active plasma concentration (~X mg active in plasma) on the Cockpit. A 4-second non-destructive Undo snackbar and [Clear doses] affordance ensure effortless error correction.",
                    "⚡ Rebound Crash Guard" to "When wear-off and adenosine rebound coincide, an amber banner warns you against tackling high-friction tasks during neurochemical depletion.",
                    "Protected Slot Keyword Auto-Detection" to "Calendar slots labeled 'Coffee', 'Caffeine', 'Meds', 'Vyvanse', or 'Adderall' are automatically converted into pharmacokinetic bouts."
                ),
                proTip = "Avoid high-dose stimulants after 14:00 to prevent Slow-Wave Sleep (SWS) suppression, even if you feel able to fall asleep on schedule.",
                scienceNote = "Adenosine continues accumulating in the brain while caffeine blocks receptor sites. When caffeine detaches, rebound drowsiness occurs naturally."
            ),
            GuideSectionItem(
                id = "social_jetlag",
                category = GuideCategory.CIRCADIAN_FLOW,
                emoji = "🌅",
                tag = "CHRONOBIOLOGY & TELEMETRY",
                title = "Social Jetlag & Wearable Sleep Telemetry",
                summary = "Detecting weekend sleep schedule drift and normalizing wearable sleep metrics for circadian precision.",
                accentColor = Color(0xFF818CF8),
                keyPoints = listOf(
                    "Social Jetlag (Δφ_SJL)" to "Measures the circular divergence between workday sleep midpoint (MSW) and weekend sleep midpoint (MSF).",
                    "🌅 Weekend Drift Badge" to "When weekend sleep drift exceeds 1.0 hour, the Cockpit displays an amber Weekend Drift alert recommending 10–15 minutes of outdoor sunlight exposure upon waking.",
                    "Stage-Weighted Telemetry" to "Processes wearable sleep logs with lab-grade stage efficiency (d_eff = 1.15 Deep + 1.05 REM + 0.85 Light) and automatically coalesces fragmented wake episodes (WASO <= 45m).",
                    "Daytime Power Naps" to "Daytime naps between 10m and 180m discharge homeostatic pressure (ΔS) with an initial 20m sleep inertia envelope without corrupting nocturnal circadian anchors."
                ),
                proTip = "Morning sunlight within 30 minutes of waking triggers SCN photopigment melanopsin, resetting your master circadian clock and accelerating evening melatonin onset.",
                scienceNote = "A 2-hour weekend sleep midpoint delay produces biological jetlag equivalent to flying across two time zones every single Monday."
            ),
            GuideSectionItem(
                id = "math_scheduling",
                category = GuideCategory.SCHEDULING_PLAN,
                emoji = "🤖",
                tag = "4-VECTOR OPTIMIZATION",
                title = "Mathematical Auto-Scheduling & Calendar Sync",
                summary = "Non-saturating algorithmic schedule generation with two-way Google Calendar synchronization and ultradian buffers.",
                accentColor = Color(0xFF38BDF8),
                keyPoints = listOf(
                    "4-Vector Task Scoring Engine" to "Prioritization Formula: Score = 100 × [0.30 U + 0.30 I + 0.25 E + 0.15 M], balancing Urgency (U), Impact (I), Circadian Energy Match (E), and Momentum (M).",
                    "Demand-Elastic Utility Damping λ(τ)" to "Logarithmically scales utility with duration, preventing huge 3-hour projects from starving shorter high-leverage tasks.",
                    "Calendar Sync & Decompression Buffers" to "Two-way sync via Android CalendarContract. External meetings are imported as busy windows, and meetings ≥ 45m automatically receive a 5-minute cognitive decompression buffer.",
                    "Ultradian Auto-Chunking" to "Tasks ≥ 60m placed into fragmented schedule slots are automatically split into linked sub-tasks ('🔗 Part 1/2' & '🔗 Part 2/2') chained by DAG dependencies.",
                    "🔒 Schedule Locking" to "Tap 'Lock Schedule' on any task to pin it to an exact time slot. Auto-scheduling will route all other tasks around locked anchors.",
                    "Protected Recurring Slots" to "Configure non-negotiable blocks for meals, gym, or family time in Settings that are guaranteed never to be booked over."
                ),
                proTip = "Tap 'Auto Schedule Day' in the Plan hub each morning. The engine will place your deep work during biological peaks and admin during dips.",
                scienceNote = "Ultradian rhythm research shows human focus peaks in 90-minute waves, after which synaptic efficiency declines rapidly without restorative rest."
            ),
            GuideSectionItem(
                id = "adhd_executive",
                category = GuideCategory.EXECUTIVE_ADHD,
                emoji = "🧠",
                tag = "NEURODIVERGENT ERGONOMICS",
                title = "ADHD Executive Hardening & Behavioral Science",
                summary = "Science-backed tools to defeat task paralysis, time blindness, dread resistance, and hyperbolic discounting.",
                accentColor = Color(0xFF8B5CF6),
                keyPoints = listOf(
                    "Implementation Intentions (WOOP)" to "Formulate Wish, Outcome, Obstacle, and If-Then Plan before starting work. Research demonstrates this doubles execution follow-through rates.",
                    "Doorway Micro-Steps (≤ 4m)" to "Decomposes daunting tasks into a microscopic 2–4 minute first physical action (e.g. 'Open document and write title'), drastically lowering activation energy.",
                    "⚡ Dopamine Warm-Up Badges" to "When a task has been postponed ≥ 3 times, a Dopamine Warm-Up / Paired Primer badge appears, granting momentum bonuses upon starting.",
                    "Parkinson's Law Urgency Ignition" to "Non-linear priority scaling triggers when deadlines enter the [0.25h, 6.0h] window, overcoming ADHD time-blindness naturally.",
                    "Zeigarnik Completion Pull" to "Leverages psychological closure inertia with sinusoidal pull peaking at 60–80% sub-task progress: M_Zeigarnik(p) = 30p + 10 sin(πp).",
                    "Context-Switching Entropy Matrix" to "Awards +8.5 point bonuses for batching similar categories within 45m and penalizes jarring context jumps (-8.5 pts).",
                    "Zero-Guilt Rollover" to "When ≥ 3 tasks become overdue, the Cockpit provides a zero-shame rollover card to batch-reschedule or archive without guilt."
                ),
                proTip = "When feeling stuck, do not force yourself to complete the entire project. Complete only Step 1 (the Doorway Step). Action generates momentum.",
                scienceNote = "Hyperbolic discounting causes distant deadlines to be cognitively discounted. Nonlinear urgency ignition restores salience before crisis mode."
            ),
            GuideSectionItem(
                id = "focus_audio_dsp",
                category = GuideCategory.FOCUS_DSP,
                emoji = "🎯",
                tag = "DEEP WORK COCKPIT",
                title = "Focus Mode, Soundscapes & Ergonomics",
                summary = "Uninterrupted attention shielding with pure-Kotlin DSP soundscapes, display wake lock, and solfeggio chimes.",
                accentColor = Color(0xFF6366F1),
                keyPoints = listOf(
                    "Display Wake Lock (FLAG_KEEP_SCREEN_ON)" to "The display remains awake throughout active focus sprints, keeping timers and micro-steps visible without needing to touch the phone.",
                    "Pure Kotlin DSP Audio Synthesizer" to "Real-time streaming acoustic synthesis over AudioTrack (16-bit PCM, 44.1kHz stereo) with 400ms raised-cosine anti-click envelopes.",
                    "🌊 Pink & Brown Noise" to "Attenuates environmental distractions, speech chatter, and office noise for seamless focus immersion.",
                    "🧠 Alpha (10Hz) & Gamma (40Hz) Binaural Beats" to "Promotes neural entrainment and cognitive binding for sustained analytical depth.",
                    "🌧️ Procedural Rain & Forest Soundscapes" to "Natural stochastic acoustic masks designed to calm sympathetic nervous system hyperactivity.",
                    "Dual-Harmonic Solfeggio Chimes" to "Procedural 528Hz + 792Hz harmonic bell tone with double-pulse tactile vibration upon timer expiration.",
                    "Interruption Shield" to "Gentle 3-second breathing interstitial when attempting to leave early, protecting your hyperfocus state.",
                    "Manual Time Logging" to "Work performed away from your phone can be retroactively logged to maintain accurate XP, stats, and estimation calibration."
                ),
                proTip = "Pair Brown Noise with 40Hz Gamma binaural beats through stereo headphones for complex coding, math, or deep analytical writing.",
                scienceNote = "40Hz gamma entrainment stimulates auditory cortex resonance and has been linked to enhanced working memory and cognitive binding."
            ),
            GuideSectionItem(
                id = "smart_ai_nlp",
                category = GuideCategory.AI_NLP,
                emoji = "🪄",
                tag = "PRIVACY-FIRST AI & NLP",
                title = "Smart Task Creation & Gemini AI BYOK",
                summary = "Natural language task syntax parsing and offline-resilient AI task decomposition.",
                accentColor = Color(0xFF10B981),
                keyPoints = listOf(
                    "Natural Language Heuristic Parser" to "Type naturally: 'Draft budget tomorrow 3pm 45m #deepwork 🐸' — dates, times, durations, hashtags, and frog flags are extracted instantly.",
                    "Archetype-Driven Decomposition" to "Classifies tasks into 6 core archetypes (Technical, Writing, Admin, Creative, Research, Everyday) and builds structured micro-step hierarchies.",
                    "Pure BYOK (Bring Your Own Key)" to "Zero hardcoded keys or shared servers. Add your Gemini API key in Settings; it is encrypted locally in DataStore and never transmitted elsewhere.",
                    "Resilient Cascade & TTL Caching" to "Features an in-memory 20-minute cache and 60s backoff guard. If network fails or no key is configured, neuroFlow degrades seamlessly to local heuristic decomposition with zero errors.",
                    "Adaptive Behavioral Directives" to "Audits avoided or deleted tasks and synthesizes a concise coaching directive to refine future task decompositions."
                ),
                proTip = "Add hashtags like #deepwork, #offline, #admin, or #quickwin to automatically map tasks into dedicated energy zones and context profiles.",
                scienceNote = "Closed-loop meta-prompting injects persistent coaching directives into task enrichment prompts to maintain alignment with minimal token footprint."
            ),
            GuideSectionItem(
                id = "vault_chronotype",
                category = GuideCategory.VAULT_CHRONO,
                emoji = "🗃️",
                tag = "KNOWLEDGE & CALIBRATION",
                title = "The Vault, Scratchpad & Chronotype Profiles",
                summary = "Icebox backlog, quick thought capture, and circadian chronotype tuning.",
                accentColor = Color(0xFFF59E0B),
                keyPoints = listOf(
                    "Quick Scratchpad Notes" to "Long-press the floating action button (+) or tap the Notes icon in the top bar to record thoughts instantly without breaking focus.",
                    "The Icebox (Zero-Distraction Backlog)" to "Park non-urgent ideas and someday/maybe tasks. Keeps your active Cockpit uncluttered until you are ready to pull them into Plan.",
                    "MEQ Chronotype Assessment" to "Complete the validated Morningness-Eveningness Questionnaire (MEQ) to calibrate your biological phase anchors (Morning Lark, Intermediate, Night Owl).",
                    "Manual Peak Energy Tuning" to "Prefer manual control? Enable manual peak anchor in Settings to lock your preferred cognitive focus hours.",
                    "Evening Shutdown Ritual" to "Review completed tasks, log remaining thoughts into scratchpad, and trigger shutdown to transition smoothly into wind-down."
                ),
                proTip = "Review your Vault scratchpad at the end of the day during Evening Shutdown to convert fleeting notes into structured tasks.",
                scienceNote = "Externalizing open loops into a trusted capture system reduces cognitive tension and prevents bedtime rumination."
            )
        )
    }

    val filteredItems = remember(searchQuery, selectedCategory, guideItems) {
        guideItems.filter { item ->
            val matchesCategory = selectedCategory == GuideCategory.ALL || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.tag.contains(searchQuery, ignoreCase = true) ||
                item.summary.contains(searchQuery, ignoreCase = true) ||
                item.keyPoints.any { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "neuroFlow V2 Manual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                        Text(
                            text = "Biological High Performance & Executive Guide",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeuroFlowPaletteV2.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuroFlowPaletteV2.SurfaceCard,
                    titleContentColor = NeuroFlowPaletteV2.TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Modern Hero Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = NeuroFlowPaletteV2.SurfaceCard,
                border = BorderStroke(
                    1.dp,
                    NeuroFlowPaletteV2.specularBorderBrush(NeuroFlowThemeV2Colors.ElectricIndigo, 0.85f)
                ),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧬", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "System Architecture & Biophysics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aligning task execution with Borbély circadian oscillations, Bateman stimulant pharmacokinetics, and neurodivergent executive ergonomics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(
                        "Search algorithms, biophysics, features...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeuroFlowPaletteV2.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeuroFlowPaletteV2.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = NeuroFlowPaletteV2.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeuroFlowPaletteV2.SurfaceCard,
                    unfocusedContainerColor = NeuroFlowPaletteV2.SurfaceCard,
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    unfocusedBorderColor = NeuroFlowPaletteV2.BorderSubtle,
                    focusedTextColor = NeuroFlowPaletteV2.TextPrimary,
                    unfocusedTextColor = NeuroFlowPaletteV2.TextPrimary
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuideCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.22f) else NeuroFlowPaletteV2.SurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeuroFlowThemeV2Colors.ElectricIndigo else NeuroFlowPaletteV2.BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(category.emoji, fontSize = 13.sp)
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeuroFlowPaletteV2.TextPrimary else NeuroFlowPaletteV2.TextSecondary
                            )
                        }
                    }
                }
            }

            // Results count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredItems.size} Module${if (filteredItems.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeuroFlowPaletteV2.TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )

                if (expandedIds.size < filteredItems.size) {
                    TextButton(
                        onClick = { expandedIds = filteredItems.map { it.id }.toSet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Expand All",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                            fontSize = 12.sp
                        )
                    }
                } else if (expandedIds.isNotEmpty()) {
                    TextButton(
                        onClick = { expandedIds = emptySet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "Collapse All",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuroFlowPaletteV2.TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Empty search state
            if (filteredItems.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuroFlowPaletteV2.SurfaceCard,
                    border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No matching modules found",
                            style = MaterialTheme.typography.titleSmall,
                            color = NeuroFlowPaletteV2.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or switching categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary
                        )
                    }
                }
            }

            // Guide Module Cards
            filteredItems.forEach { item ->
                val isExpanded = expandedIds.contains(item.id)
                ModernGuideCard(
                    item = item,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedIds = if (isExpanded) {
                            expandedIds - item.id
                        } else {
                            expandedIds + item.id
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Mindset Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NeuroFlowPaletteV2.SurfaceCard,
                border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeuroFlowPaletteV2.OptimalEmerald.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "The Core Operating Law",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeuroFlowPaletteV2.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Consistency beats intensity. One focused sprint in biological sync per day creates compounding neurological momentum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeuroFlowPaletteV2.TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModernGuideCard(
    item: GuideSectionItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_chevron_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NeuroFlowPaletteV2.SurfaceCard,
        border = BorderStroke(
            1.dp,
            if (isExpanded) {
                NeuroFlowPaletteV2.specularBorderBrush(item.accentColor, 0.75f)
            } else {
                Brush.verticalGradient(listOf(NeuroFlowPaletteV2.BorderSubtle, NeuroFlowPaletteV2.BorderSubtle))
            }
        ),
        tonalElevation = if (isExpanded) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = item.accentColor,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowPaletteV2.TextPrimary
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (isExpanded) item.accentColor else NeuroFlowPaletteV2.TextTertiary,
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = NeuroFlowPaletteV2.TextSecondary,
                lineHeight = 18.sp
            )

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        color = item.accentColor.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Key Points
                    item.keyPoints.forEachIndexed { index, (pointTitle, pointDesc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(item.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = item.accentColor
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pointTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeuroFlowPaletteV2.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pointDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowPaletteV2.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Pro-Tip Callout
                    item.proTip?.let { tip ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = item.accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("💡", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRO-TIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = item.accentColor,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeuroFlowPaletteV2.TextPrimary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }

                    // Science / Mathematical Callout
                    item.scienceNote?.let { science ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = NeuroFlowPaletteV2.SurfaceElevated,
                            border = BorderStroke(1.dp, NeuroFlowPaletteV2.BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔬", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BIOPHYSICAL BASIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeuroFlowPaletteV2.TextTertiary,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = science,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeuroFlowPaletteV2.TextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
