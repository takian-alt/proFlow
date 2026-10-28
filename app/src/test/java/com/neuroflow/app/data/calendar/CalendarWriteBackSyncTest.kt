package com.neuroflow.app.data.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.local.entity.startOfLocalDay
import com.neuroflow.app.domain.model.Recurrence
import com.neuroflow.app.domain.model.TaskStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarWriteBackSyncTest {

    private lateinit var app: Application
    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: CalendarSyncDataSource

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        contentResolver = mockk(relaxed = true)
        val testContext = object : android.content.ContextWrapper(app) {
            override fun getContentResolver(): ContentResolver = this@CalendarWriteBackSyncTest.contentResolver
        }
        dataSource = CalendarSyncDataSource(testContext)
    }

    @Test
    fun `findPrimaryWritableCalendarId returns primary calendar id`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        cursor.addRow(arrayOf(101L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns cursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 101L
    }

    @Test
    fun `findPrimaryWritableCalendarId falls back to first writable calendar when no primary exists`() = runTest {
        val emptyPrimaryCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))

        val fallbackCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        fallbackCursor.addRow(arrayOf(202L, 0, CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR))

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns emptyPrimaryCursor

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                match { !it.contains("${CalendarContract.Calendars.IS_PRIMARY} = 1") },
                any(),
                any()
            )
        } returns fallbackCursor

        val calId = dataSource.findPrimaryWritableCalendarId()
        calId shouldBe 202L
    }

    @Test
    fun `findPrimaryWritableCalendarId returns null when permission denied`() = runTest {
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.denyPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

        val unprivilegedSource = CalendarSyncDataSource(app)
        val calId = unprivilegedSource.findPrimaryWritableCalendarId()
        calId shouldBe null
    }

    @Test
    fun `findEventIdByTaskId queries using loop-prevention package and uri`() = runTest {
        val taskId = "task-focus-999"
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        cursor.addRow(arrayOf(777L, "neuroflow://task/$taskId"))

        val selectionArgsSlot = slot<Array<String>>()
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                capture(selectionArgsSlot),
                any()
            )
        } returns cursor

        val eventId = dataSource.findEventIdByTaskId(taskId)
        eventId shouldBe 777L

        selectionArgsSlot.captured[0] shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        selectionArgsSlot.captured[1] shouldBe "neuroflow://task/$taskId"
    }

    @Test
    fun `upsertTaskEvent inserts new event with loop prevention stamp and flow title`() = runTest {
        val task = TaskEntity(
            id = "task-alpha-1",
            title = "Architectural Review",
            description = "Review ADR-006 write-back",
            scheduledDate = 1770000000000L,
            scheduledTime = 3600_000L,
            estimatedDurationMinutes = 45,
            isScheduleLocked = true
        )

        // Mock primary calendar
        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns calCursor

        // No existing event
        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/8888")

        val newId = dataSource.upsertTaskEvent(task)
        newId shouldBe 8888L

        val captured = valuesSlot.captured
        captured.getAsLong(CalendarContract.Events.CALENDAR_ID) shouldBe 55L
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "[Flow] Architectural Review"
        captured.getAsString(CalendarContract.Events.CUSTOM_APP_PACKAGE) shouldBe CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID
        val expectedStart = 1770000000000L + 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
        captured.getAsLong(CalendarContract.Events.DTEND) shouldBe expectedStart + 45 * 60_000L
    }

    @Test
    fun `upsertTaskEvent formats frog task with frog title prefix`() = runTest {
        val frogTask = TaskEntity(
            id = "task-frog-7",
            title = "Eat the Frog",
            isFrog = true,
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 30
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/9999")

        dataSource.upsertTaskEvent(frogTask)

        val captured = valuesSlot.captured
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "🎯 [Flow] Eat the Frog"
    }

    @Test
    fun `upsertTaskEvent updates existing event in place when event already exists`() = runTest {
        val task = TaskEntity(
            id = "task-update-10",
            title = "Rescheduled Deep Work",
            scheduledDate = 1770000000000L,
            estimatedDurationMinutes = 60
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        // Existing event found
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(12345L, "neuroflow://task/task-update-10"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val updateUriSlot = slot<Uri>()
        every {
            contentResolver.update(capture(updateUriSlot), any(), any(), any())
        } returns 1

        val updatedId = dataSource.upsertTaskEvent(task)
        updatedId shouldBe 12345L

        updateUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 12345L)
        verify(exactly = 0) { contentResolver.insert(any(), any()) }
    }

    @Test
    fun `deleteTaskEvent deletes existing event via ContentUris`() = runTest {
        val taskId = "task-delete-99"
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(4444L, "neuroflow://task/$taskId"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val deleteUriSlot = slot<Uri>()
        every {
            contentResolver.delete(capture(deleteUriSlot), any(), any())
        } returns 1

        val deleted = dataSource.deleteTaskEvent(taskId)
        deleted shouldBe true

        deleteUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 4444L)
    }

    @Test
    fun `handleTaskStateChanged updates completed task with checkmark flow title`() = runTest {
        val completedTask = TaskEntity(
            id = "task-done-1",
            title = "Finalize Presentation",
            status = TaskStatus.COMPLETED,
            scheduledDate = 1770000000000L
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, 700))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(3333L, "neuroflow://task/task-done-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.update(any(), capture(valuesSlot), any(), any())
        } returns 1

        val result = dataSource.handleTaskStateChanged(completedTask)
        result shouldBe true

        valuesSlot.captured.getAsString(CalendarContract.Events.TITLE) shouldBe "✓ [Flow] Finalize Presentation"
    }

    @Test
    fun `handleTaskStateChanged deletes task event on ARCHIVED`() = runTest {
        val archivedTask = TaskEntity(
            id = "task-archived-1",
            title = "Old Idea",
            status = TaskStatus.ARCHIVED
        )

        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(2222L, "neuroflow://task/task-archived-1"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        every { contentResolver.delete(any(), any(), any()) } returns 1

        val result = dataSource.handleTaskStateChanged(archivedTask)
        result shouldBe true

        verify { contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 2222L), any(), any()) }
    }

    @Test
    fun `queryInstances loop-prevention filters out self-authored neuroFlow blocks`() = runTest {
        val cursor = MatrixCursor(arrayOf(
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
        ))

        val start = 1770000000000L
        val end = start + 3600_000L

        // Row 1: External busy meeting (should be included)
        cursor.addRow(arrayOf(
            start,
            end,
            "External Sync",
            "Important team sync",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "colleague@corp.com",
            "",
            ""
        ))

        // Row 2: Self-authored neuroFlow block via customAppPackage (must be filtered out)
        cursor.addRow(arrayOf(
            start + 4000_000L,
            start + 5000_000L,
            "Focus Block",
            "Deep focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            CalendarSyncDataSource.NEUROFLOW_PACKAGE_ID,
            "neuroflow://task/task-abc"
        ))

        // Row 3: Self-authored neuroFlow block via [Flow] title prefix (must be filtered out)
        cursor.addRow(arrayOf(
            start + 6000_000L,
            start + 7000_000L,
            "[Flow] Frog task",
            "Focus",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "",
            "",
            ""
        ))

        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } returns cursor

        val windows = dataSource.queryInstances(start, start + 10_000_000L)
        windows.size shouldBe 1
        windows[0].startMillis shouldBe start
        windows[0].endMillis shouldBe end
    }

    @Test
    fun `queryInstances filters out all-day events`() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI
        ))

        val start = 1770000000000L
        val end = start + 3600_000L

        // Row 1: All-Day event (should be filtered out even if AVAILABILITY_BUSY)
        cursor.addRow(arrayOf(
            start,
            start + 86_400_000L,
            "National Holiday",
            "Public holiday banner",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "system@calendar.google.com",
            1, // ALL_DAY = 1
            "",
            ""
        ))

        // Row 2: Standard meeting (ALL_DAY = 0, should be returned)
        cursor.addRow(arrayOf(
            start,
            end,
            "Sprint Planning",
            "Bi-weekly sprint planning",
            CalendarContract.Events.AVAILABILITY_BUSY,
            CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED,
            CalendarContract.Events.STATUS_CONFIRMED,
            "lead@corp.com",
            0, // ALL_DAY = 0
            "",
            ""
        ))

        every {
            contentResolver.query(any(), any(), any(), any(), any())
        } returns cursor

        val windows = dataSource.queryInstances(start, start + 10_000_000L)
        windows.size shouldBe 1
        windows[0].startMillis shouldBe start
        windows[0].endMillis shouldBe end
    }

    @Test
    fun `handleTaskStateChanged upserts recurring daily frog habit anchored on habitDate at 9am`() = runTest {
        val baseDate = 1770000000000L
        val habitTask = TaskEntity(
            id = "habit-frog-1",
            title = "Morning Meditation",
            recurrence = Recurrence.DAILY,
            habitDate = baseDate,
            scheduledDate = null,
            scheduledTime = null,
            isFrog = true,
            status = TaskStatus.ACTIVE
        )

        // Mock primary calendar
        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        // No existing event
        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/7711")

        val result = dataSource.handleTaskStateChanged(habitTask)
        result shouldBe true

        val captured = valuesSlot.captured
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "🎯 [Flow] Morning Meditation"
        val expectedStart = startOfLocalDay(baseDate) + 9 * 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
        captured.getAsInteger(CalendarContract.Events.AVAILABILITY) shouldBe CalendarContract.Events.AVAILABILITY_BUSY
    }

    @Test
    fun `handleTaskStateChanged upserts recurring locked habit with timed offset`() = runTest {
        val baseDate = 1770000000000L
        val timedHabit = TaskEntity(
            id = "habit-locked-2",
            title = "Team Standup",
            recurrence = Recurrence.DAILY,
            habitDate = baseDate,
            scheduledDate = null,
            scheduledTime = 14 * 3600_000L, // 2:00 PM
            isScheduleLocked = true,
            status = TaskStatus.ACTIVE
        )

        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(55L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/7712")

        val result = dataSource.handleTaskStateChanged(timedHabit)
        result shouldBe true

        val captured = valuesSlot.captured
        captured.getAsString(CalendarContract.Events.TITLE) shouldBe "[Flow] Team Standup"
        val expectedStart = startOfLocalDay(baseDate) + 14 * 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
    }

    @Test
    fun `upsertTaskEvent updates existing event even when findPrimaryWritableCalendarId is null`() = runTest {
        val task = TaskEntity(
            id = "task-existing-99",
            title = "Important Review",
            status = TaskStatus.COMPLETED,
            scheduledDate = 1770000000000L
        )

        // Mock NO writable primary calendar found
        val emptyCalCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns emptyCalCursor

        // But event exists in calendar provider
        val existingEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        existingEventCursor.addRow(arrayOf(9999L, "neuroflow://task/task-existing-99"))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns existingEventCursor

        val valuesSlot = slot<ContentValues>()
        val updateUriSlot = slot<Uri>()
        every {
            contentResolver.update(capture(updateUriSlot), capture(valuesSlot), any(), any())
        } returns 1

        val resultId = dataSource.upsertTaskEvent(task)
        resultId shouldBe 9999L

        updateUriSlot.captured shouldBe ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 9999L)
        valuesSlot.captured.containsKey(CalendarContract.Events.CALENDAR_ID) shouldBe false
        valuesSlot.captured.getAsString(CalendarContract.Events.TITLE) shouldBe "✓ [Flow] Important Review"
        valuesSlot.captured.getAsInteger(CalendarContract.Events.AVAILABILITY) shouldBe CalendarContract.Events.AVAILABILITY_FREE
    }

    @Test
    fun `BUG-301 upsertTaskEvent anchors dateless timed task to effective date and uses TaskBlockPlan duration`() = runTest {
        val futureDate = 1775000000000L
        val timedTask = TaskEntity(
            id = "task-dateless-timed-301",
            title = "Crucial Future Meeting",
            scheduledDate = null,
            deadlineDate = futureDate,
            scheduledTime = 14 * 3600_000L, // 2:00 PM
            effortScore = 85, // TaskBlockPlan maps >=80 to 60m
            estimatedDurationMinutes = 0, // Untimed
            isFrog = true
        )

        // Mock primary calendar
        val calCursor = MatrixCursor(arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        ))
        calCursor.addRow(arrayOf(77L, 1, CalendarContract.Calendars.CAL_ACCESS_OWNER))
        every {
            contentResolver.query(CalendarContract.Calendars.CONTENT_URI, any(), any(), any(), any())
        } returns calCursor

        val emptyEventCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns emptyEventCursor

        val valuesSlot = slot<ContentValues>()
        every {
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/events/30101")

        val resultId = dataSource.upsertTaskEvent(timedTask)
        resultId shouldBe 30101L

        val captured = valuesSlot.captured
        val expectedStart = startOfLocalDay(futureDate) + 14 * 3600_000L
        captured.getAsLong(CalendarContract.Events.DTSTART) shouldBe expectedStart
        // EffortScore 85 -> 60 minutes via TaskBlockPlan.plannedMinutes
        captured.getAsLong(CalendarContract.Events.DTEND) shouldBe expectedStart + 60 * 60_000L
        captured.getAsString(CalendarContract.Events.DESCRIPTION) shouldBe "[neuroflow] Managed focus block\n[neuroflow:task:${timedTask.id}]"
    }

    @Test
    fun `BUG-336 findEventIdByTaskId falls back to DESCRIPTION marker when custom app columns are stripped`() = runTest {
        val taskId = "task-cloud-synced-123"

        // First query (by CUSTOM_APP_PACKAGE & CUSTOM_APP_URI) returns empty because Google Calendar Sync wiped custom columns
        val emptyCustomCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))

        // Second query (by DESCRIPTION LIKE '%[neuroflow:task:$taskId]%') finds the event
        val descCursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CUSTOM_APP_URI
        ))
        descCursor.addRow(arrayOf(445566L, null))

        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                match { it.contains(CalendarContract.Events.CUSTOM_APP_PACKAGE) },
                any(),
                any()
            )
        } returns emptyCustomCursor

        every {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                any(),
                match { it.contains(CalendarContract.Events.DESCRIPTION) },
                match { it.firstOrNull()?.contains(taskId) == true },
                any()
            )
        } returns descCursor

        val eventId = dataSource.findEventIdByTaskId(taskId)
        eventId shouldBe 445566L
    }
}
