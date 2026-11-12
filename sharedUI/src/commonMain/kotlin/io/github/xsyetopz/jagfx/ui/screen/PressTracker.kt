package io.github.xsyetopz.jagfx.ui.screen

/**
 * Turns press into selection only when pointer comes up inside cell it went down in
 * (WCAG 2.5.2 Pointer Cancellation). Leaving cell cancels press, even if pointer comes
 * back before it is released.
 */
internal class PressTracker {
    private var armed = false

    /** Press went down; [inside] says whether it is in cell. */
    fun down(inside: Boolean) {
        armed = inside
    }

    /** Pointer moved to point that is, or is not, [inside] cell. */
    fun move(inside: Boolean) {
        if (!inside) armed = false
    }

    /** Pointer came up. Returns true when cell should now be selected. */
    fun up(inside: Boolean): Boolean {
        val select = armed && inside
        armed = false
        return select
    }

    /** System cancelled gesture. */
    fun cancel() {
        armed = false
    }
}
