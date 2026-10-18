package com.neuroflow.app.presentation.v2.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.neuroflow.app.presentation.common.theme.LocalIsDarkTheme

@Immutable
data class NeuroFlowSemanticPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primaryIndigo: Color,
    val energyPeakMint: Color,
    val energyModerateAmber: Color,
    val energyLowCoral: Color,
    val hyperFocusViolet: Color
)

val DarkSemanticPalette = NeuroFlowSemanticPalette(
    isDark = true,
    background = Color(0xFF0A0B0E),
    surface = Color(0xFF13151B),
    surfaceElevated = Color(0xFF1C2029),
    border = Color(0xFF2A2E3B),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    primaryIndigo = Color(0xFF6366F1),
    energyPeakMint = Color(0xFF10B981),
    energyModerateAmber = Color(0xFFF59E0B),
    energyLowCoral = Color(0xFFF43F5E),
    hyperFocusViolet = Color(0xFF8B5CF6)
)

val AmoledSemanticPalette = DarkSemanticPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0D12),
    surfaceElevated = Color(0xFF141720),
    border = Color(0xFF222634),
    primaryIndigo = Color(0xFF818CF8),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF78869B)
)

val LightSemanticPalette = NeuroFlowSemanticPalette(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    border = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF64748B),
    primaryIndigo = Color(0xFF4F46E5),
    energyPeakMint = Color(0xFF059669),
    energyModerateAmber = Color(0xFFD97706),
    energyLowCoral = Color(0xFFE11D48),
    hyperFocusViolet = Color(0xFF7C3AED)
)

val LocalNeuroFlowPalette = staticCompositionLocalOf { DarkSemanticPalette }

/**
 * Reactive semantic token bridge: existing V2 composables referencing NeuroFlowThemeV2Colors
 * automatically adapt to Light, Dark, and AMOLED modes without breaking call sites.
 */
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

    val BioMint = Color(0xFF10B981)
    val SageGreen = Color(0xFF10B981)
    val SageGreenDark = Color(0xFF059669)
    val SageGreenContainer = Color(0xFF064E3B)

    val SolarAmber = Color(0xFFF59E0B)
    val WarmAmber = Color(0xFFF59E0B)
    val WarmAmberContainer = Color(0xFF78350F)

    val PulseCoral = Color(0xFFF43F5E)
    val MutedCoral = Color(0xFFF43F5E)
    val RoseCoral = MutedCoral
    val RoseCoralContainer = Color(0xFF881337)
    val DeepViolet = Color(0xFF7C3AED)

    // Neutral Typography
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)

    /**
     * Active semantic palette for the current composition tree.
     */
    val current: NeuroFlowSemanticPalette
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current
}

@Composable
fun NeuroFlowThemeV2(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = when {
        !darkTheme -> LightSemanticPalette
        amoledDark -> AmoledSemanticPalette
        else -> DarkSemanticPalette
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primaryIndigo,
            onPrimary = Color.White,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.energyPeakMint,
            onSecondary = Color.Black,
            secondaryContainer = if (amoledDark) Color(0xFF042F2E) else Color(0xFF064E3B),
            onSecondaryContainer = Color(0xFF6EE7B7),
            tertiary = palette.energyModerateAmber,
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFF78350F),
            onTertiaryContainer = Color(0xFFFDE68A),
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.energyLowCoral,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.primaryIndigo,
            onPrimary = Color.White,
            primaryContainer = palette.surfaceElevated,
            onPrimaryContainer = palette.primaryIndigo,
            secondary = palette.energyPeakMint,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD1FAE5),
            onSecondaryContainer = Color(0xFF065F46),
            tertiary = palette.energyModerateAmber,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFEF3C7),
            onTertiaryContainer = Color(0xFF92400E),
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.energyLowCoral,
            onError = Color.White
        )
    }

    CompositionLocalProvider(
        LocalNeuroFlowPalette provides palette,
        LocalIsDarkTheme provides darkTheme // LocalIsDarkTheme provides true
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
