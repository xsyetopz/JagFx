package io.github.xsyetopz.jagfx.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialSchemeTest {
    @Test
    fun schemeFollowsTheJagFxColors() {
        for (appearance in JagFxAppearance.entries) {
            val colors = JagFxColors.of(appearance)
            val scheme = jagFxColorScheme(appearance)
            assertEquals(colors.panel, scheme.surface)
            assertEquals(colors.root, scheme.background)
            assertEquals(colors.accent, scheme.primary)
            assertEquals(colors.controlBorder, scheme.outline)
            assertEquals(colors.text, scheme.onSurface)
        }
    }

    @Test
    fun schemePairsReachTheirContrast() {
        for (appearance in JagFxAppearance.entries) {
            val scheme = jagFxColorScheme(appearance)
            val text = contrastRatio(scheme.onSurface, scheme.surface)
            val primary = contrastRatio(scheme.primary, scheme.surface)
            val outline = contrastRatio(scheme.outline, scheme.surface)
            val onPrimary = contrastRatio(scheme.onPrimary, scheme.primary)
            assertTrue(text >= 4.5f, "$appearance onSurface on surface is $text")
            assertTrue(primary >= 3f, "$appearance primary on surface is $primary")
            assertTrue(outline >= 3f, "$appearance outline on surface is $outline")
            assertTrue(onPrimary >= 4.5f, "$appearance onPrimary on primary is $onPrimary")
        }
    }
}
