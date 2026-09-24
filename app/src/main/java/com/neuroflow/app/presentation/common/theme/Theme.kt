package com.neuroflow.app.presentation.common.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NeuroFlowColors.Purple,
    onPrimary = Color.White,
    primaryContainer = NeuroFlowColors.PurpleLight,
    onPrimaryContainer = NeuroFlowColors.PurpleDark,
    secondary = NeuroFlowColors.ScheduleText,
    onSecondary = Color.White,
    secondaryContainer = NeuroFlowColors.ScheduleBg,
    onSecondaryContainer = NeuroFlowColors.ScheduleText,
    tertiary = NeuroFlowColors.DelegateText,
    onTertiary = Color.White,
    tertiaryContainer = NeuroFlowColors.DelegateBg,
    onTertiaryContainer = NeuroFlowColors.DelegateText,
    error = NeuroFlowColors.DoFirstText,
    onError = Color.White,
    errorContainer = NeuroFlowColors.DoFirstBg,
    onErrorContainer = NeuroFlowColors.DoFirstText,
    background = NeuroFlowColors.LightBackground,
    onBackground = NeuroFlowColors.LightText,
    surface = NeuroFlowColors.LightSurface,
    onSurface = NeuroFlowColors.LightText,
    surfaceVariant = NeuroFlowColors.LightSurfaceAlt,
    onSurfaceVariant = NeuroFlowColors.LightTextMuted,
    outline = NeuroFlowColors.LightTextMuted,
    outlineVariant = NeuroFlowColors.LightBorder,
)

@Composable
fun NeuroFlowTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = NeuroFlowTypography,
        content = content
    )
}
