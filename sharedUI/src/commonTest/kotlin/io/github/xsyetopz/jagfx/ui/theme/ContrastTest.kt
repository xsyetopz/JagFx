package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.github.xsyetopz.jagfx.ui.canvas.RAIL_ALPHA
import io.github.xsyetopz.jagfx.ui.canvas.SECTION_TRACE_ALPHA
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
        name: String = "",
    ) = if (appearance == JagFxAppearance.HighContrast &&
        name !in HIGH_EXEMPT
    ) {
        HIGH_TEXT
    } else {
        normal
    }

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

    /** Every palette color drawn as line, marker, or trace; [SynthPalette.grid] and fills are checked apart. */
    private fun SynthPalette.graphics() =
        mapOf(
            "envelope" to envelope,
            "marker" to marker,
            "output" to output,
            "filter" to filter,
            "zero" to zero,
            "pole" to pole,
            "midLine" to midLine,
            "loop" to loop,
            "playhead" to playhead,
            "inactive" to inactive,
        )

    @Test
    fun plotLinesAndMarkersReachThreeToOneOnTheScope() {
        for (appearance in JagFxAppearance.entries) {
            val palette = SynthPalette.of(appearance)
            val scope = mapOf("scope" to palette.scope)
            for ((name, color) in palette.graphics()) {
                check(appearance, name, color, scope, floor(appearance, NON_TEXT_MIN, name))
            }
            check(appearance, "scopeText", palette.scopeText, scope, floor(appearance, TEXT_MIN))
        }
    }

    @Test
    fun translucentPlotColorsReachThreeToOneOnTheScope() {
        for (appearance in JagFxAppearance.entries) {
            val palette = SynthPalette.of(appearance)
            val scope = mapOf("scope" to palette.scope)
            val blended =
                mapOf(
                    "filter at section alpha" to palette.filter.copy(alpha = SECTION_TRACE_ALPHA),
                    "zero rail" to palette.zero.copy(alpha = RAIL_ALPHA),
                    "pole rail" to palette.pole.copy(alpha = RAIL_ALPHA),
                )
            for ((name, color) in blended) {
                check(appearance, name, color.compositeOver(palette.scope), scope, NON_TEXT_MIN)
            }
        }
    }

    @Test
    fun markerRingReachesThreeToOneOnTheSelectionFill() {
        for (appearance in JagFxAppearance.entries) {
            val palette = SynthPalette.of(appearance)
            val fill = palette.selection.compositeOver(palette.scope)
            val onFill = mapOf("selection fill" to fill)
            check(appearance, "marker", palette.marker, onFill, NON_TEXT_MIN)
            check(appearance, "envelope", palette.envelope, onFill, NON_TEXT_MIN)
            check(appearance, "zero", palette.zero, onFill, NON_TEXT_MIN)
            check(appearance, "pole", palette.pole, onFill, NON_TEXT_MIN)
        }
    }

    @Test
    fun gridStaysBelowTheLinesItSupports() {
        for (appearance in JagFxAppearance.entries) {
            val palette = SynthPalette.of(appearance)
            assertTrue(
                contrastRatio(palette.grid, palette.scope) <
                    contrastRatio(palette.envelope, palette.scope),
                "$appearance grid must not outshine envelope lines",
            )
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
        val HIGH_EXEMPT = setOf("midLine", "inactive")
    }
}
