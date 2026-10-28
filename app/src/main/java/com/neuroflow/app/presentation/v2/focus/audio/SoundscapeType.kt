package com.neuroflow.app.presentation.v2.focus.audio

/**
 * Neuroscience-backed soundscape categories for auditory stimulation during deep work.
 *
 * Each profile targets a specific cognitive regime. Choose based on your current task:
 * - **Memorization / Study**: THETA_GAMMA_ENCODE (default)
 * - **Alert Focus / Coding**: MONAURAL_GAMMA_40HZ or BETA_16HZ
 * - **Reading / Writing**: PINK_NOISE or BROWN_NOISE
 * - **Binaural (stereo headphones only)**: GAMMA_40HZ
 */
enum class SoundscapeType(
    val displayName: String,
    val subtitle: String,
    val emoji: String = "",
    val shortChipLabel: String = displayName,
    /** Short contextual tag describing when this profile is most effective. */
    val usageTag: String = ""
) {
    NONE(
        displayName = "Off",
        subtitle = "Silence",
        emoji = "",
        shortChipLabel = "Off",
        usageTag = ""
    ),
    GAMMA_40HZ(
        displayName = "40 Hz Gamma",
        subtitle = "Binaural Prefrontal Synchrony",
        emoji = "🧠",
        shortChipLabel = "40Hz Gamma 🧠",
        usageTag = "Pre-study priming · Stereo headphones required"
    ),
    MONAURAL_GAMMA_40HZ(
        displayName = "40 Hz Monaural Gamma",
        subtitle = "Acoustic Interference + Pink Cushion",
        emoji = "⚡",
        shortChipLabel = "40Hz Monaural ⚡",
        usageTag = "Alertness & reaction speed · N-back tasks"
    ),
    BETA_16HZ(
        displayName = "16 Hz SMR Beta",
        subtitle = "Monaural Low-Fatigue Focus",
        emoji = "🎯",
        shortChipLabel = "16Hz Beta 🎯",
        usageTag = "Coding, math & problem-solving"
    ),
    BROWN_NOISE(
        displayName = "Deep Brown Noise",
        subtitle = "1/f² Stochastic Acoustic Masking",
        emoji = "🌊",
        shortChipLabel = "Deep Brown 🌊",
        usageTag = "Distraction masking · Deep reading"
    ),
    PINK_NOISE(
        displayName = "Flow Pink Noise / Rain",
        subtitle = "1/f Calming Spectral Density",
        emoji = "🌧️",
        shortChipLabel = "Pink Rain 🌧️",
        usageTag = "Calm focus · Writing & creative work"
    ),
    THETA_GAMMA_ENCODE(
        displayName = "Memory Encode (θ-γ)",
        subtitle = "Theta-Gamma PAC for Rapid Memorization",
        emoji = "🧬",
        shortChipLabel = "Memory θ-γ 🧬",
        usageTag = "Memorization · Flashcards · Exam prep"
    );

    companion object {
        /** The recommended default soundscape for new focus sessions. */
        val DEFAULT: SoundscapeType = THETA_GAMMA_ENCODE
    }
}
