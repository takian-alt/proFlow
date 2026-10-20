package com.neuroflow.app.presentation.v2.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neuroflow.app.domain.service.EnergyPhase
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
 * Extended semantic color tokens for ADR-008 UI/UX components.
 */
@Immutable
data class NeuroFlowExtendedColors(
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val borderMuted: Color,
    val energyPeak: Color,
    val energyPeakContainer: Color,
    val energyModerate: Color,
    val energyModerateContainer: Color,
    val energyLow: Color,
    val energyLowContainer: Color,
    val energyRecovery: Color,
    val energyRecoveryContainer: Color,
    val energyDepleted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val glowColor: Color,
)

val DarkExtendedColors = NeuroFlowExtendedColors(
    surfaceElevated = Color(0xFF161A22),
    surfaceSubtle = Color(0xFF0F1218),
    borderMuted = Color(0xFF21262D),
    energyPeak = Color(0xFF00E699),
    energyPeakContainer = Color(0xFF003820),
    energyModerate = Color(0xFFFFB800),
    energyModerateContainer = Color(0xFF382800),
    energyLow = Color(0xFFFF6B6B),
    energyLowContainer = Color(0xFF381010),
    energyRecovery = Color(0xFF818CF8),
    energyRecoveryContainer = Color(0xFF1E1B4B),
    energyDepleted = Color(0xFF6B7280),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textTertiary = Color(0xFF484F58),
    glowColor = Color(0x3300E699),
)

val AmoledExtendedColors = DarkExtendedColors.copy(
    surfaceSubtle = Color(0xFF000000),
    surfaceElevated = Color(0xFF0B0D12),
    borderMuted = Color(0xFF222634),
)

val LightExtendedColors = NeuroFlowExtendedColors(
    surfaceElevated = Color(0xFFF6F8FA),
    surfaceSubtle = Color(0xFFFFFFFF),
    borderMuted = Color(0xFFD0D7DE),
    energyPeak = Color(0xFF059669),
    energyPeakContainer = Color(0xFFD1FAE5),
    energyModerate = Color(0xFFD97706),
    energyModerateContainer = Color(0xFFFEF3C7),
    energyLow = Color(0xFFE11D48),
    energyLowContainer = Color(0xFFFFE4E6),
    energyRecovery = Color(0xFF6366F1),
    energyRecoveryContainer = Color(0xFFEEF2FF),
    energyDepleted = Color(0xFF9CA3AF),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF656D76),
    textTertiary = Color(0xFF8C959F),
    glowColor = Color(0x1A059669),
)

val LocalNeuroFlowColors = staticCompositionLocalOf { DarkExtendedColors }

fun phaseAccentColor(phase: EnergyPhase, colors: NeuroFlowExtendedColors): Color = when (phase) {
    EnergyPhase.PEAK -> colors.energyPeak
    EnergyPhase.REBOUND -> colors.energyPeak.copy(alpha = 0.85f)
    EnergyPhase.DIP -> colors.energyModerate
    EnergyPhase.WIND_DOWN -> colors.energyDepleted
    EnergyPhase.RECOVERY_NEEDED -> colors.energyRecovery
}

@Composable
@ReadOnlyComposable
fun phaseAccentColor(phase: EnergyPhase): Color = phaseAccentColor(phase, LocalNeuroFlowColors.current)

val NeuroFlowTypographyV2 = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Reactive semantic token bridge: existing V2 composables referencing NeuroFlowThemeV2Colors
 * automatically adapt to Light, Dark, and AMOLED modes without breaking call sites.
 */
object NeuroFlowThemeV2Colors {
    // Static base tokens (for non-composable JVM tests, DrawScopes, or outside CompositionLocal)
    val StaticObsidian = Color(0xFF0A0B0E)
    val StaticSlateSurface = Color(0xFF13151B)
    val StaticElectricIndigo = Color(0xFF6366F1)
    val StaticTextPrimary = Color(0xFFF8FAFC)
    val StaticTextSecondary = Color(0xFF94A3B8)

    val current: NeuroFlowSemanticPalette
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowPalette.current

    val Obsidian: Color
        @Composable @ReadOnlyComposable get() = current.background
    val SlateSurface: Color
        @Composable @ReadOnlyComposable get() = current.surface
    val SlateSurfaceAlt: Color
        @Composable @ReadOnlyComposable get() = current.surfaceElevated
    val SlateBorder: Color
        @Composable @ReadOnlyComposable get() = current.border
    val SlateDivider: Color
        @Composable @ReadOnlyComposable get() = current.border.copy(alpha = 0.6f)

    // Bioluminescent Accents
    val ElectricIndigo: Color
        @Composable @ReadOnlyComposable get() = current.primaryIndigo
    val ElectricIndigoDark = Color(0xFF4F46E5)
    val ElectricIndigoGlow: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyRecovery

    val BioMint: Color
        @Composable @ReadOnlyComposable get() = current.energyPeakMint
    val SageGreen: Color
        @Composable @ReadOnlyComposable get() = current.energyPeakMint
    val SageGreenDark = Color(0xFF059669)
    val SageGreenContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyPeakContainer

    val SolarAmber: Color
        @Composable @ReadOnlyComposable get() = current.energyModerateAmber
    val WarmAmber: Color
        @Composable @ReadOnlyComposable get() = current.energyModerateAmber
    val WarmAmberContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyModerateContainer

    val PulseCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val MutedCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val RoseCoral: Color
        @Composable @ReadOnlyComposable get() = current.energyLowCoral
    val RoseCoralContainer: Color
        @Composable @ReadOnlyComposable get() = LocalNeuroFlowColors.current.energyLowContainer
    val DeepViolet: Color
        @Composable @ReadOnlyComposable get() = current.hyperFocusViolet

    // Neutral Typography
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = current.textSecondary
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = current.textTertiary
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

    val extendedColors = when {
        !darkTheme -> LightExtendedColors
        amoledDark -> AmoledExtendedColors
        else -> DarkExtendedColors
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
        LocalNeuroFlowColors provides extendedColors,
        LocalIsDarkTheme provides darkTheme // LocalIsDarkTheme provides true
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeuroFlowTypographyV2,
            content = content
        )
    }
}
