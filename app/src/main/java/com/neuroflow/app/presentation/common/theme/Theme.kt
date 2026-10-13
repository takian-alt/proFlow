package com.neuroflow.app.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

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
    primary = NeuroFlowColors.Purple,
    onPrimary = Color.White,
    primaryContainer = NeuroFlowColors.PurpleDark,
    onPrimaryContainer = NeuroFlowColors.PurpleLight,
    secondary = NeuroFlowColors.ScheduleTextDark,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F291E),
    onSecondaryContainer = NeuroFlowColors.ScheduleTextDark,
    tertiary = NeuroFlowColors.DelegateTextDark,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF2E2207),
    onTertiaryContainer = NeuroFlowColors.DelegateTextDark,
    error = NeuroFlowColors.DoFirstTextDark,
    onError = Color.Black,
    errorContainer = Color(0xFF381519),
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
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (amoledDark && darkTheme) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
    } else {
        baseColorScheme
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeuroFlowTypography,
            content = content
        )
    }
}
