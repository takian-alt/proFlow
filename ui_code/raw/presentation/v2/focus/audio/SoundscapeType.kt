package com.neuroflow.app.presentation.v2.focus.audio

/**
 * Neuroscience-backed soundscape categories for auditory stimulation during deep work.
 */
enum class SoundscapeType(
    val displayName: String,
    val subtitle: String,
    val emoji: String = "",
    val shortChipLabel: String = displayName
) {
    NONE(
        displayName = "Off",
        subtitle = "Silence",
        emoji = "",
        shortChipLabel = "Off"
    ),
    GAMMA_40HZ(
        displayName = "40 Hz Gamma",
        subtitle = "Binaural Prefrontal Synchrony",
        emoji = "🧠",
        shortChipLabel = "40Hz Gamma 🧠"
    ),
    MONAURAL_GAMMA_40HZ(
        displayName = "40 Hz Monaural Gamma",
        subtitle = "Acoustic Interference + Pink Cushion",
        emoji = "⚡",
        shortChipLabel = "40Hz Monaural ⚡"
    ),
    BETA_16HZ(
        displayName = "16 Hz SMR Beta",
        subtitle = "Monaural Low-Fatigue Focus",
        emoji = "🎯",
        shortChipLabel = "16Hz Beta 🎯"
    ),
    BROWN_NOISE(
        displayName = "Deep Brown Noise",
        subtitle = "1/f² Stochastic Acoustic Masking",
        emoji = "🌊",
        shortChipLabel = "Deep Brown 🌊"
    ),
    PINK_NOISE(
        displayName = "Flow Pink Noise / Rain",
        subtitle = "1/f Calming Spectral Density",
        emoji = "🌧️",
        shortChipLabel = "Pink Rain 🌧️"
    )
}
