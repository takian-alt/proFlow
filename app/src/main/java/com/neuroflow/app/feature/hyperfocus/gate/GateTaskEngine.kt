package com.neuroflow.app.feature.hyperfocus.gate

import kotlin.random.Random

enum class GateTaskType { TRANSCRIBE, COUNT_BACK, RESTATED }

data class GateTask(
    val type: GateTaskType,
    val prompt: String,
    val expected: List<String>
)

/**
 * Effort-only cognition gate. It can always be passed: mismatches are reported, never fatal,
 * and after three of them the task auto-passes. What it costs is seconds and attention.
 */
class GateTaskEngine(
    private val random: Random = Random.Default
) {
    fun newTask(): GateTask = when (GateTaskType.entries.random(random)) {
        GateTaskType.TRANSCRIBE -> single(TRANSCRIPTIONS, "Type exactly what you see", GateTaskType.TRANSCRIBE)
        GateTaskType.RESTATED -> single(SENTENCES, "Retype this sentence word for word", GateTaskType.RESTATED)
        GateTaskType.COUNT_BACK -> countBack()
    }

    private fun single(pool: List<String>, prompt: String, type: GateTaskType): GateTask =
        GateTask(type, prompt, listOf(pool[random.nextInt(pool.size)]))

    private fun countBack(): GateTask {
        var start = random.nextInt(100, 1000)
        while (start % 100 == 0) start = random.nextInt(100, 1000)
        val values = (0 until 4).map { (start - it * 7).toString() }
        return GateTask(
            GateTaskType.COUNT_BACK,
            "Start at $start and count back by 7. Enter the next four values.",
            values
        )
    }

    fun checkAnswer(task: GateTask, answer: List<String>): Boolean =
        task.expected.indices.all { i ->
            task.expected[i].trim().equals(answer.getOrNull(i)?.trim().orEmpty(), ignoreCase = true)
        }

    fun isReasonValid(reason: String): Boolean = reason.trim().length >= MIN_REASON_CHARS

    fun shouldAutoPass(mismatchCount: Int): Boolean = mismatchCount >= MAX_MISMATCHES

    fun allowsSubmit(mismatchCount: Long, elapsedMillis: Long, reason: String): Boolean =
        isReasonValid(reason) &&
            elapsedMillis >= MIN_ELAPSED_MILLIS &&
            (mismatchCount == 0L || mismatchCount % MAX_MISMATCHES == 0L)

    companion object {
        const val MAX_MISMATCHES = 3
        const val MIN_ELAPSED_MILLIS = 12_000L
        const val MIN_REASON_CHARS = 8

        private val TRANSCRIPTIONS = listOf(
            "quiet desk", "left hinge", "amber frost", "plain route", "cold anchor",
            "dim margin", "slow orbit", "brick valve", "firm slate", "gray punch"
        )
        private val SENTENCES = listOf(
            "The train leaves before the shop opens.",
            "Nine files went to the wrong folder again.",
            "She paid for lunch with the last note in her coat.",
            "A short walk clears more than an hour of staring.",
            "The ladder leaned on the window, not the wall.",
            "Every alarm I keep is one I chose to miss."
        )
    }
}
