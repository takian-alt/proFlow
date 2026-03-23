package com.neuroflow.app.presentation.launcher.hyperfocus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neuroflow.app.presentation.launcher.hyperfocus.HyperFocusViewModel

@Composable
fun PlanningPromptScreen(viewModel: HyperFocusViewModel) {
    val taskTitles = remember { mutableStateListOf("", "", "") }
    val extraTitles = remember { mutableStateListOf<String>() }

    val allMandatoryFilled = taskTitles.all { it.isNotBlank() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Plan Tomorrow's Tasks",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Complete 3 tasks to unlock your apps tomorrow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            taskTitles.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { taskTitles[index] = it },
                    label = { Text("Task ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            extraTitles.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { extraTitles[index] = it },
                    label = { Text("Task ${taskTitles.size + index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            TextButton(onClick = { extraTitles.add("") }) {
                Text("+ Add another task")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.completePlanning(taskTitles + extraTitles.filter { it.isNotBlank() })
                },
                enabled = allMandatoryFilled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Unlock Apps")
            }
        }
    }
}
