package com.neuroflow.app.presentation.v2.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NeuroFlowThemeV2Colors {
    // Deep Obsidian & Slate background palette
    val Obsidian = Color(0xFF0A0B0E)
    val SlateSurface = Color(0xFF13151B)
    val SlateSurfaceAlt = Color(0xFF1C1F28)
    val SlateBorder = Color(0xFF2A2E3B)
    val SlateDivider = Color(0xFF222736)

    // Bioluminescent Accents
    val ElectricIndigo = Color(0xFF6366F1)
    val ElectricIndigoDark = Color(0xFF4F46E5)
    val ElectricIndigoGlow = Color(0xFF818CF8)

    val SageGreen = Color(0xFF10B981)
    val SageGreenDark = Color(0xFF059669)
    val SageGreenContainer = Color(0xFF064E3B)

    val WarmAmber = Color(0xFFF59E0B)
    val WarmAmberContainer = Color(0xFF78350F)

    val MutedCoral = Color(0xFFF43F5E)
    val RoseCoral = MutedCoral
    val RoseCoralContainer = Color(0xFF881337)

    // Neutral Typography
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)
}

val NeuroFlowV2DarkColorScheme = darkColorScheme(
    primary = NeuroFlowThemeV2Colors.ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = NeuroFlowThemeV2Colors.ElectricIndigoDark,
    onPrimaryContainer = Color.White,

    secondary = NeuroFlowThemeV2Colors.SageGreen,
    onSecondary = Color.Black,
    secondaryContainer = NeuroFlowThemeV2Colors.SageGreenContainer,
    onSecondaryContainer = Color(0xFF6EE7B7),

    tertiary = NeuroFlowThemeV2Colors.WarmAmber,
    onTertiary = Color.Black,
    tertiaryContainer = NeuroFlowThemeV2Colors.WarmAmberContainer,
    onTertiaryContainer = Color(0xFFFDE68A),

    error = NeuroFlowThemeV2Colors.MutedCoral,
    onError = Color.White,
    errorContainer = NeuroFlowThemeV2Colors.RoseCoralContainer,
    onErrorContainer = Color(0xFFFECDD3),

    background = NeuroFlowThemeV2Colors.Obsidian,
    onBackground = NeuroFlowThemeV2Colors.TextPrimary,

    surface = NeuroFlowThemeV2Colors.SlateSurface,
    onSurface = NeuroFlowThemeV2Colors.TextPrimary,
    surfaceVariant = NeuroFlowThemeV2Colors.SlateSurfaceAlt,
    onSurfaceVariant = NeuroFlowThemeV2Colors.TextSecondary,

    outline = NeuroFlowThemeV2Colors.SlateBorder,
    outlineVariant = NeuroFlowThemeV2Colors.SlateDivider
)

@Composable
fun NeuroFlowThemeV2(
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = NeuroFlowV2DarkColorScheme
    val colorScheme = if (amoledDark) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0F1116)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
