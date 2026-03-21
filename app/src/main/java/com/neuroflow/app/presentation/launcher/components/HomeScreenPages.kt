package com.neuroflow.app.presentation.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuroflow.app.presentation.launcher.LauncherViewModel
import com.neuroflow.app.presentation.launcher.data.AppInfo
import com.neuroflow.app.presentation.launcher.data.FolderDefinition
import com.neuroflow.app.presentation.launcher.folder.FolderIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Left page - Reserved for future feature (Phase 3).
 * Currently shows empty placeholder.
 *
 * @param modifier Modifier for the page container
 */
@Composable
fun LeftPage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Left Page\n(Reserved)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Right page - Quick stats panel (Phase 3).
 * Displays today's progress, streak, and task breakdown.
 *
 * @param viewModel LauncherViewModel for analytics data
 * @param modifier Modifier for the page container
 */
@Composable
fun RightPage(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTasks by viewModel.allActiveTasks.collectAsStateWithLifecycle()

    // Calculate stats from tasks
    val completedToday = remember(allTasks) {
        allTasks.count { it.status == com.neuroflow.app.domain.model.TaskStatus.COMPLETED }
    }

    val tasksByQuadrant = remember(allTasks) {
        allTasks.groupBy { it.quadrant }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Today's Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

        Divider()

        // Stats
        StatsRow(icon = "✅", label = "Tasks done", value = "$completedToday")
        StatsRow(icon = "📋", label = "Active tasks", value = "${allTasks.size}")
        StatsRow(icon = "⏱", label = "Minutes focused", value = "0") // TODO: Wire to session data
        StatsRow(icon = "🔥", label = "Streak", value = "0 days") // TODO: Wire to analytics

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tasks by Quadrant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Quadrant breakdown
        val doFirst = tasksByQuadrant[com.neuroflow.app.domain.model.Quadrant.DO_FIRST]?.size ?: 0
        val schedule = tasksByQuadrant[com.neuroflow.app.domain.model.Quadrant.SCHEDULE]?.size ?: 0
        val delegate = tasksByQuadrant[com.neuroflow.app.domain.model.Quadrant.DELEGATE]?.size ?: 0

        QuadrantRow(label = "DO FIRST", completed = 0, total = doFirst)
        QuadrantRow(label = "SCHEDULE", completed = 0, total = schedule)
        QuadrantRow(label = "DELEGATE", completed = 0, total = delegate)

        Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Open analytics in main app
                    val intent = android.content.Intent(context, com.neuroflow.app.MainActivity::class.java).apply {
                        action = "com.procus.ACTION_OPEN_ANALYTICS"
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Analytics")
            }
        }
    }
}

@Composable
private fun StatsRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun QuadrantRow(
    label: String,
    completed: Int,
    total: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$completed/$total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = if (total > 0) completed.toFloat() / total else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}
