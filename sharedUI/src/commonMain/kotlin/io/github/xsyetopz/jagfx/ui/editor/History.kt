package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Patch

/**
 * Undo and redo stacks of patches. Only patch changes enter history.
 *
 * Gesture (drag on canvas or slider) groups its edits into one undo step: while
 * [gestureDepth] is above `0`, edits are not recorded, and when outermost gesture ends,
 * [gestureStart] is recorded if patch changed. Undo and redo do nothing during gesture.
 *
 * @property past Older patches, most recent last, at most [CAPACITY].
 * @property future Patches that redo restores, most recent last.
 */
internal data class History(
    val past: List<Patch> = emptyList(),
    val future: List<Patch> = emptyList(),
    val gestureDepth: Int = 0,
    val gestureStart: Patch? = null,
) {
    val canUndo: Boolean get() = gestureDepth == 0 && past.isNotEmpty()

    val canRedo: Boolean get() = gestureDepth == 0 && future.isNotEmpty()

    /** Records that patch changed from [before]. Inside gesture, records nothing. */
    fun record(before: Patch): History =
        if (gestureDepth > 0) this else copy(past = push(past, before), future = emptyList())

    fun beginGesture(current: Patch): History =
        copy(
            gestureDepth = gestureDepth + 1,
            gestureStart = if (gestureDepth == 0) current else gestureStart,
        )

    /** Ends gesture. Ending outermost gesture records its start if [current] differs. */
    fun endGesture(current: Patch): History {
        val start = gestureStart
        return when {
            gestureDepth == 0 -> {
                this
            }

            gestureDepth > 1 -> {
                copy(gestureDepth = gestureDepth - 1)
            }

            start == null || start == current -> {
                copy(gestureDepth = 0, gestureStart = null)
            }

            else -> {
                History(past = push(past, start), future = emptyList())
            }
        }
    }

    /** Returns patch to restore and new history, or `null` when undo is unavailable. */
    fun undo(current: Patch): Pair<Patch, History>? =
        if (canUndo) {
            past.last() to copy(past = past.dropLast(1), future = future + current)
        } else {
            null
        }

    /** Returns patch to restore and new history, or `null` when redo is unavailable. */
    fun redo(current: Patch): Pair<Patch, History>? =
        if (canRedo) {
            future.last() to copy(past = push(past, current), future = future.dropLast(1))
        } else {
            null
        }

    companion object {
        const val CAPACITY: Int = 200

        private fun push(
            stack: List<Patch>,
            patch: Patch,
        ): List<Patch> = (stack + patch).takeLast(CAPACITY)
    }
}
