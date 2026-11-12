package io.github.xsyetopz.jagfx.ui.screen

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PressTrackerTest {
    @Test
    fun upInsideSelects() {
        val tracker = PressTracker()
        tracker.down(inside = true)
        assertTrue(tracker.up(inside = true))
    }

    @Test
    fun leavingThenUpDoesNotSelect() {
        val tracker = PressTracker()
        tracker.down(true)
        tracker.move(inside = false)
        tracker.move(inside = true)
        assertFalse(tracker.up(true))
    }

    @Test
    fun downOnlySelectsNothing() {
        val tracker = PressTracker()
        tracker.down(true)
        assertFalse(tracker.up(inside = false))
        assertFalse(PressTracker().up(true))
    }

    @Test
    fun dragThatEndsOutsideDoesNotSelect() {
        val tracker = PressTracker()
        tracker.down(true)
        tracker.move(true)
        tracker.move(false)
        assertFalse(tracker.up(false))
    }

    @Test
    fun dragThatEndsInsideSelectsOnce() {
        val tracker = PressTracker()
        tracker.down(true)
        tracker.move(true)
        assertTrue(tracker.up(true))
        assertFalse(tracker.up(true))
    }

    @Test
    fun cancelAndOutsideDownSelectNothing() {
        val tracker = PressTracker()
        tracker.down(true)
        tracker.cancel()
        assertFalse(tracker.up(true))
        tracker.down(false)
        assertFalse(tracker.up(true))
    }
}
