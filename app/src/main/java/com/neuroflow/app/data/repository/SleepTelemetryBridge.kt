package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.entity.SleepLogEntity

typealias SleepLog = SleepLogEntity
typealias SleepTelemetryRepository = com.neuroflow.app.data.sleep.SleepTelemetryRepository

fun SleepLog(
    id: String = java.util.UUID.randomUUID().toString(),
    bedTimeMillis: Long,
    wakeTimeMillis: Long,
    qualityRating: Int = 4,
    notes: String = ""
): SleepLogEntity = SleepLogEntity(
    id = id,
    startAt = bedTimeMillis,
    endAt = wakeTimeMillis,
    durationMinutes = ((wakeTimeMillis - bedTimeMillis) / 60_000L).toInt().coerceAtLeast(1),
    notes = notes
)
