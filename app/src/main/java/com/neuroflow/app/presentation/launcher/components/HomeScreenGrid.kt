package com.neuroflow.app.presentation.launcher.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.launcher.LauncherViewModel
import com.neuroflow.app.presentation.launcher.data.AppInfo
import com.neuroflow.app.presentation.launcher.data.HomeScreenItem
import com.neuroflow.app.presentation.launcher.data.HomeScreenPage
import com.neuroflow.app.presentation.launcher.folder.FolderIcon
import kotlinx.coroutines.launch

/**
 * Home screen grid with multi-page support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenGrid(
    viewModel: LauncherViewModel,
    launcherApps: android.content.pm.LauncherApps,
    modifier: Modifier = Modifier
) {
    val pages by viewModel.homeScreenPages.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val badgeCounts by viewModel.badgeCounts.collectAsStateWithLifecycle()
    val focusActive by viewModel.focusActive.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val appRepository = viewModel.getAppRepository()

    // Create first page if none exist
    LaunchedEffect(Unit) {
        if (pages.isEmpty()) {
            viewModel.addHomeScreenPage("Main")
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    var selectedFolder by remember { mutableStateOf<com.neuroflow.app.presentation.launcher.data.FolderDefinition?>(null) }
    var showFolderOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pager with pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                HomeScreenPageGrid(
                    page = pages[pageIndex],
                    allApps = allApps,
                    folders = folders,
                    badgeCounts = badgeCounts,
                    focusActive = focusActive,
                    launcherApps = launcherApps,
                    onAppTap = { app ->
                        scope.launch {
                            appRepository.launchApp(app.packageName, app.userHandle)
                        }
                    },
                    onFolderTap = { folder ->
                        selectedFolder = folder
                        showFolderOverlay = true
                    },
                    onRemoveItem = { position ->
                        viewModel.removeItemFromPage(pages[pageIndex].id, position)
                    }
                )
            }
        }

        // Page indicators
        PageIndicators(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            onAddPage = { viewModel.addHomeScreenPage() },
            onPageTap = { page ->
                scope.launch {
                    pagerState.animateScrollToPage(page)
                }
            }
        )
    }

    // Folder overlay
    if (showFolderOverlay && selectedFolder != null) {
        com.neuroflow.app.presentation.launcher.folder.AppFolderOverlay(
            folder = selectedFolder!!,
            apps = allApps,
            launcherApps = launcherApps,
            badgeCounts = badgeCounts,
            focusActive = focusActive,
            onDismiss = { showFolderOverlay = false },
            onAppLaunch = { packageName ->
                val app = allApps.firstOrNull { it.packageName == packageName }
                app?.let {
                    scope.launch {
                        appRepository.launchApp(it.packageName, it.userHandle)
                    }
                }
            },
            onAppLongPress = { },
            onRemoveApp = { packageName ->
                viewModel.removeAppFromFolder(selectedFolder!!.id, packageName)
            },
            onRenameFolder = { newName ->
                viewModel.renameFolder(selectedFolder!!.id, newName)
            },
            onPinToDock = { packageName ->
                viewModel.pinToDock(packageName)
            },
            onHide = { packageName ->
                viewModel.hideApp(packageName)
            }
        )
    }
}

/**
 * Single page grid (4x5 = 20 slots).
 */
@Composable
internal fun HomeScreenPageGrid(
    page: HomeScreenPage,
    allApps: List<AppInfo>,
    folders: List<com.neuroflow.app.presentation.launcher.data.FolderDefinition>,
    badgeCounts: Map<String, Int>,
    focusActive: Boolean,
    launcherApps: android.content.pm.LauncherApps,
    onAppTap: (AppInfo) -> Unit,
    onFolderTap: (com.neuroflow.app.presentation.launcher.data.FolderDefinition) -> Unit,
    onRemoveItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = 20 // 4x5

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(gridSize) { position ->
            val item = page.items.firstOrNull { it.gridPosition == position }

            GridSlot(
                item = item,
                allApps = allApps,
                folders = folders,
                badgeCounts = badgeCounts,
                focusActive = focusActive,
                launcherApps = launcherApps,
                onAppTap = onAppTap,
                onFolderTap = onFolderTap,
                onRemove = { onRemoveItem(position) }
            )
        }
    }
}

/**
 * Single grid slot (can be empty, app, or folder).
 */
@Composable
private fun GridSlot(
    item: HomeScreenItem?,
    allApps: List<AppInfo>,
    folders: List<com.neuroflow.app.presentation.launcher.data.FolderDefinition>,
    badgeCounts: Map<String, Int>,
    focusActive: Boolean,
    launcherApps: android.content.pm.LauncherApps,
    onAppTap: (AppInfo) -> Unit,
    onFolderTap: (com.neuroflow.app.presentation.launcher.data.FolderDefinition) -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            is HomeScreenItem.App -> {
                val app = allApps.firstOrNull { it.packageName == item.packageName }
                if (app != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppIcon(
                            appInfo = app,
                            launcherApps = launcherApps,
                            badgeCount = badgeCounts[app.packageName] ?: 0,
                            focusActive = focusActive,
                            modifier = Modifier.size(48.dp),
                            onTap = { onAppTap(app) },
                            onPinToDock = { },
                            onHide = { onRemove() }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            is HomeScreenItem.Folder -> {
                val folder = folders.firstOrNull { it.id == item.folderId }
                if (folder != null) {
                    FolderIcon(
                        folder = folder,
                        apps = allApps,
                        launcherApps = launcherApps,
                        modifier = Modifier.size(64.dp),
                        onTap = { onFolderTap(folder) },
                        onLongPress = { }
                    )
                }
            }
            null -> {
                // Empty slot - invisible placeholder to maintain grid layout
                Box(modifier = Modifier.size(64.dp))
            }
        }
    }
}

/**
 * Page indicators with add button.
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    onAddPage: () -> Unit,
    onPageTap: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Page dots
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 6.dp)
                    .background(
                        color = if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        shape = CircleShape
                    )
            )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Add page button
        if (pageCount < 10) {
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onAddPage,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add page",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
