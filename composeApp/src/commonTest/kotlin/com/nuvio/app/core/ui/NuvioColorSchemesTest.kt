package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NuvioColorSchemesTest {
    private val palettes: List<Pair<AppTheme, ThemeColorPalette>> =
        AppTheme.entries.map { it to ThemeColors.getColorPalette(it) }

    @Test
    fun lightSchemeKeepsAccentsReadable() {
        palettes.forEach { (theme, palette) ->
            val s = buildNuvioColorScheme(palette, darkTheme = false)
            fun check(label: String, fg: Color, bg: Color, min: Float) {
                val ratio = contrastRatio(fg, bg)
                assertTrue(ratio >= min, "$theme light $label contrast $ratio < $min")
            }
            check("onPrimary/primary", s.onPrimary, s.primary, 4.5f)
            check("primary/background", s.primary, s.background, 4.5f)
            check("onSecondary/secondary", s.onSecondary, s.secondary, 4.5f)
            check("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5f)
            check("onSecondaryContainer/secondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5f)
            check("onSurface/surface", s.onSurface, s.surface, 7f)
            check("onSurfaceVariant/surface", s.onSurfaceVariant, s.surface, 4.5f)
            check("onError/error", s.onError, s.error, 4.5f)
        }
    }

    @Test
    fun lightSchemeIsActuallyLight() {
        palettes.forEach { (theme, palette) ->
            val s = buildNuvioColorScheme(palette, darkTheme = false)
            assertTrue(s.background.luminance() > 0.8f, "$theme light background is not light")
            assertTrue(s.surface.luminance() > 0.8f, "$theme light surface is not light")
        }
    }

    @Test
    fun darkSchemeKeepsEstablishedValues() {
        palettes.forEach { (theme, palette) ->
            val s = buildNuvioColorScheme(palette, darkTheme = true)
            assertEquals(palette.secondary, s.primary, "$theme primary")
            assertEquals(palette.secondaryVariant, s.secondary, "$theme secondary")
            assertEquals(palette.background, s.background, "$theme background")
            assertEquals(palette.backgroundElevated, s.surface, "$theme surface")
            assertEquals(palette.backgroundCard, s.surfaceVariant, "$theme surfaceVariant")
            assertEquals(Color(0xFFF5F7F8), s.onSurface, "$theme onSurface")
            assertEquals(Color(0xFF969CA3), s.onSurfaceVariant, "$theme onSurfaceVariant")
            assertEquals(Color(0xFF252A2A), s.outline, "$theme outline")
        }
    }

    @Test
    fun amoledOnlyAffectsDarkMode() {
        palettes.forEach { (theme, palette) ->
            assertEquals(Color.Black, buildNuvioColorScheme(palette, darkTheme = true, amoled = true).background, "$theme")
            assertEquals(
                buildNuvioColorScheme(palette, darkTheme = false, amoled = false).background,
                buildNuvioColorScheme(palette, darkTheme = false, amoled = true).background,
                "$theme light must ignore AMOLED",
            )
            assertNotEquals(Color.Black, buildNuvioColorScheme(palette, darkTheme = false, amoled = true).background)
        }
    }

    @Test
    fun darkTokensMatchPreviousHardCodedValues() {
        palettes.forEach { (theme, palette) ->
            val scheme = buildNuvioColorScheme(palette, darkTheme = true)
            val c = defaultNuvioThemeTokens(palette, amoled = false, colorScheme = scheme, darkTheme = true).colors
            assertEquals(palette.secondary, c.accent, "$theme accent")
            assertEquals(palette.secondaryVariant, c.accentStrong, "$theme accentStrong")
            assertEquals(palette.onSecondary, c.onAccent, "$theme onAccent")
            assertEquals(palette.focusRing, c.focusRing, "$theme focusRing")
            assertEquals(palette.focusBackground, c.focusBackground, "$theme focusBackground")
            assertEquals(palette.background, c.background, "$theme background")
            assertEquals(palette.backgroundElevated, c.surface, "$theme surface")
            assertEquals(palette.backgroundCard, c.surfaceCard, "$theme surfaceCard")
            assertEquals(Color(0xFFF5F7F8), c.textPrimary, "$theme textPrimary")
            assertEquals(Color(0xFF969CA3), c.textMuted, "$theme textMuted")
            assertEquals(Color(0xFF252A2A), c.borderDefault, "$theme borderDefault")
            assertEquals(Color(0xFF3A4040), c.borderStrong, "$theme borderStrong")
            assertEquals(palette.accentGradient, c.accentGradient, "$theme accentGradient")
        }
    }

    @Test
    fun playerTokensStayCinematicInLightMode() {
        palettes.forEach { (theme, palette) ->
            val scheme = buildNuvioColorScheme(palette, darkTheme = false)
            val c = defaultNuvioThemeTokens(palette, amoled = false, colorScheme = scheme, darkTheme = false).colors
            assertEquals(Color.White, c.playerControlsForeground, "$theme")
            assertEquals(palette.secondary, c.playerTimelineFill, "$theme")
            assertEquals(Color.Black.copy(alpha = 0.72f), c.playerControlsBackground, "$theme")
        }
    }

    @Test
    fun lightAccentGradientsStayVisible() {
        palettes.forEach { (theme, palette) ->
            val scheme = buildNuvioColorScheme(palette, darkTheme = false)
            val c = defaultNuvioThemeTokens(palette, amoled = false, colorScheme = scheme, darkTheme = false).colors
            c.accentGradient.forEach {
                assertTrue(contrastRatio(it, c.background) >= 3f, "$theme gradient stop too pale on light")
            }
        }
    }
}
