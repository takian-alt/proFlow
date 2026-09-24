package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.entity.NoteEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class NoteRepositoryTest : StringSpec({

    val noteDao = mockk<NoteDao>(relaxed = true)
    val repository = NoteRepository(noteDao)

    val sampleNote = NoteEntity(
        id = "test-note-1",
        title = "Brainstorming Ideas",
        content = "1. Minimal UI\n2. Note Taking feature\n3. On Hold / Planning",
        colorHex = "BUTTER",
        isPinned = false,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    "searchNotes calls observeAll when query is blank" {
        every { noteDao.observeAll() } returns flowOf(listOf(sampleNote))

        val result = repository.searchNotes("").first()
        result shouldBe listOf(sampleNote)
    }

    "searchNotes queries noteDao when query is non-blank" {
        every { noteDao.searchNotes("Minimal") } returns flowOf(listOf(sampleNote))

        val result = repository.searchNotes("  Minimal  ").first()
        result shouldBe listOf(sampleNote)
    }

    "saveNote updates timestamp and inserts to dao" {
        coEvery { noteDao.insert(any()) } returns Unit

        repository.saveNote(sampleNote)

        coVerify {
            noteDao.insert(match {
                it.id == sampleNote.id && it.title == sampleNote.title && it.updatedAt >= 1000L
            })
        }
    }

    "togglePin flips isPinned flag and updates note" {
        coEvery { noteDao.update(any()) } returns Unit

        repository.togglePin(sampleNote)

        coVerify {
            noteDao.update(match {
                it.id == sampleNote.id && it.isPinned
            })
        }
    }

    "updateColor changes colorHex and updates note" {
        coEvery { noteDao.update(any()) } returns Unit

        repository.updateColor(sampleNote, "MINT")

        coVerify {
            noteDao.update(match {
                it.id == sampleNote.id && it.colorHex == "MINT"
            })
        }
    }

    "deleteNote delegates to dao" {
        coEvery { noteDao.delete(sampleNote) } returns Unit

        repository.deleteNote(sampleNote)

        coVerify {
            noteDao.delete(sampleNote)
        }
    }
})
