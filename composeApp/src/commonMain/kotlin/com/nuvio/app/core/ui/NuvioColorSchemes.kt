package com.nuvio.app.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG relative-luminance contrast ratio between two opaque colors. */
internal fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}

/** Darkens [color] toward black in small steps until it reaches [minRatio] against [against]. */
internal fun darkenUntilContrast(color: Color, against: Color, minRatio: Float): Color {
    var result = color
    var steps = 0
    while (contrastRatio(result, against) < minRatio && steps < 24) {
        result = lerp(result, Color.Black, 0.1f)
        steps++
    }
    return result
}

private val LightBackgroundBase = Color(0xFFFFFBFE)
private val LightElevatedBase = Color(0xFFF7F2FA)
private val LightCardBase = Color(0xFFF3EDF7)
private val LightOnSurface = Color(0xFF1D1B20)
private val LightOnSurfaceVariant = Color(0xFF5E5A66)
private val LightOutline = Color(0xFF79747E)
private val LightOutlineVariantBase = Color(0xFFCAC4D0)
private val LightNeutralAccent = Color(0xFF312F35)

private val DarkOnSurface = Color(0xFFF5F7F8)
private val DarkOnSurfaceVariant = Color(0xFF969CA3)

/**
 * Builds the Material 3 color scheme for [palette]. Dark output keeps the app's established
 * values; light output derives neutrals from the M3 baseline (lightly tinted by the accent)
 * and darkens accents until they pass contrast on light surfaces.
 * AMOLED only applies in dark mode.
 */
internal fun buildNuvioColorScheme(
    palette: ThemeColorPalette,
    darkTheme: Boolean,
    amoled: Boolean = false,
): ColorScheme = if (darkTheme) darkScheme(palette, amoled) else lightScheme(palette)

private fun darkScheme(palette: ThemeColorPalette, amoled: Boolean): ColorScheme {
    val background = if (amoled) Color.Black else palette.background
    val onPrimaryContainer =
        if (palette.focusBackground.luminance() > 0.5f) Color(0xFF111111) else Color(0xFFF5F7F8)
    return darkColorScheme(
        primary = palette.secondary,
        onPrimary = palette.onSecondary,
        primaryContainer = palette.focusBackground,
        onPrimaryContainer = onPrimaryContainer,
        secondary = palette.secondaryVariant,
        onSecondary = palette.onSecondaryVariant,
        background = background,
        onBackground = DarkOnSurface,
        surface = palette.backgroundElevated,
        onSurface = DarkOnSurface,
        surfaceVariant = palette.backgroundCard,
        onSurfaceVariant = DarkOnSurfaceVariant,
        // Existing code reads colorScheme.outline directly as its subtle border/divider color.
        outline = Color(0xFF252A2A),
        error = Color(0xFFE36A8A),
        onError = Color(0xFFFCE5EC),
        surfaceContainerLowest = background,
        surfaceContainerLow = palette.backgroundElevated,
        surfaceContainer = palette.backgroundCard,
        surfaceContainerHigh = lerp(palette.backgroundCard, DarkOnSurface, 0.04f),
        surfaceContainerHighest = lerp(palette.backgroundCard, DarkOnSurface, 0.08f),
    )
}

private fun lightScheme(palette: ThemeColorPalette): ColorScheme {
    val accent = palette.secondary
    val tint = 0.03f
    val background = lerp(LightBackgroundBase, accent, tint)
    val elevated = lerp(LightElevatedBase, accent, tint)
    val card = lerp(LightCardBase, accent, tint)

    // Near-white accents (e.g. the WHITE theme) become a strong neutral instead of mid-gray.
    val primary = if (accent.luminance() > 0.6f) {
        LightNeutralAccent
    } else {
        darkenUntilContrast(accent, background, 4.5f)
    }
    val primaryContainer = lerp(background, primary, 0.18f)
    val secondary = darkenUntilContrast(
        if (palette.secondaryVariant.luminance() > 0.6f) LightNeutralAccent else palette.secondaryVariant,
        background,
        4.5f,
    )
    val secondaryContainer = lerp(background, secondary, 0.14f)

    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = darkenUntilContrast(primary, primaryContainer, 7f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = darkenUntilContrast(secondary, secondaryContainer, 7f),
        background = background,
        onBackground = LightOnSurface,
        surface = elevated,
        onSurface = LightOnSurface,
        surfaceVariant = card,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = lerp(LightOutlineVariantBase, accent, tint),
        error = Color(0xFFB3261E),
        onError = Color.White,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = elevated,
        surfaceContainer = card,
        surfaceContainerHigh = lerp(card, LightOnSurface, 0.04f),
        surfaceContainerHighest = lerp(card, LightOnSurface, 0.08f),
    )
}
