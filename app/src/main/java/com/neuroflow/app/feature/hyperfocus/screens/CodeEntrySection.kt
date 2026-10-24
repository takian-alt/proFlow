package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.domain.UnlockResult
import kotlin.math.roundToInt

/**
 * A reward code and nothing else: no navigation, no route, no assumption about what a successful
 * entry unlocks. Under D3 a code buys a budget, so what happens next belongs to whoever embedded
 * this — the gateway spends it there and then, and nothing may offer it before the gate.
 */
@Composable
fun CodeEntrySection(
    viewModel: HyperFocusViewModel,
    onCodeAccepted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val lockoutSecondsRemaining by viewModel.lockoutSecondsRemaining.collectAsState()
    val submitResult by viewModel.submitCodeResult.collectAsState()

    val chars = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Shake animation offset
    val shakeOffset = remember { Animatable(0f) }

    val isLocked = lockoutSecondsRemaining != null
    val allFilled = chars.all { it.isNotEmpty() }
    val attemptsRemaining = (3 - prefs.wrongCodeAttempts).coerceAtLeast(0)

    val haptic = LocalHapticFeedback.current

    // React to submit result
    LaunchedEffect(submitResult) {
        when (submitResult) {
            is UnlockResult.Success -> {
                viewModel.clearSubmitCodeResult()
                onCodeAccepted()
            }
            is UnlockResult.InvalidCode -> {
                viewModel.clearSubmitCodeResult()
                // Shake the input boxes
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        0f at 0
                        -16f at 50
                        16f at 100
                        -12f at 150
                        12f at 200
                        -8f at 250
                        8f at 300
                        0f at 400
                    }
                )
                // Clear the entered chars
                for (i in chars.indices) chars[i] = ""
                focusRequesters[0].requestFocus()
            }
            is UnlockResult.Lockout, null -> { /* nothing */ }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Title
        Text(
            text = "Enter your unlock code",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Code boxes or lockout countdown
        if (isLocked) {
            val secs = lockoutSecondsRemaining!!
            Text(
                text = "Try again in ${secs / 60}:${String.format("%02d", secs % 60)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                chars.forEachIndexed { index, value ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (value.isNotEmpty()) 2.dp else 1.dp,
                            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.size(width = 42.dp, height = 54.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { newVal ->
                                val char = newVal.uppercase().lastOrNull()?.toString() ?: ""
                                chars[index] = char
                                if (char.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequesters[index])
                                .onKeyEvent { event ->
                                    if (event.key == Key.Backspace && chars[index].isEmpty() && index > 0) {
                                        focusRequesters[index - 1].requestFocus()
                                        chars[index - 1] = ""
                                        true
                                    } else false
                                },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    innerTextField()
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Attempts remaining
            Text(
                text = "$attemptsRemaining of 3 attempts remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Confirm button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.submitCode(chars.joinToString(""))
                // The caller reacts through onCodeAccepted, which the result effect above invokes
            },
            enabled = allFilled && !isLocked,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Confirm", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Help text
        Text(
            text = "Don't have a code? Complete a task to earn one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Request focus on first box when screen appears
    LaunchedEffect(Unit) {
        if (!isLocked) {
            focusRequesters[0].requestFocus()
        }
    }
}
