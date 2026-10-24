package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository

/**
 * The one tap that does not pass a gate. It is ungated by decision F4, so it lives on the surface
 * the user is standing on — the black home, the overlay and the gateway — and never behind the gate
 * it would short-circuit.
 *
 * The availability and the app list arrive as data, not from a ViewModel: the three surfaces are
 * driven by two different ViewModels, and a `hiltViewModel()` default would hand the home screen a
 * second, unrelated HyperFocusViewModel whose catalog sweep the user never asked for. The list is
 * still the caller's hidden-apps flow — an app the surface hides is exactly an app that needs an
 * emergency, and enumerating the launcher again would give the picker and the grid different answers.
 */
@Composable
fun EmergencyControl(
    available: Boolean,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val trimmed = query.trim()
    val matches = if (trimmed.isEmpty()) hiddenApps else hiddenApps.filter {
        it.label.contains(trimmed, ignoreCase = true) ||
            it.packageName.contains(trimmed, ignoreCase = true)
    }
    // A dotted query that matched nothing is read as a package name the catalog never surfaced.
    val rawPackage = trimmed.takeIf { it.isNotBlank() && it.contains('.') && matches.isEmpty() }
    val target = selected ?: rawPackage
    val targetLabel = hiddenApps.firstOrNull { it.packageName == target }?.label ?: target.orEmpty()

    Button(
        onClick = { showDialog = true },
        enabled = available,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6D4C41),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF333333),
            disabledContentColor = Color(0xFF777777)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text("Emergency $EMERGENCY_COPY_MINUTES minutes", fontWeight = FontWeight.Bold)
    }

    if (!available) {
        Text(
            text = EMERGENCY_UNAVAILABLE_COPY,
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Emergency access") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "$EMERGENCY_COPY_MINUTES minutes with one app. No task, and no reason " +
                            "asked for. You only get one per session.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { value ->
                            query = value
                            selected = null
                        },
                        label = { Text("Find the app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (rawPackage != null) {
                        PickerRow(
                            label = "Grant to $rawPackage",
                            selected = target == rawPackage,
                            onSelect = { selected = rawPackage }
                        )
                    } else if (matches.isEmpty()) {
                        Text(
                            text = if (hiddenApps.isEmpty())
                                "Nothing is hidden right now. Type a package name to grant it anyway."
                            else "No app matches \"$trimmed\". Type its package name to grant it anyway.",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }
                    matches.forEach { entry ->
                        PickerRow(
                            label = entry.label,
                            selected = selected == entry.packageName,
                            onSelect = { selected = entry.packageName }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = target != null,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8A80)),
                    onClick = {
                        val packageName = target ?: return@TextButton
                        // Collapse before granting: the grant ends this screen's reason to exist, and
                        // a dialog left open over a launched app is a window on the wrong state.
                        showDialog = false
                        onConfirm(packageName)
                    }
                ) {
                    val label = if (targetLabel.isBlank()) "Use Emergency" else "Use on ${if (targetLabel.length > 16) targetLabel.take(16) + "…" else targetLabel}"
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            dismissButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFAAAAAA)),
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            label,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Kept in sync with HyperFocusManagerImpl.EMERGENCY_MINUTES (domain/HyperFocusManager.kt:65); the
// manager owns the number, this is only the copy that promises it.
private const val EMERGENCY_COPY_MINUTES = 10
private const val EMERGENCY_UNAVAILABLE_COPY =
    "Emergency is spent for this session. A reward code still opens a window."
