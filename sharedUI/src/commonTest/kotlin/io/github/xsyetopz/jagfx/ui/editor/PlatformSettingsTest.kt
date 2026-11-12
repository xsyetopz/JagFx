package io.github.xsyetopz.jagfx.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformSettingsTest {
    @Test
    fun systemThemeFollowsPlatformHighContrast() {
        assertEquals(ThemePreference.HighContrast, resolveTheme(ThemePreference.System, true))
        assertEquals(ThemePreference.System, resolveTheme(ThemePreference.System, false))
    }

    @Test
    fun explicitThemeWinsOverPlatform() {
        val explicit =
            listOf(ThemePreference.Classic, ThemePreference.Light, ThemePreference.HighContrast)
        for (theme in explicit) {
            assertEquals(theme, resolveTheme(theme, true))
        }
    }

    @Test
    fun textScaleMultipliesAndClamps() {
        assertEquals(100, resolveTextScale(100, 100))
        assertEquals(150, resolveTextScale(100, 150))
        assertEquals(150, resolveTextScale(150, 80))
        assertEquals(200, resolveTextScale(150, 200))
    }
}
