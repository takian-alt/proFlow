package com.neuroflow.app.feature.hyperfocus.gate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.screens.CodeEntrySection
import com.neuroflow.app.feature.hyperfocus.screens.EmergencyControl

/**
 * The gateway: everything the home screen hides, listed once per ViewModel rather than once per
 * tick, because re-enumerating the launcher's app list is a cross-process call.
 */
@Composable
fun GatewayScreen(
    onLaunch: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HyperFocusViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val emergencyAvailable by viewModel.emergencyAvailable.collectAsStateWithLifecycle()
    val budgetSeconds by viewModel.gateBudgetSecondsRemaining.collectAsStateWithLifecycle()
    val prefs by viewModel.hyperFocusPrefs.collectAsStateWithLifecycle()
    val activePackage = prefs.activePassPackage

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
                text = "Apps behind the gate",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF80CBC4))
            ) {
                Text("Home", fontSize = 14.sp)
            }
        }

        Text(
            text = gatewayBudgetLabel(budgetSeconds),
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp
        )

        if (hiddenApps.isEmpty()) {
            Text(
                text = "Nothing is hidden. Every app you can launch is on the home screen.",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp
            )
        } else {
            hiddenApps.forEach { entry ->
                GatewayRow(
                    entry = entry,
                    isActive = entry.packageName == activePackage,
                    onOpen = {
                        viewModel.grantPassFor(entry.packageName) { onLaunch(entry.packageName) }
                    }
                )
            }
        }

        EmergencyControl(
            available = emergencyAvailable,
            hiddenApps = hiddenApps,
            onConfirm = { packageName ->
                viewModel.grantEmergencyFor(packageName)
                onLaunch(packageName)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter a reward code",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        CodeEntrySection(
            viewModel = viewModel,
            onCodeAccepted = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * No `enabled` flag tied to the budget: the row is always tappable and the ViewModel refuses when
 * there is nothing to spend. A second source of truth in the UI would disagree with the first the
 * moment the budget expired while this screen was open.
 */
@Composable
private fun GatewayRow(
    entry: AppCatalogRepository.AppEntry,
    isActive: Boolean,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF69F0AE) else Color(0xFF333333),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.label, color = Color.White, fontSize = 15.sp)
            Text(
                text = if (isActive) "Pass granted from here" else "Spends the budget above",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        }
        Button(
            onClick = onOpen,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF80CBC4),
                contentColor = Color.Black
            )
        ) {
            Text(if (isActive) "Again" else "Open")
        }
    }
}

/**
 * The gateway's money line. Reads the unspent budget, which keeps ticking between passes (E1). The
 * FULL tier's deadline is Int.MAX_VALUE minutes away, so a plain minute counter would print
 * 35791394m — that value is a window with no timer, and it is said that way.
 */
internal fun gatewayBudgetLabel(budgetSeconds: Long?): String {
    val seconds =
        budgetSeconds ?: return "No budget yet. Enter a reward code, or use Emergency on the home screen."
    if (seconds >= NO_TIMER_SECONDS) return "Budget remaining: no timer on this pass"
    return "Budget remaining: ${seconds / 60}m ${seconds % 60}s"
}

private const val NO_TIMER_SECONDS = 24L * 60 * 60
