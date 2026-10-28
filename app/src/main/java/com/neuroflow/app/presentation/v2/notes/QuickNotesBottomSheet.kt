package com.neuroflow.app.presentation.v2.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.data.local.entity.NoteEntity
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotesBottomSheet(
    notes: List<NoteEntity>,
    onSaveNote: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenFullNotes: () -> Unit = {},
    onCreateTask: ((title: String, description: String) -> Unit)? = null,
    onCreateTaskSynthesis: ((synthesis: com.neuroflow.app.domain.service.QuickNoteSynthesis, rawContent: String) -> Unit)? = null,
    onSynthesizeNote: (suspend (String) -> com.neuroflow.app.domain.service.QuickNoteSynthesis)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var synthesis by remember { mutableStateOf<com.neuroflow.app.domain.service.QuickNoteSynthesis?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(content, title) {
        val trimmed = content.trim()
        if (trimmed.length >= 5) {
            kotlinx.coroutines.delay(600L)
            val full = if (title.isNotBlank()) "$title: $content" else content
            try {
                if (onSynthesizeNote != null) {
                    synthesis = onSynthesizeNote(full)
                } else {
                    val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(full)
                    synthesis = com.neuroflow.app.domain.service.QuickNoteSynthesis(
                        extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { content.take(35) } },
                        suggestedCategory = inferred.taskType.name,
                        estimatedMinutes = inferred.durationMinutes,
                        tags = inferred.contextTags,
                        firstActionStep = "Review note: ${content.take(20)}..."
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(full)
                synthesis = com.neuroflow.app.domain.service.QuickNoteSynthesis(
                    extractedTaskTitle = inferred.sanitizedTitle.ifBlank { title.ifBlank { content.take(35) } },
                    suggestedCategory = inferred.taskType.name,
                    estimatedMinutes = inferred.durationMinutes,
                    tags = inferred.contextTags,
                    firstActionStep = "Review note: ${content.take(20)}..."
                )
            }
        } else {
            synthesis = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NeuroFlowThemeV2Colors.SlateSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NeuroFlowThemeV2Colors.SlateBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "📝 Scratchpad & Working Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeuroFlowThemeV2Colors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Jot down ideas, research findings, and brain dumps without distraction",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeuroFlowThemeV2Colors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onDismiss()
                            onOpenFullNotes()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Full Notes Workspace",
                            tint = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close sheet",
                            tint = NeuroFlowThemeV2Colors.TextSecondary
                        )
                    }
                }
            }

            // Quick Composer Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Note Title (Optional)...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                        )
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Write your thoughts or draft actions here...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                            unfocusedBorderColor = NeuroFlowThemeV2Colors.SlateBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (content.isNotBlank()) {
                                    onSaveNote(title, content)
                                    title = ""
                                    content = ""
                                }
                            },
                            enabled = content.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Note", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Note AI Task Extraction Preview
            synthesis?.let { syn ->
                com.neuroflow.app.presentation.v2.settings.QuickNoteAiSynthesizerCard(
                    synthesis = syn,
                    onConvertToTaskClick = { finalSyn ->
                        val finalTaskTitle = title.trim().ifBlank { finalSyn.extractedTaskTitle }
                        val taskSynthesis = if (finalTaskTitle != finalSyn.extractedTaskTitle) finalSyn.copy(extractedTaskTitle = finalTaskTitle) else finalSyn
                        if (onCreateTaskSynthesis != null) {
                            onCreateTaskSynthesis(taskSynthesis, content)
                            onSaveNote(finalTaskTitle, content)
                        } else if (onCreateTask != null) {
                            onCreateTask(finalTaskTitle, content)
                            onSaveNote(finalTaskTitle, content)
                        } else {
                            onSaveNote(finalTaskTitle, content)
                        }
                        title = ""
                        content = ""
                        onDismiss()
                    }
                )
            }

            // Recent Notes List
            if (notes.isNotEmpty()) {
                Text(
                    text = "Recent Notes (${notes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val dateFormatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                            .format(Date(note.updatedAt))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenFullNotes()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled Note" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeuroFlowThemeV2Colors.TextPrimary,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (note.isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = NeuroFlowThemeV2Colors.WarmAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeuroFlowThemeV2Colors.TextSecondary,
                                    maxLines = 3
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeuroFlowThemeV2Colors.TextTertiary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (onCreateTaskSynthesis != null || onCreateTask != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            modifier = Modifier.clickable {
                                                val fullText = if (note.title.isNotBlank()) "${note.title}: ${note.content}" else note.content
                                                val inferred = com.neuroflow.app.domain.inference.TaskInferenceEngine.inferFromInput(fullText)
                                                val explicitTitle = note.title.trim().takeIf { it.isNotBlank() }
                                                val resolvedTaskTitle = explicitTitle ?: inferred.sanitizedTitle.ifBlank { note.content.take(35) }
                                                if (onCreateTaskSynthesis != null) {
                                                    val syn = com.neuroflow.app.domain.service.QuickNoteSynthesis(
                                                        extractedTaskTitle = resolvedTaskTitle,
                                                        suggestedCategory = inferred.taskType.name,
                                                        estimatedMinutes = inferred.durationMinutes,
                                                        tags = inferred.contextTags,
                                                        firstActionStep = "Review note: ${note.content.take(20)}..."
                                                    )
                                                    onCreateTaskSynthesis(syn, note.content)
                                                } else {
                                                    onCreateTask?.invoke(resolvedTaskTitle, note.content)
                                                }
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, NeuroFlowThemeV2Colors.ElectricIndigo.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚡ Convert to Task",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NeuroFlowThemeV2Colors.ElectricIndigoGlow,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
