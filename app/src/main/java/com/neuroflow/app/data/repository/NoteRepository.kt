package com.neuroflow.app.data.repository

import com.neuroflow.app.data.local.dao.NoteDao
import com.neuroflow.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.observeAll()
    val noteCount: Flow<Int> = noteDao.observeNoteCount()

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return if (query.isBlank()) {
            noteDao.observeAll()
        } else {
            noteDao.searchNotes(query.trim())
        }
    }

    fun observeNoteById(id: String): Flow<NoteEntity?> = noteDao.observeById(id)

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getById(id)

    suspend fun saveNote(note: NoteEntity) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.insert(updatedNote)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.delete(note)
    }

    suspend fun deleteNoteById(id: String) {
        noteDao.deleteById(id)
    }

    suspend fun togglePin(note: NoteEntity) {
        val updated = note.copy(
            isPinned = !note.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.update(updated)
    }

    suspend fun updateColor(note: NoteEntity, colorHex: String) {
        val updated = note.copy(
            colorHex = colorHex,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.update(updated)
    }

    suspend fun getAllNotes(): List<NoteEntity> = noteDao.getAllNotes()
}
