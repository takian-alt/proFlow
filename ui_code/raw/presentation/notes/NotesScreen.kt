package com.neuroflow.app.presentation.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    if (uiState.isEditing && uiState.editingNote != null) {
        NoteEditorScreen(
            note = uiState.editingNote!!,
            onDismiss = { viewModel.closeEditor() },
            onSave = { title, content, colorId, isPinned ->
                viewModel.saveNote(title, content, colorId, isPinned)
            },
            onDelete = {
                viewModel.deleteNote(uiState.editingNote!!)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search notes...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Notes",
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.totalNoteCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        "${uiState.totalNoteCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search Notes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openNewNote() },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Note") },
                text = { Text("New Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        if (uiState.notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "No notes match '${uiState.searchQuery}'"
                        else "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "Try searching for different keywords"
                        else "Tap '+ New Note' to capture your ideas, thoughts, and reflections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val pinnedNotes = remember(uiState.notes) { uiState.notes.filter { it.isPinned } }
            val otherNotes = remember(uiState.notes) { uiState.notes.filterNot { it.isPinned } }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            "PINNED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isDark = isDark,
                            onClick = { viewModel.openEditNote(note) },
                            onExtractTask = { viewModel.createTaskFromNote(note) }
                        )
                    }
                }

                if (otherNotes.isNotEmpty()) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                "OTHERS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isDark = isDark,
                            onClick = { viewModel.openEditNote(note) },
                            onExtractTask = { viewModel.createTaskFromNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    isDark: Boolean,
    onClick: () -> Unit,
    onExtractTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawCardBg = NoteColors.getColor(note.colorHex, isDark)
    val accentColor = if (note.colorHex == "DEFAULT" || isDark) {
        com.neuroflow.app.presentation.v2.theme.NeuroFlowPaletteV2.ReboundCyan
    } else {
        rawCardBg
    }
    val formattedDate = remember(note.updatedAt) {
        val nowCal = java.util.Calendar.getInstance()
        val noteCal = java.util.Calendar.getInstance().apply { timeInMillis = note.updatedAt }
        val isSameDay = nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == noteCal.get(java.util.Calendar.DAY_OF_YEAR)
        val isYesterday = nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) - noteCal.get(java.util.Calendar.DAY_OF_YEAR) == 1
        when {
            isSameDay -> "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
            isYesterday -> "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(note.updatedAt))
            nowCal.get(java.util.Calendar.YEAR) == noteCal.get(java.util.Calendar.YEAR) ->
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updatedAt))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
        }
    }

    com.neuroflow.app.presentation.v2.common.UpgradedNoteCardV2(
        title = note.title,
        contentPreview = note.content,
        noteAccentColor = accentColor,
        updatedAtFormatted = formattedDate,
        onNoteClick = onClick,
        onExtractTaskWithAiClick = onExtractTask,
        modifier = modifier
    )
}
