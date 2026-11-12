package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {
    private fun JagFxColors.surfaces() =
        mapOf(
            "root" to root,
            "header" to header,
            "panel" to panel,
            "footer" to footer,
            "cell" to cell,
            "cellSelected" to cellSelected,
            "input" to input,
            "button" to button,
        )

    private fun check(
        appearance: JagFxAppearance,
        what: String,
        foreground: Color,
        backgrounds: Map<String, Color>,
        minimum: Float,
    ) {
        for ((name, background) in backgrounds) {
            val ratio = contrastRatio(foreground, background)
            println(
                "CONTRAST $appearance $what on $name = ${(ratio * 100).roundToInt() / 100f} (min $minimum)",
            )
            assertTrue(ratio >= minimum, "$appearance $what on $name is $ratio, below $minimum")
        }
    }

    private fun floor(
        appearance: JagFxAppearance,
        normal: Float,
    ) = if (appearance == JagFxAppearance.HighContrast) HIGH_TEXT else normal

    @Test
    fun luminanceMatchesTheWcagReferencePoints() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), 0.001f)
        assertEquals(
            contrastRatio(Color.Black, Color.White),
            contrastRatio(Color.White, Color.Black),
        )
    }

    @Test
    fun textReachesFourPointFiveToOneOnItsBackgrounds() {
        for (appearance in JagFxAppearance.entries) {
            val colors = JagFxColors.of(appearance)
            val minimum = floor(appearance, TEXT_MIN)
            val surfaces = colors.surfaces()
            val withHover = surfaces + ("buttonHover" to colors.buttonHover)
            check(appearance, "text", colors.text, withHover, minimum)
            check(appearance, "textButton", colors.textButton, withHover, minimum)
            check(appearance, "textDim", colors.textDim, surfaces, minimum)
            check(appearance, "textSubtle", colors.textSubtle, surfaces, minimum)
            check(appearance, "warning", colors.warning, surfaces, minimum)
            check(
                appearance,
                "accent",
                colors.accent,
                withHover + ("buttonActive" to colors.buttonActive),
                minimum,
            )
            check(
                appearance,
                "text",
                colors.text,
                mapOf("buttonActive" to colors.buttonActive),
                minimum,
            )
        }
    }

    @Test
    fun darkSubtleTextIsAtLeast8A8A8AOnPanel() {
        val subtle = JagFxColors.Classic.textSubtle
        assertTrue(
            subtle.red >= 0x8A / 255f && subtle.green >= 0x8A / 255f && subtle.blue >= 0x8A / 255f,
        )
        assertTrue(contrastRatio(subtle, Color(0xFF1E1E1E)) >= TEXT_MIN)
    }

    @Test
    fun plotLinesAndMarkersReachThreeToOneOnTheScope() {
        for (appearance in JagFxAppearance.entries) {
            val palette = SynthPalette.of(appearance)
            val minimum = floor(appearance, NON_TEXT_MIN)
            val scope = mapOf("scope" to palette.scope)
            check(appearance, "envelope", palette.envelope, scope, minimum)
            check(appearance, "marker", palette.marker, scope, minimum)
            check(appearance, "output", palette.output, scope, minimum)
            check(appearance, "filter", palette.filter, scope, minimum)
            check(appearance, "zero", palette.zero, scope, minimum)
            check(appearance, "pole", palette.pole, scope, minimum)
            check(appearance, "midLine", palette.midLine, scope, NON_TEXT_MIN)
            check(appearance, "loop", palette.loop, scope, minimum)
            check(appearance, "playhead", palette.playhead, scope, minimum)
            check(appearance, "scopeText", palette.scopeText, scope, floor(appearance, TEXT_MIN))
        }
    }

    @Test
    fun controlBordersAndFocusRingReachThreeToOneOnTheirSurroundings() {
        for (appearance in JagFxAppearance.entries) {
            val colors = JagFxColors.of(appearance)
            val minimum = floor(appearance, NON_TEXT_MIN)
            val surroundings = colors.surfaces() + ("buttonHover" to colors.buttonHover)
            check(appearance, "controlBorder", colors.controlBorder, surroundings, minimum)
            check(appearance, "focusRing", colors.accent, surroundings, minimum)
        }
    }

    private companion object {
        const val TEXT_MIN = 4.5f
        const val NON_TEXT_MIN = 3f
        const val HIGH_TEXT = 7f
    }
}
