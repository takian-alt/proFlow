package com.neuroflow.app.presentation.common.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = compositionLocalOf { false }

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

private val DarkColorScheme = darkColorScheme(
    primary = NeuroFlowColors.PurpleDarkMode,
    onPrimary = Color(0xFF1A1A3E),
    primaryContainer = NeuroFlowColors.PurpleDark,
    onPrimaryContainer = NeuroFlowColors.PurpleLight,
    secondary = NeuroFlowColors.ScheduleTextDark,
    onSecondary = Color(0xFF0A2E0F),
    secondaryContainer = NeuroFlowColors.ScheduleBgDark,
    onSecondaryContainer = NeuroFlowColors.ScheduleTextDark,
    tertiary = NeuroFlowColors.DelegateTextDark,
    onTertiary = Color(0xFF2E2A00),
    tertiaryContainer = NeuroFlowColors.DelegateBgDark,
    onTertiaryContainer = NeuroFlowColors.DelegateTextDark,
    error = NeuroFlowColors.DoFirstTextDark,
    onError = Color(0xFF3E0A0A),
    errorContainer = NeuroFlowColors.DoFirstBgDark,
    onErrorContainer = NeuroFlowColors.DoFirstTextDark,
    background = NeuroFlowColors.DarkBackground,
    onBackground = NeuroFlowColors.DarkText,
    surface = NeuroFlowColors.DarkSurface,
    onSurface = NeuroFlowColors.DarkText,
    surfaceVariant = NeuroFlowColors.DarkSurfaceAlt,
    onSurfaceVariant = NeuroFlowColors.DarkTextMuted,
    outline = NeuroFlowColors.DarkTextMuted,
    outlineVariant = NeuroFlowColors.DarkBorder,
)

@Composable
fun NeuroFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeuroFlowTypography,
            content = content
        )
    }
}
