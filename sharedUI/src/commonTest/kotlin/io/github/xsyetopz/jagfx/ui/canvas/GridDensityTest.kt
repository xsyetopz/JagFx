package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.ui.screen.timeTicks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridDensityTest {
    @Test
    fun theValueGridHasTwoRowsAndThreeLines() {
        assertEquals(2, VALUE_ROWS)
        assertEquals(listOf(0f, 0.5f, 1f), valueGridFractions())
    }

    @Test
    fun timeTicksAreAtLeast120PixelsApart() {
        assertEquals(120f, MIN_TICK_SPACING)
        val plotWidth = 1000f
        val ticks = timeTicks(1000, 0f..1f, plotWidth, MIN_TICK_SPACING)
        assertTrue(ticks.size <= 9, "got $ticks")
        ticks.zipWithNext().forEach { (a, b) ->
            assertTrue((b - a) / 1000f * plotWidth >= MIN_TICK_SPACING, "got $ticks")
        }
    }

    @Test
    fun theAnalysisGridDrawsOneLineAtEachOctaveOfTheAxis() {
        val octaves = octaveSemitones()
        assertEquals((0..96 step 12).toList(), octaves)
        assertEquals(FilterFrequencyScale.OCTAVES + 1, octaves.size)
    }
}
