package com.neuroflow.app.feature.hyperfocus.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.gate.GateTask
import com.neuroflow.app.feature.hyperfocus.gate.GateTaskEngine
import kotlinx.coroutines.delay

/**
 * The exit gate. It costs the same attention as opening a hidden app, because ending the session
 * removes every block on the device at once. `onSubmit` returns whether the attempt ended the
 * session, so a mismatch is tallied on the click that failed it.
 */
@Composable
fun EndSessionGateScreen(
    task: GateTask,
    onSubmit: (answer: List<String>, reason: String, mismatchCount: Int, elapsedMillis: Long) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answers by remember(task) { mutableStateOf(task.expected.map { "" }) }
    var mismatches by remember(task) { mutableIntStateOf(0) }
    var reason by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(1_000L)
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
        Text(
            text = "Before you end it",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ending the session lifts every block on this device, now and for the rest of " +
                "the time you set aside.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp
        )

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            task.expected.forEachIndexed { index, _ ->
                OutlinedTextField(
                    value = answers.getOrElse(index) { "" },
                    onValueChange = { value ->
                        answers = answers.mapIndexed { i, old -> if (i == index) value else old }
                    },
                    label = { Text("${index + 1}") },
                    singleLine = task.expected.size > 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index == task.expected.lastIndex) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (reasonValid(reason) && elapsed >= GateTaskEngine.MIN_ELAPSED_MILLIS) {
                                if (!onSubmit(answers, reason, mismatches, elapsed)) mismatches += 1
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Why are you ending the session?") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mismatches: $mismatches/${GateTaskEngine.MAX_MISMATCHES}",
                color = if (mismatches >= GateTaskEngine.MAX_MISMATCHES) Color(0xFFFFB74D) else Color(0xFFAAAAAA),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    holdSeconds > 0 -> "Hold for $holdSeconds more second${if (holdSeconds == 1L) "" else "s"}"
                    !reasonValid(reason) -> "Write a reason to continue"
                    else -> "Ready"
                },
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = {
                if (!onSubmit(answers, reason, mismatches, elapsed)) mismatches += 1
            },
            enabled = reasonValid(reason) && elapsed >= GateTaskEngine.MIN_ELAPSED_MILLIS,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color(0xFF777777)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End the session")
        }

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keep it running")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun reasonValid(reason: String): Boolean =
    reason.trim().length >= GateTaskEngine.MIN_REASON_CHARS
