package com.neuroflow.app.presentation.notes

import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.model.EnergyLevel
import com.neuroflow.app.domain.model.Priority
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    "BUG-148: createTaskFromNote extracts NLP metadata, writes to calendar sync, and emits userActionMessage" {
        runTest(testDispatcher) {
            val noteRepo = mockk<NoteRepository>(relaxed = true)
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val calendarSyncRepo = mockk<CalendarSyncRepository>(relaxed = true)

            every { noteRepo.searchNotes(any()) } returns MutableStateFlow(emptyList())
            every { noteRepo.noteCount } returns MutableStateFlow(0)

            val insertedSlot = slot<TaskEntity>()
            coEvery { taskRepo.insert(capture(insertedSlot)) } returns Unit
            coEvery { calendarSyncRepo.handleTaskStateChanged(any()) } returns true

            val viewModel = NotesViewModel(noteRepo, taskRepo, calendarSyncRepo)

            val testNote = NoteEntity(
                id = "note-nlp-1",
                title = "Prepare Budget",
                content = "Prepare quarterly financial deck ~45m #finance !urgent",
                colorHex = "DEFAULT",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val messages = mutableListOf<String>()
            val collectJob = launch {
                viewModel.userActionMessage.collect { messages.add(it) }
            }

            viewModel.createTaskFromNote(testNote)
            advanceUntilIdle()

            coVerify(exactly = 1) { taskRepo.insert(any()) }
            coVerify(exactly = 1) { calendarSyncRepo.handleTaskStateChanged(any()) }

            val inserted = insertedSlot.captured
            inserted.estimatedDurationMinutes shouldBe 45
            inserted.priority shouldBe Priority.HIGH
            inserted.tags shouldContain "finance"
            inserted.energyLevel shouldBe EnergyLevel.HIGH

            messages.size shouldBe 1
            messages.first() shouldContain "Created task 'Prepare"
            messages.first() shouldContain "⚡"

            collectJob.cancel()
        }
    }

    "BUG-184: saveNote deletes existing note by id when both title and content are blank" {
        runTest(testDispatcher) {
            val noteRepo = mockk<NoteRepository>(relaxed = true)
            val taskRepo = mockk<TaskRepository>(relaxed = true)
            val calendarSyncRepo = mockk<CalendarSyncRepository>(relaxed = true)

            every { noteRepo.searchNotes(any()) } returns MutableStateFlow(emptyList())
            every { noteRepo.noteCount } returns MutableStateFlow(1)
            coEvery { noteRepo.deleteNoteById(any()) } returns Unit

            val viewModel = NotesViewModel(noteRepo, taskRepo, calendarSyncRepo)

            val existingNote = NoteEntity(
                id = "note-to-clear-1",
                title = "Old Title",
                content = "Old Content",
                colorHex = "DEFAULT",
                createdAt = System.currentTimeMillis() - 10000L,
                updatedAt = System.currentTimeMillis() - 10000L
            )

            viewModel.openEditNote(existingNote)
            viewModel.saveNote(title = "   ", content = "", colorHex = "DEFAULT", isPinned = false)
            advanceUntilIdle()

            coVerify(exactly = 1) { noteRepo.deleteNoteById("note-to-clear-1") }
            viewModel.isEditing.value shouldBe false
            viewModel.editingNote.value shouldBe null
        }
    }
})
