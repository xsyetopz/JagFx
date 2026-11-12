package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.input.key.Key
import io.github.xsyetopz.jagfx.core.model.Coefficient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoleZeroKeysTest {
    private val a = PoleRef(0, 0, 0)
    private val b = PoleRef(0, 0, 1)
    private val c = PoleRef(1, 0, 0)
    private val refs = listOf(a, b, c)
    private val values =
        mutableMapOf(
            a to Coefficient(20000, 10000),
            b to Coefficient(0, 0),
            c to Coefficient(65535, 65535),
        )

    private fun edit(
        key: Key,
        selected: PoleRef?,
        nudge: Boolean = false,
        list: List<PoleRef> = refs,
    ) = poleKeyEdit(key, nudge, list, selected) { values.getValue(it) }

    @Test
    fun arrowsCycleThroughEveryMarkerAndWrap() {
        assertEquals(b, edit(Key.DirectionRight, a)?.selection)
        assertEquals(b, edit(Key.DirectionDown, a)?.selection)
        assertEquals(a, edit(Key.DirectionRight, c)?.selection)
        assertEquals(c, edit(Key.DirectionLeft, a)?.selection)
        assertEquals(a, edit(Key.DirectionUp, b)?.selection)
        assertNull(edit(Key.DirectionRight, a)?.coefficient)
    }

    @Test
    fun withNothingSelectedNextPicksTheFirstAndPreviousTheLast() {
        assertEquals(a, edit(Key.DirectionRight, null)?.selection)
        assertEquals(c, edit(Key.DirectionLeft, null)?.selection)
    }

    @Test
    fun otherKeysAndEmptyPlotsDoNothing() {
        assertNull(edit(Key.A, a))
        assertNull(edit(Key.A, a, nudge = true))
        assertNull(edit(Key.DirectionRight, null, list = emptyList()))
    }

    @Test
    fun nudgingWithNothingSelectedDoesNothing() {
        assertNull(edit(Key.DirectionRight, null, nudge = true))
    }

    @Test
    fun shiftLeftRightMovesOneSemitoneAndKeepsTheMagnitude() {
        val later = edit(Key.DirectionRight, a, nudge = true)?.coefficient
        val earlier = edit(Key.DirectionLeft, a, nudge = true)?.coefficient
        assertTrue(later != null && earlier != null)
        assertTrue(later.phase > 20000 && earlier.phase < 20000)
        assertEquals(10000, later.magnitude)
        assertEquals(a, edit(Key.DirectionRight, a, nudge = true)?.selection)
    }

    @Test
    fun shiftUpDownMovesOneMagnitudeStepOnTheGrid() {
        val up = edit(Key.DirectionUp, a, nudge = true)?.coefficient
        val down = edit(Key.DirectionDown, a, nudge = true)?.coefficient
        assertTrue(up != null && down != null)
        assertEquals(20000, up.phase)
        assertEquals(PoleZeroGeometry.snapRawMagnitude(up.magnitude), up.magnitude)
        assertEquals(PoleZeroGeometry.snapRawMagnitude(down.magnitude), down.magnitude)
        assertTrue(up.magnitude > 10000 && down.magnitude < 10000)
    }

    @Test
    fun nudgesStopAtTheEdgesOfTheRange() {
        assertEquals(65535, edit(Key.DirectionUp, c, nudge = true)?.coefficient?.magnitude)
        assertEquals(65535, edit(Key.DirectionRight, c, nudge = true)?.coefficient?.phase)
        assertEquals(0, edit(Key.DirectionDown, b, nudge = true)?.coefficient?.magnitude)
        assertEquals(0, edit(Key.DirectionLeft, b, nudge = true)?.coefficient?.phase)
        assertNotEquals(0, edit(Key.DirectionRight, b, nudge = true)?.coefficient?.phase)
    }
}
