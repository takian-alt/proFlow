package com.neuroflow.app.feature.hyperfocus.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neuroflow.app.feature.hyperfocus.HyperFocusViewModel
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.domain.ActivationDecision
import com.neuroflow.app.feature.hyperfocus.domain.ActivationRejection
import com.neuroflow.app.feature.hyperfocus.domain.decideActivation
import com.neuroflow.app.feature.hyperfocus.surface.AllowlistTier
import com.neuroflow.app.feature.hyperfocus.surface.SystemExempt
import com.neuroflow.app.feature.hyperfocus.surface.draftFrom
import com.neuroflow.app.feature.hyperfocus.surface.toggleTier

/**
 * The whole allowlist in one screen: what sits on the black home, what is reachable through the gate,
 * and what needs a pass. The edits happen on a draft so the counts at the foot describe the list the
 * user is arranging rather than whatever was saved last.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowlistSettingsScreen(
    viewModel: HyperFocusViewModel,
    onNavigateBack: () -> Unit,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.hyperFocusPrefs.collectAsState()
    val activationMessage by viewModel.activationMessage.collectAsState()
    var draft by remember { mutableStateOf(draftFrom(prefs)) }
    var edited by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppCatalogRepository.AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    // The prefs flow starts from an empty default and the stored tiers arrive a frame later, so an
    // untouched draft has to follow them — otherwise the editor opens blank over a real allowlist and
    // Save writes it away.
    LaunchedEffect(prefs) { if (!edited) draft = draftFrom(prefs) }

    LaunchedEffect(Unit) { apps = viewModel.loadEditableApps() }

    val trimmed = query.trim()
    val visible = remember(apps, trimmed) {
        val sorted = apps.sortedBy { it.label.lowercase() }
        if (trimmed.isEmpty()) sorted else sorted.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
                it.packageName.contains(trimmed, ignoreCase = true)
        }
    }

    val essentialCount = draft.essential.size
    val allowedCount = draft.allowed.size
    val gatedCount = (apps.size - essentialCount - allowedCount).coerceAtLeast(0)
    val decision = decideActivation(prefs, SystemExempt.STATIC, viewModel.selfPackage)
    // Only the empty surface is actionable from this screen — the user is standing on the editor that
    // fixes it. "Already running" is not a reason to grey out a button on a screen that can't see the
    // session.
    val startBlockedReason = if (decision == ActivationDecision.Rejected(ActivationRejection.EmptySurface))
        "Pin at least one app as essential." else null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("HyperFocus") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Essential apps stay on the black home. Allowed apps are one gate away. " +
                    "Everything else needs a pass before it will open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(visible, key = { it.packageName }) { app ->
                    AppTierRow(
                        label = app.label,
                        packageName = app.packageName,
                        tier = when (app.packageName) {
                            in draft.essential -> AllowlistTier.Essential
                            in draft.allowed -> AllowlistTier.Allowed
                            else -> null
                        },
                        onTier = { tier ->
                            edited = true
                            draft = toggleTier(draft, app.packageName, tier)
                        }
                    )
                    HorizontalDivider()
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$essentialCount on the home · $allowedCount behind the gate · $gatedCount need a pass",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Exempt system apps and ${viewModel.selfPackage} are always reachable " +
                            "and are not listed here.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            activationMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.saveAllowlist(draft.essential, draft.allowed) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Button(
                    onClick = onStartSession,
                    enabled = startBlockedReason == null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Start session") }
            }
            startBlockedReason?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AppTierRow(
    label: String,
    packageName: String,
    tier: AllowlistTier?,
    onTier: (AllowlistTier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AllowlistTier.entries.forEach { candidate ->
            FilterChip(
                selected = tier == candidate,
                onClick = { onTier(candidate) },
                label = { Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
