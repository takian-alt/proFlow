package com.neuroflow.app.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "More Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Section: Planning Tools
            MoreMenuSectionHeader("Planning")
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Notes",
                subtitle = "Brain dumps, daily thoughts & scratchpad",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.Notes.route)
                }
            )
            MoreMenuItem(
                icon = Icons.Filled.PauseCircle,
                title = "On Hold / Backlog",
                subtitle = "Someday/maybe items & frozen tasks",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.OnHold.route)
                }
            )
            MoreMenuItem(
                icon = Icons.Filled.Psychology,
                title = "Identity & Core Goals",
                subtitle = "Self-concept priming & long-term anchors",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.Identity.route)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Time & Focus
            MoreMenuSectionHeader("Time & Focus")
            MoreMenuItem(
                icon = Icons.Filled.Timer,
                title = "Log Time Manually",
                subtitle = "Record offline or retrospective focus work",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.LogTime.route)
                }
            )
            MoreMenuItem(
                icon = Icons.Filled.RocketLaunch,
                title = "HyperFocus",
                subtitle = "Allowlist, gate & kiosk mode",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.HyperFocusAllowlist.route)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Support & Config
            MoreMenuSectionHeader("Settings & Support")
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "How neuroFlow Works",
                subtitle = "Neuroscience principles, daily loops & guide",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.AppGuide.route)
                }
            )
            MoreMenuItem(
                icon = Icons.Filled.CloudSync,
                title = "Account & Cloud Sync",
                subtitle = "Backup, multi-device sync & Google Sign-In",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.AccountSync.route)
                }
            )
            MoreMenuItem(
                icon = Icons.Filled.Settings,
                title = "Preferences & Settings",
                subtitle = "Circadian windows, themes, weights & DND",
                onClick = {
                    onDismiss()
                    onNavigate(Screen.Settings.route)
                }
            )
        }
    }
}

@Composable
private fun MoreMenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
