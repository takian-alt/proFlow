package com.neuroflow.app.data.calendar

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.provider.CalendarContract
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class CalendarInboundSyncTest {
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
            override fun getContentResolver(): ContentResolver = this@CalendarInboundSyncTest.contentResolver
        }
        dataSource = CalendarSyncDataSource(testContext)
    }

    @Test
    fun testDetectExternalModifications_returnsEmptyWhenNoEvents() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DELETED,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.DESCRIPTION
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns cursor

        val deltas = dataSource.detectExternalModifications(sinceMillis = System.currentTimeMillis() - 3600_000L)
        assertNotNull(deltas)
        assertTrue(deltas.isEmpty())
    }

    @Test
    fun testDetectExternalModifications_detectsMovedEvent() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DELETED,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.DESCRIPTION
        ))
        cursor.addRow(arrayOf(
            101L,
            1731600000000L,
            1731603600000L,
            0,
            "com.neuroflow.app",
            "neuroflow://task/task-123",
            "[neuroflow:task:task-123]"
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns cursor

        val deltas = dataSource.detectExternalModifications(sinceMillis = 0L)
        assertEquals(1, deltas.size)
        val delta = deltas.first()
        assertEquals("task-123", delta.taskId)
        assertEquals(ExternalDeltaType.MOVED, delta.type)
        assertEquals(1731600000000L, delta.newStartMillis)
        assertEquals(1731603600000L, delta.newEndMillis)
    }

    @Test
    fun testDetectExternalModifications_detectsDeletedEvent() = runTest {
        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DELETED,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.DESCRIPTION
        ))
        cursor.addRow(arrayOf(
            102L,
            1731600000000L,
            1731603600000L,
            1, // DELETED
            "com.neuroflow.app",
            "neuroflow://task/task-456",
            "[neuroflow:task:task-456]"
        ))
        every {
            contentResolver.query(CalendarContract.Events.CONTENT_URI, any(), any(), any(), any())
        } returns cursor

        val deltas = dataSource.detectExternalModifications(sinceMillis = 0L)
        assertEquals(1, deltas.size)
        val delta = deltas.first()
        assertEquals("task-456", delta.taskId)
        assertEquals(ExternalDeltaType.DELETED, delta.type)
    }
}
