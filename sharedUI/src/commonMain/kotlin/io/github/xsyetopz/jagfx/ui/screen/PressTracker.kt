package io.github.xsyetopz.jagfx.ui.screen

/**
 * Turns a press into a selection only when the pointer comes up inside the cell it went down in
 * (WCAG 2.5.2 Pointer Cancellation). Leaving the cell cancels the press, even if the pointer comes
 * back before it is released.
 */
internal class PressTracker {
    private var armed = false

    /** A press went down; [inside] says whether it is in the cell. */
    fun down(inside: Boolean) {
        armed = inside
    }

    /** The pointer moved to a point that is, or is not, [inside] the cell. */
    fun move(inside: Boolean) {
        if (!inside) armed = false
    }

    /** The pointer came up. Returns true when the cell should now be selected. */
    fun up(inside: Boolean): Boolean {
        val select = armed && inside
        armed = false
        return select
    }

    /** The system cancelled the gesture. */
    fun cancel() {
        armed = false
    }
}
