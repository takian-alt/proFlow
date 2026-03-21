package com.neuroflow.app.presentation.launcher.domain

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
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
     * Get gesture exclusion rects based on navigation mode.
     *
     * @param screenHeightPx Screen height in pixels
     * @param screenWidthPx Screen width in pixels
     * @return List of exclusion rectangles
     */
    fun getExclusionRects(screenHeightPx: Int, screenWidthPx: Int): List<Rect> {
        if (navigationMode != NavigationMode.GESTURE) {
            return emptyList()
        }

        val exclusionDp = 200.dp
        val density = context.resources.displayMetrics.density
        val exclusionPx = (exclusionDp.value * density).toInt()

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
     * Only fires on deliberate, clearly directional swipes.
     */
    @Composable
    fun Modifier.attachGestures(): Modifier {
        val density = LocalDensity.current

        // Minimum distance in px before we consider it a swipe
        val minSwipePx = with(density) { 80.dp.toPx() }
        // Minimum ratio of primary axis to secondary axis (must be clearly directional)
        val directionRatio = 2.5f

        return this
            .nestedScroll(object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
                override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
            })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        var gestureHandled = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            if (gestureHandled) continue

                            val delta = change.position - downPos
                            val absX = abs(delta.x)
                            val absY = abs(delta.y)

                            // Must travel minimum distance
                            if (absX < minSwipePx && absY < minSwipePx) continue

                            // Must be clearly directional — not a diagonal
                            if (absY > absX) {
                                // Vertical — only if Y dominates strongly
                                if (absY < absX * directionRatio) continue
                                if (delta.y < 0) {
                                    onSwipeUp()
                                } else {
                                    onSwipeDown()
                                }
                                gestureHandled = true
                            } else {
                                // Horizontal — only if X dominates strongly
                                if (absX < absY * directionRatio) continue
                                if (delta.x < 0) {
                                    onSwipeLeft()
                                } else {
                                    onSwipeRight()
                                }
                                gestureHandled = true
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
    }
}
