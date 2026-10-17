package com.neuroflow.app.data.calendar

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.neuroflow.app.domain.service.TimeSlotWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

interface CalendarSyncRepository {
    suspend fun getBusyWindows(startMillis: Long, endMillis: Long): List<TimeSlotWindow>
}

interface AndroidCalendarSyncRepository : CalendarSyncRepository

/**
 * ADR-002 Native Android Calendar Sync DataSource
 *
 * Bridges local Android OS Calendar Provider via CalendarContract.Instances.
 * 100% offline-first, multi-provider (Google, Outlook, Exchange, DAVx5), zero OAuth tokens.
 *
 * Enforces:
 * 1. Filtering out declined events, canceled events, and free availability.
 * 2. Excluding self-authored neuroFlow blocks ("com.neuroflow.app").
 * 3. Sweep-line interval coalescing with 5-minute post-meeting decompression buffer for meetings >= 45m.
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
            CalendarContract.Instances.ORGANIZER
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

                while (cursor.moveToNext()) {
                    val s = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L
                    val e = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val desc = if (descIdx >= 0) cursor.getString(descIdx) ?: "" else ""
                    val avail = if (availIdx >= 0) cursor.getInt(availIdx) else CalendarContract.Events.AVAILABILITY_BUSY
                    val attendeeStatus = if (attendeeIdx >= 0) cursor.getInt(attendeeIdx) else CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else CalendarContract.Events.STATUS_CONFIRMED
                    val organizer = if (orgIdx >= 0) cursor.getString(orgIdx) ?: "" else ""

                    // 1. Filter out free availability
                    if (avail == CalendarContract.Events.AVAILABILITY_FREE) {
                        continue
                    }

                    // 2. Filter out declined or canceled events
                    if (attendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
                        status == CalendarContract.Events.STATUS_CANCELED) {
                        continue
                    }

                    // 3. Filter out self-authored neuroFlow blocks
                    val isNeuroFlowBlock = title.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        desc.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        organizer.contains(NEUROFLOW_PACKAGE_ID, ignoreCase = true) ||
                        title.contains("[neuroflow]", ignoreCase = true) ||
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

    companion object {
        const val NEUROFLOW_PACKAGE_ID = "com.neuroflow.app"
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
