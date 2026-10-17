package com.neuroflow.app.presentation.v2.focus.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neuroflow.app.domain.engine.WoopEngine
import com.neuroflow.app.presentation.v2.theme.NeuroFlowThemeV2Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WoopPromptSheet(
    onSubmit: (wish: String, outcome: String, obstacle: String, plan: String) -> Unit,
    onSkip: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var wish by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var obstacle by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }
    var planEdited by remember { mutableStateOf(false) }

    // Auto-populate plan from obstacle unless user has manually edited it
    LaunchedEffect(obstacle) {
        if (!planEdited) {
            plan = WoopEngine.generateIfThenPlan(obstacle)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "WOOP It 🎯",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Wish · Outcome · Obstacle · Plan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = wish,
                onValueChange = { wish = it },
                label = { Text("Wish: What do you want to achieve?") },
                placeholder = { Text("e.g. Finish the quarterly report draft") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    focusedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigo
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = outcome,
                onValueChange = { outcome = it },
                label = { Text("Outcome: What is the best result / feeling?") },
                placeholder = { Text("e.g. Relief, clear weekend ahead") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    focusedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigo
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = obstacle,
                onValueChange = {
                    obstacle = it
                    planEdited = false
                },
                label = { Text("Obstacle: What internal hurdle might stop you?") },
                placeholder = { Text("e.g. Checking phone when stuck, fatigue") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    focusedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigo
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = plan,
                onValueChange = {
                    plan = it
                    planEdited = true
                },
                label = { Text("Plan: If [obstacle], then I will [action]") },
                placeholder = { Text("e.g. If I want to check my phone, then I will take 3 deep breaths") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeuroFlowThemeV2Colors.ElectricIndigo,
                    focusedLabelColor = NeuroFlowThemeV2Colors.ElectricIndigo
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
                Button(
                    onClick = { onSubmit(wish, outcome, obstacle, plan) },
                    enabled = wish.isNotBlank() && obstacle.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuroFlowThemeV2Colors.ElectricIndigo)
                ) {
                    Text("Save & Focus")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
