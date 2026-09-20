package com.neuroflow.app.presentation.launcher.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflow.app.presentation.launcher.LauncherViewModel
import com.neuroflow.app.presentation.launcher.data.AppInfo
import com.neuroflow.app.presentation.launcher.data.HomeScreenItem
import com.neuroflow.app.presentation.launcher.data.HomeScreenPage
import com.neuroflow.app.presentation.launcher.domain.AdaptiveIconProcessor
import com.neuroflow.app.presentation.launcher.folder.FolderIcon
import com.neuroflow.app.presentation.launcher.theme.LocalLauncherTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    LaunchedEffect(Unit) {
        if (pages.isEmpty()) viewModel.addHomeScreenPage("Main")
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    var selectedFolder by remember { mutableStateOf<com.neuroflow.app.presentation.launcher.data.FolderDefinition?>(null) }
    var showFolderOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(400.dp)
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                HomeScreenPageGrid(
                    page = pages[pageIndex],
                    allApps = allApps,
                    folders = folders,
                    badgeCounts = badgeCounts,
                    focusActive = focusActive,
                    launcherApps = launcherApps,
                    onAppTap = { app -> scope.launch { appRepository.launchApp(app.packageName, app.userHandle) } },
                    onFolderTap = { folder -> selectedFolder = folder; showFolderOverlay = true },
                    onRemoveItem = { position -> viewModel.removeItemFromPage(pages[pageIndex].id, position) },
                    onMoveItem = { from, to -> viewModel.moveItemInPage(pages[pageIndex].id, from, to) },
                    isDraggingIcon = viewModel.isDraggingIcon
                )
            }
        }

        PageIndicators(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            onAddPage = { viewModel.addHomeScreenPage() },
            onPageTap = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
        )
    }

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
                app?.let { scope.launch { appRepository.launchApp(it.packageName, it.userHandle) } }
            },
            onAppLongPress = { },
            onRemoveApp = { packageName -> viewModel.removeAppFromFolder(selectedFolder!!.id, packageName) },
            onRenameFolder = { newName -> viewModel.renameFolder(selectedFolder!!.id, newName) },
            onPinToDock = { packageName -> viewModel.pinToDock(packageName) },
            onHide = { packageName -> viewModel.hideApp(packageName) }
        )
    }
}

/**
 * Grid with drag-to-reorder using detectDragGesturesAfterLongPress.
 *
 * Each slot registers its window-space bounds via onGloballyPositioned.
 * On long-press, drag mode activates and a ghost follows the finger.
 * On release, the item is moved to the nearest slot.
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
    onMoveItem: ((Int, Int) -> Unit)? = null,
    isDraggingIcon: androidx.compose.runtime.MutableState<Boolean>? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val columns = 4
    val rows = 5

    // Window-space bounds of each slot index
    val slotBounds = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }

    // Drag state
    var draggingFrom by remember { mutableStateOf<Int?>(null) }
    var draggingOver by remember { mutableStateOf<Int?>(null) }
    // Finger position in window coords
    var fingerWindowPos by remember { mutableStateOf(Offset.Zero) }
    // Grid container window-space origin
    var gridWindowOrigin by remember { mutableStateOf(Offset.Zero) }

    // Find the nearest slot to a window-space point
    fun slotAt(pos: Offset): Int? {
        return slotBounds.entries.minByOrNull { (_, rect) ->
            val cx = rect.left + rect.width / 2
            val cy = rect.top + rect.height / 2
            val dx = pos.x - cx
            val dy = pos.y - cy
            dx * dx + dy * dy
        }?.key
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                gridWindowOrigin = Offset(bounds.left, bounds.top)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until columns) {
                        val position = row * columns + col
                        val item = page.items.firstOrNull { it.gridPosition == position }
                        val isDragging = draggingFrom == position
                        val isDropTarget = draggingOver == position
                            && draggingFrom != null
                            && draggingFrom != position

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coords ->
                                    slotBounds[position] = coords.boundsInWindow()
                                }
                                .background(
                                    if (isDropTarget)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .then(
                                    if (item != null) {
                                        Modifier
                                            .pointerInput(position, item) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (draggingFrom == null) {
                                                            when (item) {
                                                                is HomeScreenItem.App -> {
                                                                    val app = allApps.firstOrNull {
                                                                        it.packageName == item.packageName
                                                                    }
                                                                    app?.let { onAppTap(it) }
                                                                }
                                                                is HomeScreenItem.Folder -> {
                                                                    val folder = folders.firstOrNull {
                                                                        it.id == item.folderId
                                                                    }
                                                                    folder?.let { onFolderTap(it) }
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .pointerInput(position, item) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { localOffset ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        draggingFrom = position
                                                        draggingOver = position
                                                        isDraggingIcon?.value = true
                                                        val bounds = slotBounds[position]
                                                        if (bounds != null) {
                                                            fingerWindowPos = Offset(
                                                                bounds.left + localOffset.x,
                                                                bounds.top + localOffset.y
                                                            )
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        // Accumulate using dragAmount delta (window-space)
                                                        fingerWindowPos = fingerWindowPos + dragAmount
                                                        draggingOver = slotAt(fingerWindowPos)
                                                    },
                                                    onDragEnd = {
                                                        val from = draggingFrom
                                                        val to = draggingOver
                                                        if (from != null && to != null && from != to) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            onMoveItem?.invoke(from, to)
                                                        }
                                                        draggingFrom = null
                                                        draggingOver = null
                                                        isDraggingIcon?.value = false
                                                    },
                                                    onDragCancel = {
                                                        draggingFrom = null
                                                        draggingOver = null
                                                        isDraggingIcon?.value = false
                                                    }
                                                )
                                            }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isDragging) {
                                SlotContent(
                                    item = item,
                                    allApps = allApps,
                                    folders = folders,
                                    badgeCounts = badgeCounts,
                                    focusActive = focusActive,
                                    launcherApps = launcherApps
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ghost icon — rendered in grid-local coordinates
        val fromPos = draggingFrom
        if (fromPos != null) {
            val item = page.items.firstOrNull { it.gridPosition == fromPos }
            if (item != null) {
                val iconSizeDp = 64.dp
                val density = LocalDensity.current
                val iconSizePx = with(density) { iconSizeDp.toPx() }

                val ghostX = fingerWindowPos.x - gridWindowOrigin.x - iconSizePx / 2
                val ghostY = fingerWindowPos.y - gridWindowOrigin.y - iconSizePx / 2

                Box(
                    modifier = Modifier
                        .offset { IntOffset(ghostX.roundToInt(), ghostY.roundToInt()) }
                        .size(iconSizeDp)
                        .zIndex(10f)
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                            alpha = 0.92f
                            shadowElevation = 8.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SlotContent(
                        item = item,
                        allApps = allApps,
                        folders = folders,
                        badgeCounts = badgeCounts,
                        focusActive = false,
                        launcherApps = launcherApps
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotContent(
    item: HomeScreenItem?,
    allApps: List<AppInfo>,
    folders: List<com.neuroflow.app.presentation.launcher.data.FolderDefinition>,
    badgeCounts: Map<String, Int>,
    focusActive: Boolean,
    launcherApps: android.content.pm.LauncherApps
) {
    when (item) {
        is HomeScreenItem.App -> {
            val app = allApps.firstOrNull { it.packageName == item.packageName } ?: return
            val theme = LocalLauncherTheme.current
            val context = LocalContext.current
            val themeBgColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
            val themeFgColor = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()
            val processedIcon = remember(app.icon, theme.iconShape, theme.themedIconsEnabled, themeBgColor, themeFgColor) {
                val sizePx = (48.dp.value * context.resources.displayMetrics.density).toInt()
                AdaptiveIconProcessor.process(
                    drawable = app.icon,
                    shape = theme.iconShape,
                    sizePx = sizePx,
                    context = context,
                    themed = theme.themedIconsEnabled,
                    themeBgColor = themeBgColor,
                    themeFgColor = themeFgColor
                )
            }
            val iconAlpha = if (focusActive && app.distractionScore > 70) 0.4f else 1.0f
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        bitmap = processedIcon.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(48.dp).alpha(iconAlpha)
                    )
                    val badge = badgeCounts[app.packageName] ?: 0
                    if (badge > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(if (badge > 99) "99+" else "$badge", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        is HomeScreenItem.Folder -> {
            val folder = folders.firstOrNull { it.id == item.folderId } ?: return
            FolderIcon(
                folder = folder,
                apps = allApps,
                launcherApps = launcherApps,
                modifier = Modifier.size(64.dp),
                onTap = {},
                onLongPress = {}
            )
        }
        null -> Box(modifier = Modifier.size(64.dp))
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    onAddPage: () -> Unit,
    onPageTap: (Int) -> Unit
) {
    if (pageCount <= 1 && pageCount < 10) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAddPage,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add page",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        return
    }

    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = index == currentPage
                val targetWidth = if (isSelected) 20.dp else 6.dp
                val width by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "page_indicator_width"
                )
                val targetColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                val color by animateColorAsState(
                    targetValue = targetColor,
                    label = "page_indicator_color"
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPageTap(index)
                        }
                )
            }
        }
        if (pageCount < 10) {
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = onAddPage,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add page",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
