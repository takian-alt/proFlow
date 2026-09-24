package com.neuroflow.app.presentation.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.neuroflow.app.data.sync.BackupMeta
import com.neuroflow.app.data.sync.BackupStatus
import com.neuroflow.app.data.sync.GoogleAuthState
import com.neuroflow.app.data.sync.RestoreStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountSyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val restoreStatus by viewModel.restoreStatus.collectAsStateWithLifecycle()
    val availableBackups by viewModel.availableBackups.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result)
    }

    fun launchGoogleSignIn() {
        val activity = context.findActivity() ?: return
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.neuroflow.app.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        client.signOut().addOnCompleteListener {
            signInLauncher.launch(client.signInIntent)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Cloud Sync") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val state = authState) {
                is GoogleAuthState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is GoogleAuthState.SignedOut, is GoogleAuthState.Error -> {
                    item { SignedOutCard(
                        error = (state as? GoogleAuthState.Error)?.message,
                        onSignIn = { launchGoogleSignIn() }
                    ) }
                }

                is GoogleAuthState.SignedIn -> {
                    item { ProfileCard(state = state, onSignOut = { viewModel.signOut() }) }

                    item {
                        BackupRestoreCard(
                            backupStatus = backupStatus,
                            restoreStatus = restoreStatus,
                            onBackupNow = { viewModel.backupNow() },
                            onRestoreLatest = { viewModel.restoreLatest() },
                            onClearBackupStatus = { viewModel.clearBackupStatus() },
                            onClearRestoreStatus = { viewModel.clearRestoreStatus() }
                        )
                    }

                    item {
                        AutoSyncToggleCard(
                            enabled = autoSyncEnabled,
                            onToggle = { viewModel.setAutoSync(it) }
                        )
                    }

                    if (availableBackups.isNotEmpty()) {
                        item {
                            Text(
                                "Backup History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(availableBackups) { meta ->
                            BackupHistoryItem(
                                meta = meta,
                                onRestore = { viewModel.restoreFromBackup(meta.backupId) },
                                onDelete = { viewModel.deleteBackup(meta.backupId) }
                            )
                        }
                    }


                    item {
                        Text(
                            "Your data is stored privately in your Google account and is never shared. Backups include tasks, goals, sessions, sleep logs, WOOP data, energy predictions, and settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedOutCard(error: String?, onSignIn: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Cloud,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Cloud Backup & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with Google to back up your tasks, goals, sessions, and settings to your private cloud storage. Access your data on any device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google")
            }
        }
    }
}

@Composable
private fun ProfileCard(state: GoogleAuthState.SignedIn, onSignOut: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.photoUrl != null) {
                AsyncImage(
                    model = state.photoUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(state.displayName, fontWeight = FontWeight.SemiBold)
                Text(state.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Composable
private fun BackupRestoreCard(
    backupStatus: BackupStatus,
    restoreStatus: RestoreStatus,
    onBackupNow: () -> Unit,
    onRestoreLatest: () -> Unit,
    onClearBackupStatus: () -> Unit,
    onClearRestoreStatus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            // Backup status banner
            when (backupStatus) {
                is BackupStatus.InProgress -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is BackupStatus.Success -> StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    message = "Backup saved • ${formatTimestamp(backupStatus.timestamp)}",
                    isError = false,
                    onDismiss = onClearBackupStatus
                )
                is BackupStatus.Error -> StatusBanner(
                    icon = Icons.Filled.Error,
                    message = backupStatus.message,
                    isError = true,
                    onDismiss = onClearBackupStatus
                )
                else -> Unit
            }

            // Restore status banner
            when (restoreStatus) {
                is RestoreStatus.InProgress -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is RestoreStatus.Success -> StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    message = "Restored ${restoreStatus.itemCount} items • ${formatTimestamp(restoreStatus.timestamp)}",
                    isError = false,
                    onDismiss = onClearRestoreStatus
                )
                is RestoreStatus.Error -> StatusBanner(
                    icon = Icons.Filled.Error,
                    message = restoreStatus.message,
                    isError = true,
                    onDismiss = onClearRestoreStatus
                )
                else -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBackupNow,
                    enabled = backupStatus !is BackupStatus.InProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back up now")
                }
                OutlinedButton(
                    onClick = onRestoreLatest,
                    enabled = restoreStatus !is RestoreStatus.InProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
}

@Composable
private fun AutoSyncToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(

        onClick = { onToggle(!enabled) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-sync", fontWeight = FontWeight.Medium)
                Text("Back up automatically once a day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun BackupHistoryItem(
    meta: BackupMeta,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Backup") },
            text = { Text("Are you sure you want to delete the backup from ${formatTimestamp(meta.createdAt)}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTimestamp(meta.createdAt), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${meta.taskCount} tasks · ${meta.goalCount} goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRestore) { Text("Restore") }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete backup",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


@Composable
private fun StatusBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, isError: Boolean, onDismiss: () -> Unit) {
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = containerColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = contentColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = contentColor, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

