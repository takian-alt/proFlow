package com.neuroflow.app.domain.model

enum class PtsRecurrence {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM
}

enum class PtsCategory {
    MINDFULNESS,
    EXERCISE,
    MEAL,
    FAMILY,
    BREAK,
    CUSTOM
}

data class ProtectedTimeSlot(
    val id: String,
    val label: String,
    val startMinuteOfDay: Int, // 0..1439
    val durationMinutes: Int,   // 15..360
    val recurrence: PtsRecurrence = PtsRecurrence.DAILY,
    val weekdayBitmask: Int = 0b1111111, // bit 1 = Sunday, bit 2 = Monday, etc.
    val category: PtsCategory = PtsCategory.BREAK,
    val recoveryBufferMinutes: Int = 0, // buffer after slot
    val isEnabled: Boolean = true
)
