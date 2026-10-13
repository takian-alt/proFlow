package com.neuroflow.app.feature.hyperfocus.gate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import kotlinx.coroutines.delay

/**
 * The cognitive gate. Nothing here is a puzzle the user can fail: mismatches are tallied, three of
 * them auto-pass, and the only hard floor is twelve seconds of attention plus a written reason.
 */
@Composable
fun GateTaskScreen(
    onPassed: () -> Unit,
    onBack: () -> Unit,
    viewModel: HyperFocusViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val task by viewModel.gateTask.collectAsStateWithLifecycle()
    val passed by viewModel.gatePassed.collectAsStateWithLifecycle()

    // The engine's own auto-pass threshold, read from the single instance this ViewModel checks
    // answers against. A local copy of the number would let the tally's ceiling drift from the
    // condition that actually opens the gate.
    val maxMismatches = GateTaskEngine.MAX_MISMATCHES

    var answers by remember(task) { mutableStateOf(task.expected.map { "" }) }
    var mismatches by remember(task) { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(1000L)
        }
    }

    LaunchedEffect(passed) {
        if (passed) {
            viewModel.consumeGatePass()
            onPassed()
        }
    }

    val holdSeconds = ((GateTaskEngine.MIN_ELAPSED_MILLIS - elapsed) / 1000).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Before you open it",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF333333),
                    contentColor = Color.White
                )
            ) {
                Text("Back", fontSize = 14.sp)
            }
        }

        Text(
            text = task.prompt,
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp))
                .padding(16.dp)
        )

        if (task.expected.size == 1) {
            OutlinedTextField(
                value = answers.firstOrNull().orEmpty(),
                onValueChange = { answers = listOf(it) },
                label = { Text("Your answer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                task.expected.forEachIndexed { index, _ ->
                    OutlinedTextField(
                        value = answers.getOrElse(index) { "" },
                        onValueChange = { value ->
                            answers = answers.mapIndexed { i, old ->
                                if (i == index) value else old
                            }
                        },
                        label = { Text("${index + 1}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Why are you opening this app?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (viewModel.gateEngine.allowsSubmit(elapsedMillis = elapsed, reason = reason)) {
                        viewModel.submitGateAnswer(answers, reason, mismatches, elapsed)
                    }
                }
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A tally, not a bar: filling it never locks the control, because the submit guard reads
            // only elapsed time and the reason. Mismatches open the gate early through
            // GateTaskEngine.shouldAutoPass, which is the ViewModel's decision, not this one.
            Text(
                text = "Mismatches: $mismatches/$maxMismatches",
                color = if (mismatches >= maxMismatches) Color(0xFFFFB74D) else Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
            if (holdSeconds > 0) {
                Text(
                    text = "Hold for $holdSeconds more second${if (holdSeconds == 1L) "" else "s"}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = if (reason.trim().length < GateTaskEngine.MIN_REASON_CHARS)
                        "Write a reason to continue" else "Ready",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
        }

        Button(
            onClick = {
                val submitted = answers
                if (!viewModel.submitGateAnswer(submitted, reason, mismatches, elapsed)) {
                    mismatches += 1
                }
            },
            enabled = viewModel.gateEngine.allowsSubmit(elapsedMillis = elapsed, reason = reason),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color(0xFF777777)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
