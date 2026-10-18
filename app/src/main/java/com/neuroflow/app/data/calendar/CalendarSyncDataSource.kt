package com.neuroflow.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.domain.model.TaskStatus
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface CalendarSyncRepository {
    suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow>
    suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long? = null,
        durationMinutes: Int? = null
    ): Long?
    suspend fun deleteTaskEvent(taskId: String): Boolean
    suspend fun handleTaskStateChanged(task: TaskEntity): Boolean
}

interface AndroidCalendarSyncRepository : CalendarSyncRepository {
    suspend fun findPrimaryWritableCalendarId(): Long?
    suspend fun findEventIdByTaskId(taskId: String): Long?
}

/**
 * ADR-002 Native Android Calendar Sync DataSource
 *
 * Bridges local Android OS Calendar Provider via CalendarContract.Instances and CalendarContract.Events.
 * 100% offline-first, multi-provider (Google, Outlook, Exchange, DAVx5), zero OAuth tokens.
 *
 * Enforces:
 * 1. Filtering out declined events, canceled events, and free availability.
 * 2. Excluding self-authored neuroFlow blocks ("com.neuroflow.app" / "neuroflow://task/...").
 * 3. Sweep-line interval coalescing with 5-minute post-meeting decompression buffer for meetings >= 45m.
 * 4. Loop-safe 2-way write-back for Daily Frogs and locked Focus Blocks with idempotent upsert and lifecycle cleanup.
 */
@Singleton
class CalendarSyncDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarSyncRepository, AndroidCalendarSyncRepository {

    override suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        return withContext(Dispatchers.IO) {
            val raw = queryInstances(startMillis, endMillis)
            coalesceWindows(raw)
        }
    }

    internal fun queryInstances(startMillis: Long, endMillis: Long): List<TimeSlotWindow> {
        if (startMillis >= endMillis) return emptyList()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return emptyList()
        }

        val results = mutableListOf<TimeSlotWindow>()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        val uri = builder.build()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        )

        val selection = null
        val selectionArgs = null
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val availIdx = cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
                val attendeeIdx = cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val statusIdx = cursor.getColumnIndex(CalendarContract.Instances.STATUS)
                val orgIdx = cursor.getColumnIndex(CalendarContract.Instances.ORGANIZER)
                val pkgIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_PACKAGE)
                val appUriIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_URI)

                while (cursor.moveToNext()) {
                    val s = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val e = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val desc = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                    val avail = if (availIdx >= 0) cursor.getInt(availIdx) else CalendarContract.Events.AVAILABILITY_BUSY
                    val attendeeStatus = if (attendeeIdx >= 0) cursor.getInt(attendeeIdx) else CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else CalendarContract.Events.STATUS_CONFIRMED
                    val organizer = if (orgIdx >= 0) cursor.getString(orgIdx) ?: "" else ""
                    val customPkg = if (pkgIdx >= 0) cursor.getString(pkgIdx) ?: "" else ""
                    val customUri = if (appUriIdx >= 0) cursor.getString(appUriIdx) ?: "" else ""

                    // 1. Filter out free availability
                    if (avail == CalendarContract.Events.AVAILABILITY_FREE) {
                        continue
                    }

                    // 2. Filter out declined or canceled events
                    if (attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
                        status == CalendarContract.Events.STATUS_CANCELED) {
                        continue
                    }

                    // 3. Filter out self-authored neuroFlow blocks (loop prevention)
                    val isNeuroFlowBlock = customPkg.equals(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        customUri.startsWith(CUSTOM_APP_URI_PREFIX, ignoreCase = true) ||
                        title.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        desc.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        organizer.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        title.contains("[neuroflow]", ignoreCase = true) ||
                        title.contains("[Flow]", ignoreCase = true) ||
                        desc.contains("[neuroflow]", ignoreCase = true)

                    if (isNeuroFlowBlock) {
                        continue
                    }

                    if (e > s) {
                        results.add(TimeSlotWindow(startMillis = s, endMillis = e))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query calendar provider", e)
        }

        return results
    }

    override suspend fun findPrimaryWritableCalendarId(): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        // 1. Primary writable calendar: IS_PRIMARY == 1 and access >= CAL_ACCESS_CONTRIBUTOR
        try {
            val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Primary calendar query failed, trying fallback", e)
        }

        // 2. Fallback: First writable calendar
        try {
            val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Fallback writable calendar query failed", e)
        }

        null
    }

    override suspend fun findEventIdByTaskId(taskId: String): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        )
        val selection = "${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?"
        val selectionArgs = arrayOf(NEUROFLOW_PACKAGE_ID, "$CUSTOM_APP_URI_PREFIX$taskId")

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                    if (idIdx >= 0) {
                        return@withContext cursor.getLong(idIdx)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to query event by taskId $taskId", e)
        }

        null
    }

    override suspend fun upsertTaskEvent(
        task: TaskEntity,
        startMillis: Long?,
        durationMinutes: Int?
    ): Long? = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext null

        val calendarId = findPrimaryWritableCalendarId() ?: return@withContext null

        val effectiveStart = startMillis
            ?: task.scheduledTime
            ?: task.scheduledDate
            ?: System.currentTimeMillis()

        val effectiveDuration = durationMinutes
            ?: (if (task.plannedBlockMinutes > 0) task.plannedBlockMinutes else if (task.estimatedDurationMinutes > 0) task.estimatedDurationMinutes else 25)

        val endMillis = effectiveStart + (effectiveDuration * 60_000L)

        val cleanTitle = task.title
            .removePrefix("✓ [Flow] ")
            .removePrefix("🎯 [Flow] ")
            .removePrefix("[Flow] ")
            .trim()

        val formattedTitle = when {
            task.status == TaskStatus.COMPLETED -> "✓ [Flow] $cleanTitle"
            task.isFrog -> "🎯 [Flow] $cleanTitle"
            else -> "[Flow] $cleanTitle"
        }

        val formattedDesc = if (task.description.isBlank()) {
            "[neuroflow] Managed focus block"
        } else {
            "${task.description}\n\n[neuroflow] Managed focus block"
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, formattedTitle)
            put(CalendarContract.Events.DESCRIPTION, formattedDesc)
            put(CalendarContract.Events.DTSTART, effectiveStart)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, NEUROFLOW_PACKAGE_ID)
            put(CalendarContract.Events.CUSTOM_APP_URI, "$CUSTOM_APP_URI_PREFIX${task.id}")
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        val existingEventId = findEventIdByTaskId(task.id)

        try {
            if (existingEventId != null) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                context.contentResolver.update(updateUri, values, null, null)
                existingEventId
            } else {
                val insertUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                insertUri?.lastPathSegment?.toLongOrNull()
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to upsert calendar event for task ${task.id}", e)
            null
        }
    }

    override suspend fun deleteTaskEvent(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext false

        val existingEventId = findEventIdByTaskId(taskId) ?: return@withContext false

        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
            val deletedRows = context.contentResolver.delete(deleteUri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            android.util.Log.w("CalendarSyncDataSource", "Failed to delete calendar event for task $taskId", e)
            false
        }
    }

    override suspend fun handleTaskStateChanged(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        when (task.status) {
            TaskStatus.COMPLETED -> {
                val existingEventId = findEventIdByTaskId(task.id)
                if (existingEventId != null) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
            TaskStatus.ARCHIVED -> {
                deleteTaskEvent(task.id)
            }
            TaskStatus.ACTIVE -> {
                if (task.scheduledDate == null && task.scheduledTime == null) {
                    deleteTaskEvent(task.id)
                } else if (task.isScheduleLocked || task.isFrog) {
                    upsertTaskEvent(task) != null
                } else {
                    false
                }
            }
        }
    }

    companion object {
        const val NEUROFLOW_PACKAGE_ID = "com.neuroflow.app"
        const val CUSTOM_APP_URI_PREFIX = "neuroflow://task/"
        private const val DECOMPRESSION_THRESHOLD_MILLIS = 45 * 60_000L
        private const val BUFFER_MILLIS = 5 * 60_000L

        /**
         * Sweep-line interval coalescing and post-meeting decompression buffer.
         *
         * Formula (ADR-002 Section 6.4):
         * BusyWindow_effective = [s_merged, e_merged + I(e_merged - s_merged >= 45m) * 5m]
         * Overlapping intervals and adjacent intervals separated by < 5m are coalesced into a unified non-overlapping blocked horizon.
         */
        fun coalesceWindows(rawWindows: List<TimeSlotWindow>): List<TimeSlotWindow> {
            val valid = rawWindows.filter { it.endMillis > it.startMillis }
            if (valid.isEmpty()) return emptyList()

            val sorted = valid.sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
            val merged = mutableListOf<TimeSlotWindow>()

            var curStart = sorted[0].startMillis
            var curEnd = sorted[0].endMillis

            for (i in 1 until sorted.size) {
                val next = sorted[i]
                // Coalesce if overlapping or adjacent separated by < 5 minutes
                if (next.startMillis <= curEnd + BUFFER_MILLIS) {
                    curEnd = max(curEnd, next.endMillis)
                } else {
                    val duration = curEnd - curStart
                    val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                        curEnd + BUFFER_MILLIS
                    } else {
                        curEnd
                    }
                    merged.add(TimeSlotWindow(curStart, effectiveEnd))
                    curStart = next.startMillis
                    curEnd = next.endMillis
                }
            }

            // Final interval
            val duration = curEnd - curStart
            val effectiveEnd = if (duration >= DECOMPRESSION_THRESHOLD_MILLIS) {
                curEnd + BUFFER_MILLIS
            } else {
                curEnd
            }
            merged.add(TimeSlotWindow(curStart, effectiveEnd))

            if (merged.size <= 1) return merged

            // Final sweep to resolve any secondary overlap created by decompression expansion
            val finalPass = mutableListOf<TimeSlotWindow>()
            var pStart = merged[0].startMillis
            var pEnd = merged[0].endMillis

            for (i in 1 until merged.size) {
                val win = merged[i]
                if (win.startMillis <= pEnd) {
                    pEnd = max(pEnd, win.endMillis)
                } else {
                    finalPass.add(TimeSlotWindow(pStart, pEnd))
                    pStart = win.startMillis
                    pEnd = win.endMillis
                }
            }
            finalPass.add(TimeSlotWindow(pStart, pEnd))
            return finalPass
        }
    }
}
