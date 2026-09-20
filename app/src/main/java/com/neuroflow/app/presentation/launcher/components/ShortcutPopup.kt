package com.neuroflow.app.presentation.launcher.components

import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.UserHandle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.neuroflow.app.presentation.launcher.domain.BiometricAppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ShortcutPopup displays app shortcuts and standard options in a Popup anchored to the icon.
 *
 * Requirements:
 * - Display dynamic and static shortcuts via LauncherApps.getShortcuts()
 * - Show max 4 shortcuts above standard options
 * - Render as Popup anchored to icon position (not BottomSheet)
 * - Include standard options: "Open", "Pin to Dock", "Hide", "Lock app", "App Info"
 * - Launch shortcuts via LauncherApps.startShortcut()
 * - Silently omit shortcuts section on API 25 and below
 * - Omit shortcuts section when app has no declared shortcuts
 * - Hide "Lock app" option when BiometricManager reports no hardware (Requirement 15.6)
 *
 * @param packageName Package name of the app
 * @param userHandle User handle for the app (supports work profiles)
 * @param launcherApps LauncherApps system service
 * @param offset Offset from the anchor point (icon position)
 * @param onDismiss Callback when popup is dismissed
 * @param onOpen Callback to open the app
 * @param onPinToDock Callback to pin app to dock
 * @param onHide Callback to hide app from drawer
 * @param onLock Callback to lock app with biometric authentication
 * @param onAppInfo Callback to open app info settings
 */
@Composable
fun ShortcutPopup(
    packageName: String,
    userHandle: UserHandle,
    launcherApps: LauncherApps,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPinToDock: () -> Unit,
    onHide: () -> Unit,
    onLock: () -> Unit,
    onAddToHome: (() -> Unit)? = null,
    onAppInfo: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Check if biometric authentication is available (Requirement 15.6)
    val isBiometricAvailable = remember { BiometricAppLock.isAvailable(context) }

    // Load shortcuts on composition
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(packageName, userHandle) {
        isLoading = true
        shortcuts = loadShortcuts(launcherApps, packageName, userHandle)
        isLoading = false
    }

    // Convert DpOffset to IntOffset
    val intOffset = with(density) {
        androidx.compose.ui.unit.IntOffset(
            offset.x.roundToPx(),
            offset.y.roundToPx()
        )
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = intOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(220.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                // Shortcuts section (only if shortcuts exist and API >= 26)
                if (!isLoading && shortcuts.isNotEmpty()) {
                    shortcuts.take(4).forEach { shortcut ->
                        ShortcutMenuItem(
                            label = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut",
                            onClick = {
                                scope.launch {
                                    launchShortcut(launcherApps, shortcut)
                                    onDismiss()
                                }
                            }
                        )
                    }

                    // Divider between shortcuts and standard options
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Standard options
                ShortcutMenuItem(
                    label = "Open",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        onOpen()
                        onDismiss()
                    }
                )

                ShortcutMenuItem(
                    label = "Pin to Dock",
                    icon = Icons.Default.Add,
                    onClick = {
                        onPinToDock()
                        onDismiss()
                    }
                )

                // Add to Home Screen option (only shown from drawer context)
                if (onAddToHome != null) {
                    ShortcutMenuItem(
                        label = "Add to Home Screen",
                        icon = Icons.Default.Add,
                        onClick = {
                            onAddToHome()
                            onDismiss()
                        }
                    )
                }

                ShortcutMenuItem(
                    label = "Hide",
                    icon = Icons.Default.Close,
                    onClick = {
                        onHide()
                        onDismiss()
                    }
                )

                // Lock app option (only show if biometric hardware available - Requirement 15.6)
                if (isBiometricAvailable) {
                    ShortcutMenuItem(
                        label = "Lock app",
                        icon = Icons.Default.Lock,
                        onClick = {
                            onLock()
                            onDismiss()
                        }
                    )
                }

                ShortcutMenuItem(
                    label = "App Info",
                    icon = Icons.Default.Info,
                    onClick = {
                        onAppInfo()
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * Individual menu item in the shortcut popup.
 */
@Composable
private fun ShortcutMenuItem(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

/**
 * Load shortcuts for the specified app.
 * Returns empty list on API 25 and below, or when app has no shortcuts.
 *
 * @param launcherApps LauncherApps system service
 * @param packageName Package name of the app
 * @param userHandle User handle for the app
 * @return List of ShortcutInfo (max 4, empty on API < 26 or no shortcuts)
 */
private suspend fun loadShortcuts(
    launcherApps: LauncherApps,
    packageName: String,
    userHandle: UserHandle
): List<ShortcutInfo> = withContext(Dispatchers.IO) {
    // Silently omit shortcuts on API 25 and below (Requirement 5.4)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return@withContext emptyList()
    }

    try {
        // Query shortcuts using LauncherApps API
        val query = LauncherApps.ShortcutQuery()
        query.setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
        query.setPackage(packageName)

        val shortcuts = launcherApps.getShortcuts(query, userHandle) ?: emptyList()

        // Return max 4 shortcuts (Requirement 5.2)
        shortcuts.take(4)
    } catch (e: Exception) {
        // Silently handle errors and return empty list
        android.util.Log.e("ShortcutPopup", "Error loading shortcuts for $packageName", e)
        emptyList()
    }
}

/**
 * Launch a shortcut using LauncherApps API.
 *
 * @param launcherApps LauncherApps system service
 * @param shortcut ShortcutInfo to launch
 */
private suspend fun launchShortcut(
    launcherApps: LauncherApps,
    shortcut: ShortcutInfo
) = withContext(Dispatchers.IO) {
    try {
        // Launch shortcut via LauncherApps.startShortcut (Requirement 5.3)
        launcherApps.startShortcut(shortcut, null, null)
    } catch (e: Exception) {
        android.util.Log.e("ShortcutPopup", "Error launching shortcut ${shortcut.id}", e)
    }
}
