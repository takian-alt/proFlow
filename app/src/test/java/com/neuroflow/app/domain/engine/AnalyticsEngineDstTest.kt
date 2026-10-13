package com.neuroflow.app.domain.engine

import com.neuroflow.app.data.local.entity.TimeSessionEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.TimeZone

class AnalyticsEngineDstTest : StringSpec({
    "seven day focus trend keeps sessions on spring DST day in correct calendar day" {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        try {
            fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
                Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

            val session = TimeSessionEntity(
                taskId = "late-sunday-focus",
                startedAt = at(2026, Calendar.MARCH, 8, 23, 30),
                endedAt = at(2026, Calendar.MARCH, 9, 0, 0),
                durationMinutes = 30f
            )

            val trend = AnalyticsEngine.sevenDayFocusTrend(
                sessions = listOf(session),
                nowMillis = at(2026, Calendar.MARCH, 9, 12, 0)
            )

            trend[5].first shouldBe "Sun"
            trend[5].second shouldBe 30f
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
})
