package com.neuroflow.app.presentation.launcher.domain

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Centralized gesture handler for launcher home screen.
 * Detects navigation mode and handles all swipe and long-press gestures.
 *
 * @param context Android context for system settings access
 * @param onSwipeUp Callback for swipe up gesture (opens app drawer)
 * @param onSwipeDown Callback for swipe down gesture (notification shade)
 * @param onSwipeLeft Callback for swipe left gesture (quick stats panel)
 * @param onSwipeRight Callback for swipe right gesture (soft bounce)
 * @param onLongPress Callback for long press gesture (launcher settings)
 */
class LauncherGestureHandler(
    private val context: Context,
    private val onSwipeUp: () -> Unit,
    private val onSwipeDown: () -> Unit,
    private val onSwipeLeft: () -> Unit,
    private val onSwipeRight: () -> Unit,
    private val onLongPress: () -> Unit
) {
    /**
     * Detected navigation mode from system settings.
     */
    val navigationMode: NavigationMode by lazy {
        detectNavigationMode()
    }

    /**
     * Detect the current navigation mode from system settings.
     */
    private fun detectNavigationMode(): NavigationMode {
        return try {
            val mode = Settings.Secure.getInt(
                context.contentResolver,
                "navigation_mode",
                0
            )
            when (mode) {
                0 -> NavigationMode.THREE_BUTTON
                1 -> NavigationMode.TWO_BUTTON
                2 -> NavigationMode.GESTURE
                else -> NavigationMode.THREE_BUTTON
            }
        } catch (e: Exception) {
            Log.w("LauncherGesture", "Failed to detect navigation mode", e)
            NavigationMode.THREE_BUTTON
        }
    }

    /**
     * Get gesture exclusion rects based on navigation mode and manufacturer.
     *
     * @param screenHeightPx Screen height in pixels
     * @param screenWidthPx Screen width in pixels
     * @return List of exclusion rectangles
     */
    fun getExclusionRects(screenHeightPx: Int, screenWidthPx: Int): List<Rect> {
        if (navigationMode != NavigationMode.GESTURE) {
            return emptyList()
        }

        // Determine exclusion zone height based on manufacturer
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val exclusionDp = if (isSamsung) 280.dp else 200.dp

        // Convert dp to pixels
        val density = context.resources.displayMetrics.density
        val exclusionPx = (exclusionDp.value * density).toInt()

        // Bottom exclusion rect
        return listOf(
            Rect(
                0,
                screenHeightPx - exclusionPx,
                screenWidthPx,
                screenHeightPx
            )
        )
    }

    /**
     * Attach gesture detection to a composable.
     * Uses nestedScroll API to avoid conflicts with scrollable children.
     */
    @Composable
    fun Modifier.attachGestures(): Modifier {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current

        // Calculate swipe threshold based on orientation
        val swipeThreshold = with(density) {
            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape: 30% of screen height
                (configuration.screenHeightDp * 0.3f).dp.toPx()
            } else {
                // Portrait: fixed 200dp or above exclusion zone
                val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
                val exclusionDp = if (isSamsung) 280.dp else 200.dp
                exclusionDp.toPx()
            }
        }

        var dragStart by remember { mutableStateOf<Offset?>(null) }
        var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

        return this
            .nestedScroll(object : NestedScrollConnection {
                // Allow scrollable children to consume scroll first
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    return Offset.Zero // Let children scroll first
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return Velocity.Zero // Let children fling first
                }
            })
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                    },
                    onDragEnd = {
                        val start = dragStart
                        if (start != null) {
                            // Gesture ended - dragStart was set but no onDrag called
                            // This is handled in onDrag
                        }
                        dragStart = null
                        longPressJob?.cancel()
                        longPressJob = null
                    },
                    onDragCancel = {
                        dragStart = null
                        longPressJob?.cancel()
                        longPressJob = null
                    },
                    onDrag = { change, dragAmount ->
                        val start = dragStart
                        if (start != null) {
                            val current = change.position
                            val delta = current - start

                            // Determine gesture direction
                            val absX = abs(delta.x)
                            val absY = abs(delta.y)

                            // Require minimum drag distance
                            val minDragDistance = 50f

                            if (absX > minDragDistance || absY > minDragDistance) {
                                if (absY > absX) {
                                    // Vertical gesture
                                    if (delta.y < 0) {
                                        // Swipe up
                                        if (start.y > swipeThreshold) {
                                            onSwipeUp()
                                            dragStart = null
                                        }
                                    } else {
                                        // Swipe down
                                        onSwipeDown()
                                        dragStart = null
                                    }
                                } else {
                                    // Horizontal gesture
                                    if (delta.x < 0) {
                                        // Swipe left
                                        onSwipeLeft()
                                        dragStart = null
                                    } else {
                                        // Swipe right
                                        onSwipeRight()
                                        dragStart = null
                                    }
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        onLongPress()
                    }
                )
            }
    }
}
