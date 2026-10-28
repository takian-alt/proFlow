package com.neuroflow.app.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflow.app.data.calendar.CalendarSyncRepository
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.data.local.entity.TaskEntity
import com.neuroflow.app.data.repository.NoteRepository
import com.neuroflow.app.data.repository.TaskRepository
import com.neuroflow.app.domain.inference.TaskInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val editingNote: NoteEntity? = null,
    val isEditing: Boolean = false,
    val totalNoteCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val calendarSyncRepository: CalendarSyncRepository? = null
) : ViewModel() {

    private val _userActionMessage = MutableSharedFlow<String>()
    val userActionMessage: SharedFlow<String> = _userActionMessage.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    val uiState: StateFlow<NotesUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            noteRepository.searchNotes(query)
        },
        _searchQuery,
        _editingNote,
        _isEditing,
        noteRepository.noteCount
    ) { notes, query, editing, isEdit, count ->
        NotesUiState(
            notes = notes,
            searchQuery = query,
            editingNote = editing,
            isEditing = isEdit,
            totalNoteCount = count
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openNewNote() {
        _editingNote.value = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = "",
            content = "",
            colorHex = "DEFAULT",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _isEditing.value = true
    }

    fun openEditNote(note: NoteEntity) {
        _editingNote.value = note
        _isEditing.value = true
    }

    fun closeEditor() {
        _editingNote.value = null
        _isEditing.value = false
    }

    fun saveNote(title: String, content: String, colorHex: String, isPinned: Boolean) {
        val current = _editingNote.value ?: return
        if (title.isBlank() && content.isBlank()) {
            viewModelScope.launch {
                noteRepository.deleteNoteById(current.id)
                closeEditor()
            }
            return
        }
        viewModelScope.launch {
            val updated = current.copy(
                title = title.trim(),
                content = content.trim(),
                colorHex = colorHex,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.saveNote(updated)
            closeEditor()
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.togglePin(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            if (_editingNote.value?.id == note.id) {
                closeEditor()
            }
        }
    }

    fun updateColor(note: NoteEntity, colorHex: String) {
        viewModelScope.launch {
            noteRepository.updateColor(note, colorHex)
        }
    }

    fun createTaskFromNote(note: NoteEntity) {
        viewModelScope.launch {
            val fullText = if (note.title.isNotBlank()) "${note.title}: ${note.content}" else note.content
            val inferred = TaskInferenceEngine.inferFromInput(fullText)
            val now = System.currentTimeMillis()
            val tagsCombined = inferred.contextTags.distinct().filter { it.isNotBlank() }.joinToString(",")
            val taskTitle = inferred.sanitizedTitle.ifBlank { note.title.ifBlank { note.content.take(40).trim() } }.ifBlank { "Task from Note" }
            val newTask = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = taskTitle,
                description = note.content.trim(),
                estimatedDurationMinutes = inferred.durationMinutes,
                energyLevel = inferred.energyDemand,
                taskType = inferred.taskType,
                tags = tagsCombined,
                isFrog = inferred.isFrog,
                quadrant = inferred.quadrant,
                priority = inferred.priority,
                deadlineDate = inferred.deadlineDate,
                deadlineTime = inferred.deadlineTime,
                scheduledDate = inferred.scheduledDate,
                scheduledTime = inferred.scheduledTime,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insert(newTask)
            calendarSyncRepository?.handleTaskStateChanged(newTask)
            _userActionMessage.emit("Created task '${newTask.title.take(24)}' ⚡")
        }
    }
}
