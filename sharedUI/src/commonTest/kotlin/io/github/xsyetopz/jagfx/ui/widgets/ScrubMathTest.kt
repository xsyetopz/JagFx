package io.github.xsyetopz.jagfx.ui.widgets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrubMathTest {
    private val range = 0f..200f

    @Test
    fun fractionMapsRangeAndClamps() {
        assertEquals(0.5f, ScrubMath.fraction(100f, range))
        assertEquals(0f, ScrubMath.fraction(-5f, range))
        assertEquals(1f, ScrubMath.fraction(500f, range))
        assertEquals(0f, ScrubMath.fraction(3f, 3f..3f))
    }

    @Test
    fun dragMovesAHundredthOfTheRangePerPixelFromTheAnchor() {
        assertEquals(120f, ScrubMath.dragValue(100f, 10f, range))
        assertEquals(80f, ScrubMath.dragValue(100f, -10f, range))
    }

    @Test
    fun dragClampsToTheRange() {
        assertEquals(200f, ScrubMath.dragValue(190f, 50f, range))
        assertEquals(0f, ScrubMath.dragValue(10f, -50f, range))
    }

    @Test
    fun wheelStepsByDirectionAndClamps() {
        assertEquals(6f, ScrubMath.wheelValue(5f, 1f, 1f, range))
        assertEquals(3f, ScrubMath.wheelValue(5f, -1f, 2f, range))
        assertEquals(5f, ScrubMath.wheelValue(5f, 0f, 2f, range))
        assertEquals(200f, ScrubMath.wheelValue(200f, 1f, 1f, range))
        assertEquals(0f, ScrubMath.wheelValue(0f, -1f, 1f, range))
    }

    @Test
    fun resetUsesDefaultOrMinimum() {
        assertEquals(50f, ScrubMath.resetValue(50f, range))
        assertEquals(200f, ScrubMath.resetValue(999f, range))
        assertEquals(10f, ScrubMath.resetValue(null, 10f..20f))
    }

    @Test
    fun parseClampedIntClampsAndRejectsText() {
        assertEquals(5, parseClampedInt("5", 0..10))
        assertEquals(5, parseClampedInt(" 5 ", 0..10))
        assertEquals(10, parseClampedInt("99", 0..10))
        assertEquals(0, parseClampedInt("-4", 0..10))
        assertEquals(10, parseClampedInt("99999999999999", 0..10))
        assertNull(parseClampedInt("", 0..10))
        assertNull(parseClampedInt("abc", 0..10))
        assertNull(parseClampedInt("1.5", 0..10))
    }
}
