package com.neuroflow.app.domain.assistant

/**
 * Instant on-device structured goal decomposer that turns any goal title into clean,
 * actionable milestones and paper-fast starter steps with zero latency or network dependency.
 */
object GoalBreakdownEngine {

    fun generateLocalBreakdown(
        goalTitle: String,
        goalDescription: String = ""
    ): List<GoalBreakdownMilestone> {
        val combined = "$goalTitle $goalDescription".lowercase()

        return when {
            combined.containsAny("exam", "study", "course", "learn", "degree", "cert", "book", "read") -> listOf(
                GoalBreakdownMilestone(
                    title = "1. Map Syllabus & Materials",
                    subtasks = listOf(
                        "List core topics & high-weight chapters",
                        "Gather notes, textbooks, and practice tests"
                    ),
                    estimatedMinutesPerTask = 25
                ),
                GoalBreakdownMilestone(
                    title = "2. Deep Study Sprints",
                    subtasks = listOf(
                        "Complete first core topic with active recall notes",
                        "Summarize tough concepts in your own words"
                    ),
                    estimatedMinutesPerTask = 45
                ),
                GoalBreakdownMilestone(
                    title = "3. Practice & Polish",
                    subtasks = listOf(
                        "Take a timed practice quiz on weak areas",
                        "Review mistakes and lock in final summary sheet"
                    ),
                    estimatedMinutesPerTask = 40
                )
            )

            combined.containsAny("app", "code", "build", "ship", "launch", "website", "feature", "project", "mvp") -> listOf(
                GoalBreakdownMilestone(
                    title = "1. Scope & Sketch",
                    subtasks = listOf(
                        "Write 3 must-have deliverables for v1",
                        "Sketch rough user flow & architecture"
                    ),
                    estimatedMinutesPerTask = 30
                ),
                GoalBreakdownMilestone(
                    title = "2. Core Build",
                    subtasks = listOf(
                        "Build working vertical slice end-to-end",
                        "Wire edge cases, state persistence, and UI polish"
                    ),
                    estimatedMinutesPerTask = 50
                ),
                GoalBreakdownMilestone(
                    title = "3. Verify & Release",
                    subtasks = listOf(
                        "Test critical flows and fix top bugs",
                        "Ship release build and share update"
                    ),
                    estimatedMinutesPerTask = 35
                )
            )

            combined.containsAny("fit", "gym", "run", "weight", "health", "workout", "marathon", "muscle") -> listOf(
                GoalBreakdownMilestone(
                    title = "1. Baseline & Routine",
                    subtasks = listOf(
                        "Pick 3 weekly workout days & anchor times",
                        "Set up gear, water bottle, and tracking log"
                    ),
                    estimatedMinutesPerTask = 20
                ),
                GoalBreakdownMilestone(
                    title = "2. Consistency Block",
                    subtasks = listOf(
                        "Complete first 3 scheduled sessions",
                        "Prep high-protein meals / recovery snacks"
                    ),
                    estimatedMinutesPerTask = 45
                ),
                GoalBreakdownMilestone(
                    title = "3. Progressive Milestone",
                    subtasks = listOf(
                        "Check weekly progress and nudge load up 5–10%",
                        "Schedule active recovery walk or mobility session"
                    ),
                    estimatedMinutesPerTask = 30
                )
            )

            combined.containsAny("write", "blog", "paper", "thesis", "essay", "video", "youtube", "newsletter") -> listOf(
                GoalBreakdownMilestone(
                    title = "1. Outline & Angle",
                    subtasks = listOf(
                        "Brain-dump raw bullet points & key hook",
                        "Organize bullets into 3–5 clear sections"
                    ),
                    estimatedMinutesPerTask = 25
                ),
                GoalBreakdownMilestone(
                    title = "2. Ugly First Draft",
                    subtasks = listOf(
                        "Draft first half without editing",
                        "Finish full rough draft to the conclusion"
                    ),
                    estimatedMinutesPerTask = 45
                ),
                GoalBreakdownMilestone(
                    title = "3. Edit & Publish",
                    subtasks = listOf(
                        "Tighten headline, clarity, and examples",
                        "Final proofread and hit publish"
                    ),
                    estimatedMinutesPerTask = 30
                )
            )

            else -> listOf(
                GoalBreakdownMilestone(
                    title = "1. Clarify & Kick Off",
                    subtasks = listOf(
                        "Define what 'done' looks like for ${goalTitle.trim().take(36)}",
                        "Complete one 15-minute starter step today"
                    ),
                    estimatedMinutesPerTask = 20
                ),
                GoalBreakdownMilestone(
                    title = "2. Core Execution",
                    subtasks = listOf(
                        "Tackle the biggest leverage milestone first",
                        "Clear any blockers or missing dependencies"
                    ),
                    estimatedMinutesPerTask = 45
                ),
                GoalBreakdownMilestone(
                    title = "3. Review & Wrap Up",
                    subtasks = listOf(
                        "Check quality against your original target",
                        "Finalize and archive completed deliverables"
                    ),
                    estimatedMinutesPerTask = 30
                )
            )
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { this.contains(it, ignoreCase = true) }
}
