# proFlow — How to Best Use the App

> **TL;DR:** Add tasks with good details → trust the score → tap Focus → ship.

This guide walks you through every feature from first launch to advanced workflows, so you can extract maximum value from proFlow's science-backed prioritization engine.

---

## Table of Contents

1. [First Launch & Onboarding](#1-first-launch--onboarding)
2. [Adding Great Tasks](#2-adding-great-tasks)
3. [Understanding the Priority Score](#3-understanding-the-priority-score)
4. [The Eisenhower Matrix](#4-the-eisenhower-matrix)
5. [Focus Mode & the Pomodoro Timer](#5-focus-mode--the-pomodoro-timer)
6. [WOOP Framework](#6-woop-framework)
7. [Schedule & Time Blocking](#7-schedule--time-blocking)
8. [Analytics Dashboard](#8-analytics-dashboard)
9. [Identity Affirmations](#9-identity-affirmations)
10. [Goals (Yearly & Weekly)](#10-goals-yearly--weekly)
11. [Ulysses Contracts](#11-ulysses-contracts)
12. [HyperFocus Mode](#12-hyperfocus-mode)
13. [Focus Launcher](#13-focus-launcher)
14. [Autonomy Nudge System](#14-autonomy-nudge-system)
15. [Fresh Start Engine](#15-fresh-start-engine)
16. [Task Splitter](#16-task-splitter)
17. [Peak Energy Detector](#17-peak-energy-detector)
18. [Distraction Engine](#18-distraction-engine)
19. [Home Screen Widget](#19-home-screen-widget)
20. [Task History](#20-task-history)
21. [Manual Time Logging](#21-manual-time-logging)
22. [Daily Workflow — Putting It All Together](#22-daily-workflow--putting-it-all-together)
23. [Tips & Tricks](#23-tips--tricks)

---

## 1. First Launch & Onboarding

When you open proFlow for the first time, the onboarding flow collects three things:

| Step | What it sets | Why it matters |
|---|---|---|
| **Identity** | Your name and one identity statement (e.g., *"I am someone who finishes what I start"*) | Seeds the scoring engine's self-concept anchors |
| **Peak energy hours** | Your best focus window (e.g., 9 am – 12 pm) | Circadian scoring boosts high-effort tasks during your peak |
| **First task** | One concrete task to get started | Removes the blank-slate problem immediately |

**Tip:** Be specific with your identity statement — it will surface at strategic moments (streak breaks, fresh starts) to reconnect you with your *why*.

---

## 2. Adding Great Tasks

The quality of the priority score depends entirely on the details you provide. Tap **+** from any screen to open the New Task sheet.

### Fields that affect the score

| Field | Range | Impact |
|---|---|---|
| **Quadrant** | DO FIRST / SCHEDULE / DELEGATE / ELIMINATE | Biggest single factor — sets the base weight |
| **Impact** | 0–100 | How much completing this task moves the needle |
| **Effort** | 0–100 | How hard it is; low-effort tasks get a quick-win boost |
| **Energy level** | LOW / MEDIUM / HIGH | Matched against your peak hours via Cognitive Load Theory |
| **Deadline** | Date + optional time | Drives hyperbolic urgency scaling (Temporal Motivation Theory) |
| **🐸 Frog** | Toggle | Marks your most-dreaded task; gets a big morning boost |
| **If-Then Plan** | Free text | *"When I sit down at my desk, I will…"* doubles completion rates |
| **Enjoyment** | 0–100 | Temptation Bundling: enjoyable tasks score better when you need a momentum win |
| **Public commitment** | Toggle | Social accountability adds a loss-aversion multiplier |
| **Goal link** | Select a goal | Attaches the task to a long-term goal; goal-risk amplification kicks in if the goal is at risk |

### Quick tips for better tasks

- **Be specific:** "Write introduction for Q2 report" scores better than "Report"
- **Set even rough deadlines:** Without a deadline, temporal urgency stays flat
- **Mark your frog early:** Doing the hardest task first unlocks momentum for the rest of the day
- **Add an if-then plan:** Research shows this *doubles* follow-through — it takes 20 seconds

---

## 3. Understanding the Priority Score

Every task has a live score from **0 to 999**. Higher = do it sooner.

### How it works

The `TaskScoringEngine` combines 16+ evidence-based frameworks into a single number:

1. **Quadrant weight** — DO FIRST tasks start much higher than ELIMINATE
2. **Deadline pressure** — urgency rises *exponentially* as deadlines approach (Temporal Motivation Theory)
3. **Energy matching** — HIGH-energy tasks score higher *during* your peak hours, not outside them
4. **Circadian timing** — analytical tasks peak in the morning; creative tasks peak late morning/afternoon
5. **Frog boost** — morning multiplier for your most-dreaded task (Eat the Frog)
6. **Quick-win momentum** — low-effort tasks get a boost when you need to build momentum (Fogg Tiny Habits)
7. **Open-loop pressure** — the longer a task sits incomplete, the more it nags (Zeigarnik Effect)
8. **Progress boost** — started tasks get a momentum multiplier to help you finish what you began
9. **If-then plan** — tasks with a concrete plan score higher, scaled by effort (Gollwitzer 1999)
10. **Commitment multiplier** — public commitments add a loss-aversion amplifier
11. **Distraction cost** — if the app you'd need to use for this task distracts you, the score adjusts

### Score breakdown

In Focus Mode, tap **"Why this score?"** to see every component and its contribution. Use this to understand which factors are limiting a task's score and what you could add.

### What you don't need to do

You **don't** need to manually manage scores. Add good task details, trust the engine, and tap the ✦ badge (highest-scored task) to always start with the right thing.

---

## 4. The Eisenhower Matrix

The Matrix is your command centre. It shows all active tasks across four quadrants.

| Quadrant | Meaning | Default action |
|---|---|---|
| **DO FIRST** | Urgent + Important | Work on these today |
| **SCHEDULE** | Important, not urgent | Block time this week |
| **DELEGATE** | Urgent, low impact | Batch, automate, or hand off |
| **ELIMINATE** | Neither | Delete or archive |

### Navigation

- Tap any task to open it in **Focus Mode**
- Tap a **quadrant header** to see all tasks in that quadrant (including overflow)
- The **✦ badge** marks the single highest-scored task — your recommended next action

### Auto-assignment

If you leave the quadrant blank, the scoring engine will auto-assign it based on the task's impact, deadline, and value scores. You can always override this.

---

## 5. Focus Mode & the Pomodoro Timer

Focus Mode is where work happens. Tap any task from the Matrix, Schedule, or Launcher to enter it.

### The timer

| Button | Action |
|---|---|
| **Start** | Begins time tracking for this task |
| **Pause** | Pauses the session (state is saved) |
| **Done** | Marks the task complete; opens WOOP reflection |
| **Skip** | Advances to the next highest-scored task |

### Pomodoro mode

The built-in 🍅 timer runs 25-minute work sprints. After each sprint:
1. You earn a break
2. Cumulative time is logged automatically
3. The WOOP reflection prompt appears after the session

### XP

Completing a task earns XP proportional to its **impact score**. High-impact tasks reward more. XP is tracked in the Analytics dashboard.

### Score breakdown

While in Focus Mode, tap the score badge to open the **score breakdown sheet**. Every contributing factor is listed with its weight and current value.

---

## 6. WOOP Framework

WOOP (Wish – Outcome – Obstacle – Plan) is a four-step mental contrasting technique backed by extensive research. proFlow integrates it at two moments:

### Before a focus session (WoopPromptSheet)

When you tap **Start Focus**, a WOOP prompt appears:
1. **Wish** — what do you want to accomplish in this session?
2. **Outcome** — what's the best result you could imagine?
3. **Obstacle** — what's the most likely internal obstacle?
4. **Plan** — if [obstacle] happens, then I will [action]

You can skip the prompt, but completing it significantly improves follow-through.

### After a focus session (Mini WOOP Reflection)

When you mark a task **Done**, a short reflection screen opens:
- Was the session as difficult as you expected?
- Did your obstacle appear?
- How did your plan hold up?

### Affective Forecasting

The `WoopEngine` tracks your **expectation vs. reality** over time. In the Analytics dashboard, the *WOOP Calibration* insight shows how well you predict session difficulty — a MAPE under 25% means your estimates are reliable.

---

## 7. Schedule & Time Blocking

The Schedule screen shows a day/week timeline with all tasks that have a scheduled time.

### Adding to the schedule

1. Tap an **hour slot** to assign an existing task or create a new one for that time
2. Tap the **+** button to add a task without a specific slot
3. In the task editor, toggle **Lock schedule** to pin a task to its slot permanently

### Locked tasks

Locked tasks:
- Cannot be moved by the scoring engine
- Receive a **schedule-lock scoring boost** when their slot is approaching
- Are marked with a 🔒 icon

### Best practice

Use **SCHEDULE** quadrant tasks for important-but-not-urgent work. Block 90-minute deep-work sessions in the morning during your peak energy window.

---

## 8. Analytics Dashboard

The Analytics screen turns your work history into actionable insights.

### Cards

| Card | What it shows |
|---|---|
| **XP & Points** | Total XP, today's XP, this-week's XP, top-5 tasks by XP earned |
| **7-Day Trend** | Vico-powered bar chart of focus time per day |
| **Completion Rate** | % of tasks completed on time |
| **Streaks** | Consecutive days with at least one completed task |
| **MAPE** | Mean Absolute Percentage Error for your time estimates |
| **Neuro Boost Insights** | Completion rate for frog tasks, anxiety tasks, public commitments, if-then plans |
| **Procrastination Radar** | Top tasks you keep deferring — worth addressing |
| **Dynamic Peak Energy** | Auto-detected focus window vs. your manually configured one |
| **Ulysses Contracts** | Live contracts and WIN/LOSS record |

### Using analytics to improve

- If your **MAPE** is high, your effort estimates are off — spend a week deliberately calibrating
- If the **Procrastination Radar** shows the same task repeatedly, either split it, delete it, or schedule a Ulysses Contract
- If your **7-Day Trend** has gaps, a Ulysses Contract or streak commitment can help rebuild momentum

---

## 9. Identity Affirmations

Identity affirmations are short statements about who you are as a person — *not* goals, but identity anchors.

**Examples:**
- *"I am someone who ships work every day"*
- *"I finish what I start"*
- *"I do the hard things first"*

### How to use them

1. Open the **Identity** screen from the navigation drawer
2. Add 3–5 statements that resonate with you
3. Read them before a difficult focus session

### Why it works

Self-Determination Theory shows that identity-based motivation is more durable than outcome-based motivation. proFlow surfaces your identity statements at strategic moments (Fresh Start prompts, Ulysses Contract creation) to reinforce your self-concept.

---

## 10. Goals (Yearly & Weekly)

Goals connect your daily tasks to long-term outcomes.

### Creating goals

Open the **navigation drawer** → Goals section:
- **Yearly goals** — big annual targets (e.g., "Launch v1.0 of my app")
- **Weekly goals** — shorter, 7-day commitments (e.g., "Finish authentication module")

### Linking tasks to goals

In the task editor, select a **Goal** from the dropdown. The scoring engine will:
- Apply a **goal-risk amplification** multiplier (Loss Aversion) when the linked goal is at risk
- Show goal progress in the Analytics Goal Progress card

### Best practice

Set 1–3 **yearly goals** at the start of each year. Create 1–2 **weekly goals** every Monday. Link your highest-impact tasks to goals so the scoring engine knows what truly matters.

---

## 11. Ulysses Contracts

A Ulysses Contract is a binding commitment for a critical task — named after the Greek hero who had himself tied to the mast to resist the Sirens.

### Creating a contract

1. Open any task → tap **Create Ulysses Contract**
2. Write a specific commitment: *"I will complete the login screen by Friday at 5 pm"*
3. Set a deadline — the contract's WIN or LOSS is evaluated automatically at the deadline

### Automatic evaluation

The `UlyssesEvaluatorWorker` checks every contract at its deadline:
- **WIN** — task was completed before the deadline
- **LOSS** — task was not completed

### Where to use contracts

Use Ulysses Contracts for tasks that you've been procrastinating on despite their importance. The psychological cost of a recorded LOSS can be a powerful motivator.

**Tip:** View your WIN/LOSS record in the **Analytics → Ulysses Contracts card** to track your commitment reliability.

---

## 12. HyperFocus Mode

HyperFocus is a whole-phone distraction blocker. It's the nuclear option for deep work.

### How to activate

1. Open **Launcher Settings → HyperFocus**
2. Select apps to block
3. Set a task target (e.g., "complete 3 tasks")
4. Tap **Activate HyperFocus**

### Required permissions

| Permission | Why |
|---|---|
| **Accessibility Service** | Intercepts app launches to show the blocking overlay |
| **Usage Access** | Reads which apps you've opened to compute distraction scores |

Grant both permissions from the **Permission Setup screen** that appears on first activation.

### While active

- Blocked apps show a motivational **Blocking Overlay** instead of opening
- A **HyperFocus status bar** appears on all launcher screens
- To exit early, request an **unlock code** (AES-256-GCM encrypted, time-limited)
- After repeated failures to enter the code, a lockout period activates

### Rewards

Completing your task target earns a **reward tier**: Bronze → Silver → Gold → Platinum, based on tasks completed and total focus time. View earned rewards on the **Rewards screen**.

### Best practice

Use HyperFocus for your most important deep-work sessions — not every session. Reserve it for mornings when you need to do your hardest work without distractions.

---

## 13. Focus Launcher

The Focus Launcher replaces your Android home screen with a minimal, task-first interface.

### Setting proFlow as your launcher

1. Press the **Home** button
2. Android will ask which launcher to use — select **proFlow** and tap **Always**

Or go to *Android Settings → Apps → Default Apps → Home app → proFlow*.

### What you'll see

| Element | Purpose |
|---|---|
| **Focus Task Card** | Always-visible card with your top-scored task; tap *Start Focus* to begin immediately |
| **Habit Quick Row** | Today's due habits for one-tap check-in |
| **App Grid** | Paged app grid (configurable columns × rows) |
| **Dock** | 5-slot bottom row; long-press any icon for context menu |
| **Date/Time** | Tappable clock; swipe down to expand notifications |

### Gestures

| Gesture | Action |
|---|---|
| Swipe up | Open App Drawer |
| Swipe down | Expand notification shade |
| Long-press home screen | Enter edit mode (drag icons, add widgets) |
| Swipe right | Open Quick Stats Panel |

### Quick Stats Panel

Swipe right from the home screen to see today's completed task count, active streak, current XP, and recent session totals — without leaving the launcher.

### Customization (Launcher Settings)

Long-press the home screen → **Settings**, or open the proFlow app → **Launcher Settings**:
- Grid size (3 × 5 to 6 × 8)
- Clock style (Digital / Minimal)
- Icon shape (Circle / Squircle / Rounded-Square / Teardrop / System Default)
- Icon pack selection
- Notification badges toggle
- Backup and restore

---

## 14. Autonomy Nudge System

If a task goes untouched for 2 hours, proFlow sends a smart notification with two action buttons:

| Button | What it does |
|---|---|
| **Not ready yet** | Snoozes the nudge for a later window |
| **It feels too big** | Automatically splits the task into 3 subtasks (see Task Splitter) |

### Why it exists

Procrastination often stems from tasks that feel too large or ambiguous. The nudge gives you a low-friction way to either defer consciously or decompose the task — both of which are better than indefinite avoidance.

---

## 15. Fresh Start Engine

The Fresh Start Engine detects motivational reset moments and surfaces an encouraging prompt:

| Trigger | Example |
|---|---|
| Monday | "New week — new start" |
| Month start | "New month, new momentum" |
| Streak break | "Everyone falls off — here's how to come back" |
| 3-day+ absence | "Welcome back — let's rebuild" |

### Frequency cap

proFlow shows a Fresh Start prompt **at most once per ISO week** to avoid notification fatigue.

### How to use it

When you see a Fresh Start prompt, take 60 seconds to:
1. Review your current goals
2. Pick one task to complete today
3. Start a focus session immediately

The power of fresh starts is the psychological permission they grant to begin again without guilt.

---

## 16. Task Splitter

When a task is flagged as "too big" (via the Autonomy Nudge or manually), the Task Splitter breaks it into three equal subtasks.

### What the splitter does

- Creates **Part 1/3**, **Part 2/3**, and **Part 3/3** subtasks
- Each inherits the parent's **quadrant**, **priority**, and **one-third of the impact score**
- The parent task is **archived** (not deleted)

### When to use it

Use the Task Splitter for any task that has been sitting in your queue for 3+ days. Large tasks score well in theory but often feel impossible to start — breaking them down makes the first step concrete.

---

## 17. Peak Energy Detector

The Peak Energy Detector learns your personal focus windows from your session history.

### How it works

The `PeakEnergyDetector` uses **exponential recency weighting** on your completed sessions:
- Sessions from the last 7 days → weight × 3
- Sessions from the last 30 days → weight × 1.5
- Older sessions → weight × 0.5

It then blends the detected window with your manually configured peak hours via linear interpolation.

### Where to see it

**Analytics → Dynamic Peak Energy card** shows:
- Your auto-detected peak window
- Your manually configured window
- The detection confidence level (higher after more sessions)

### Best practice

Run at least 10–15 focus sessions before trusting the auto-detected window. Until then, rely on your manual peak hours setting from **Settings → Peak Energy Hours**.

---

## 18. Distraction Engine

The Distraction Engine computes a **distraction score (0–100)** for every app on your device, using `UsageStatsManager` data correlated with your focus sessions.

### Scoring layers

1. **App-switch frequency** — how often you jump to and from this app (logarithmic scale)
2. **Category weight** — social media > messaging > video > email > utilities
3. **Interruption depth** — how deeply it interrupted a focus session
4. **Recovery time** — how long it took to return to focused work after using it
5. **Circadian timing** — distraction impact is higher during your peak hours

### Where to find it

**Launcher Settings → Distraction Scores**: each installed app shows a 0–100 slider. Use *Quick Categorize* to bulk-assign sensible defaults by category.

### How it feeds the scoring engine

Apps with high distraction scores contribute a **distraction cost penalty** to tasks that require them. This means a task that requires you to open a high-distraction app will score slightly lower, naturally nudging you toward lower-distraction work first.

---

## 19. Home Screen Widget

The proFlow widget shows your **current top-priority task** on any Android home screen (or launcher page).

### Adding the widget

- On a standard Android launcher: long-press home screen → Widgets → proFlow → Focus Task
- On the proFlow Launcher: long-press a page → Add Widget → proFlow

### What it shows

- Task title
- Priority score
- Quadrant badge
- **Start Focus** button (launches the focus session directly)

The widget updates every time the `FocusWidgetUpdateWorker` runs (periodic background refresh).

---

## 20. Task History

The History screen is a searchable archive of all completed tasks.

### What you can do

- **Search** by task title or date
- **View full audit trail** for any completed task (time logged, sessions, score at completion)
- **Use it for weekly reviews** — see everything you shipped last week

### Weekly review practice

Every Friday, open History and count your completions. Cross-reference with Analytics → 7-Day Trend. Ask:
- Did I complete my highest-impact tasks?
- Which tasks were deferred repeatedly?
- What's one thing I'd do differently next week?

---

## 21. Manual Time Logging

If you worked on a task outside of a timed focus session (on paper, in a meeting, mentally), you can log that time manually.

### Two ways to log

1. **ManualTimeLogSheet** — from the task detail view, tap **Log Time** to open the quick bottom sheet
2. **Log Time screen** — full-screen time entry accessible from the navigation menu

Both methods add to the task's total logged time and feed the Analytics time-tracking charts.

---

## 22. Daily Workflow — Putting It All Together

Here's the recommended daily routine for maximum productivity with proFlow:

### Morning (15 minutes)

1. Open proFlow (or glance at the Focus Launcher)
2. Check the **Daily Plan notification** (7 am) — your top 3 tasks for the day
3. Review the **Eisenhower Matrix** for any overnight changes
4. Confirm your **peak energy hours** are still accurate in Settings
5. Look at the **✦ badge** — that's your first task

### Deep work block (your peak hours)

1. Tap the ✦ task → **Start Focus**
2. Complete the **WOOP prompt** (30 seconds)
3. Work in **Pomodoro** sprints (25 min on, 5 min off)
4. After the session, complete the **WOOP reflection** (1 minute)
5. Tap **Done** or **Skip** to the next task

### Afternoon (lighter work)

1. The scoring engine will surface lower-effort or SCHEDULE tasks
2. Use **Schedule view** to work through time-blocked tasks
3. Check for any **Autonomy Nudge** notifications — address them or split the tasks

### Evening (5 minutes)

1. Open **Analytics** — check today's XP and streak
2. Log any manual time
3. Add tomorrow's tasks with good detail
4. Glance at the **Procrastination Radar** — any tasks you need to address?

---

## 23. Tips & Tricks

### Power-user tips

- **Trust the score.** When in doubt, do the ✦ task. The engine has more information than your gut in that moment.
- **Add if-then plans to your top 3 tasks.** The Gollwitzer research is clear — it doubles follow-through. It takes 20 seconds.
- **Set your peak energy hours accurately.** The Circadian scoring multiplier has a significant effect on which tasks surface during your best hours.
- **Review your Distraction Scores monthly.** As your app usage patterns change, the distraction weights should too.
- **Use Ulysses Contracts sparingly.** Reserve them for tasks you've genuinely been avoiding — the psychological weight works best when it's rare.

### Common mistakes to avoid

- **Don't set everything as DO FIRST.** The quadrant only matters if it's accurate. If everything is urgent, nothing is.
- **Don't skip deadlines.** Even a rough deadline (e.g., "end of week") activates Temporal Motivation Theory and significantly improves prioritization.
- **Don't ignore the Procrastination Radar.** If the same task appears every week, make a decision: do it, delete it, or delegate it.
- **Don't set HyperFocus for every session.** It's a high-friction mode — save it for when you genuinely need the nuclear option.

### Getting the most from the scoring engine

| Action | Scoring benefit |
|---|---|
| Add a deadline | Activates hyperbolic urgency scaling |
| Mark a task as a Frog 🐸 | Big morning boost |
| Add an if-then plan | Score multiplier scaled by effort |
| Toggle public commitment | Loss-aversion amplifier |
| Link to a goal at risk | Goal-risk amplification |
| Complete sessions consistently | Peak Energy Detector improves accuracy |
| Keep Distraction Scores calibrated | Distraction cost reflects reality |

---

*For technical documentation, see [README.md](README.md). For contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).*
