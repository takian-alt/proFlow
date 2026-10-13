package com.neuroflow.app.presentation.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.analytics.AnalyticsMode
import com.neuroflow.app.presentation.analytics.AnalyticsScreen
import com.neuroflow.app.presentation.history.HistoryScreen

enum class ProgressTab(val title: String) {
    OVERVIEW("Overview"),
    HISTORY("History"),
    INSIGHTS("Insights")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onOpenMoreMenu: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val summary by viewModel.summaryState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(ProgressTab.OVERVIEW) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenMoreMenu) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == ProgressTab.OVERVIEW) {
                WeeklyProgressSummary(
                    completedTasks = summary.completedThisWeek,
                    focusMinutes = summary.focusMinutesThisWeek,
                    deltaTasks = summary.deltaTasksVsLastWeek
                )
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                ProgressTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ProgressTab.entries.size
                        ),
                        onClick = { selectedTab = tab },
                        selected = selectedTab == tab,
                        label = { Text(tab.title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    ProgressTab.OVERVIEW -> {
                        AnalyticsScreen(hideTopBar = true, mode = AnalyticsMode.OVERVIEW)
                    }
                    ProgressTab.INSIGHTS -> {
                        AnalyticsScreen(hideTopBar = true, mode = AnalyticsMode.INSIGHTS)
                    }
                    ProgressTab.HISTORY -> {
                        HistoryScreen(hideTopBar = true)
                    }
                }
            }

        }
    }
}

@Composable
private fun WeeklyProgressSummary(
    completedTasks: Int,
    focusMinutes: Int,
    deltaTasks: Int
) {
    val comparison = when {
        deltaTasks > 0 -> "+$deltaTasks vs last week"
        deltaTasks < 0 -> "$deltaTasks vs last week"
        else -> "Same as last week"
    }
    val comparisonColor = when {
        deltaTasks > 0 -> MaterialTheme.colorScheme.primary
        deltaTasks < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = comparison,
                    style = MaterialTheme.typography.labelSmall,
                    color = comparisonColor,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProgressMetric(
                    value = completedTasks.toString(),
                    label = "Tasks completed",
                    modifier = Modifier.weight(1f)
                )
                ProgressMetric(
                    value = "$focusMinutes min",
                    label = "Focus time",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
