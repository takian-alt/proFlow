package com.neuroflow.app.feature.hyperfocus.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.neuroflow.app.feature.hyperfocus.data.AppCatalogRepository
import com.neuroflow.app.feature.hyperfocus.screens.EmergencyControl

@Composable
fun HyperFocusHomeScreen(
    state: HomeSurfaceState,
    canHandOff: Boolean,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    emergencyAvailable: Boolean,
    onOpenEssential: (String) -> Unit,
    onOpenGate: () -> Unit,
    onEmergencyOpen: (String) -> Unit,
    onDeactivate: () -> Unit,
    onOpenAllowlistEditor: () -> Unit,
    onOpenHomeAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        HomeSurfaceState.HandOff ->
            if (!canHandOff) OffPlaceholder(
                onOpenAllowlistEditor = onOpenAllowlistEditor,
                onOpenHomeAppSettings = onOpenHomeAppSettings
            )
            // When a hand-off target exists the activity is already finishing; painting a
            // black frame here only makes the swap visible.
            else Box(modifier = modifier.fillMaxSize().background(Color.Black))

        is HomeSurfaceState.Active -> ActiveSurface(
            state = state,
            hiddenApps = hiddenApps,
            emergencyAvailable = emergencyAvailable,
            onOpenEssential = onOpenEssential,
            onOpenGate = onOpenGate,
            onEmergencyOpen = onEmergencyOpen,
            onDeactivate = onDeactivate,
            modifier = modifier
        )
    }
}

@Composable
private fun ActiveSurface(
    state: HomeSurfaceState.Active,
    hiddenApps: List<AppCatalogRepository.AppEntry>,
    emergencyAvailable: Boolean,
    onOpenEssential: (String) -> Unit,
    onOpenGate: () -> Unit,
    onEmergencyOpen: (String) -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF050505))) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            contentPadding = PaddingValues(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(
                    state = state,
                    hiddenCount = hiddenApps.size,
                    onDeactivate = onDeactivate
                )
            }
            items(state.essentials, key = { it.packageName }) { tile ->
                HomeTileView(tile = tile, onClick = { onOpenEssential(tile.packageName) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Button(
                        onClick = onOpenGate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2E2E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hiddenApps.isEmpty()) "Open the gate"
                            else "Open the gate (${hiddenApps.size} hidden)",
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    EmergencyControl(
                        available = emergencyAvailable,
                        hiddenApps = hiddenApps,
                        onConfirm = onEmergencyOpen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    state: HomeSurfaceState.Active,
    hiddenCount: Int,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = state.passSecondsRemaining
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = "HyperFocus is on",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = when {
                seconds == null -> "Nothing open. Essentials only."
                state.passPackage == null -> "A window is open: ${seconds / 60}m ${seconds % 60}s"
                else -> "Open: ${state.passPackage} for ${seconds / 60}m ${seconds % 60}s"
            },
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = if (hiddenCount == 0) "No app is hidden right now."
            else "$hiddenCount app${if (hiddenCount == 1) "" else "s"} behind the gate.",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedButton(
                onClick = onDeactivate,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
            ) {
                Text("End session")
            }
        }
    }
}

@Composable
private fun HomeTileView(tile: HomeTile, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(tile.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(tile.packageName)
                .toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = tile.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(56.dp)
            )
        } else {
            // A package uninstalled mid-session throws here, and a throwing icon lookup inside a grid
            // item would crash the whole home rather than the one tile (spec 8, "uninstalled mid-pass").
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = tile.label.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = tile.label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun OffPlaceholder(
    onOpenAllowlistEditor: () -> Unit,
    onOpenHomeAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().background(Color(0xFF050505))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HyperFocus is off",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "No other home app on this device could be found, so this screen is not " +
                    "pretending to be one. Choose your home app, or start a session.",
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenHomeAppSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Set my home app")
                }
                Button(
                    onClick = onOpenAllowlistEditor,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E2E2E),
                        contentColor = Color.White
                    )
                ) {
                    Text("Edit allowlist")
                }
            }
        }
    }
}
