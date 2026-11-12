package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.input.key.Key
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits

/** Raw units a Shift arrow moves a point: 1/64 of the voice or of the level range. */
internal const val KEY_STEP: Int = 1024

/** Raw units a Ctrl or Cmd arrow moves a point. */
internal const val KEY_FINE_STEP: Int = 64

/** What a key does in a focused envelope cell. */
internal enum class EnvelopeKeyCommand {
    SelectPrevious,
    SelectNext,
    SelectFirst,
    SelectLast,
    Add,
    Delete,
    MoveEarlier,
    MoveLater,
    MoveUp,
    MoveDown,
}

/** The result of a key: [segments] is `null` when only the selection changes. */
internal data class KeyEdit(
    val segments: List<Segment>?,
    val selection: Int,
)

/**
 * The command of [key]: an arrow selects the previous or next point (Left, Right) and with
 * [move] (Shift, Ctrl, or Cmd held) moves it; Home and End select the ends; Insert, `+`
 * add a point after the selected one; Delete and Backspace remove it.
 */
internal fun envelopeKeyCommand(
    key: Key,
    move: Boolean,
): EnvelopeKeyCommand? =
    when (key) {
        Key.DirectionLeft -> {
            if (move) EnvelopeKeyCommand.MoveEarlier else EnvelopeKeyCommand.SelectPrevious
        }

        Key.DirectionRight -> {
            if (move) EnvelopeKeyCommand.MoveLater else EnvelopeKeyCommand.SelectNext
        }

        Key.DirectionUp -> {
            if (move) EnvelopeKeyCommand.MoveUp else null
        }

        Key.DirectionDown -> {
            if (move) EnvelopeKeyCommand.MoveDown else null
        }

        Key.MoveHome -> {
            EnvelopeKeyCommand.SelectFirst
        }

        Key.MoveEnd -> {
            EnvelopeKeyCommand.SelectLast
        }

        Key.Insert, Key.Plus, Key.NumPadAdd -> {
            EnvelopeKeyCommand.Add
        }

        Key.Delete, Key.Backspace -> {
            EnvelopeKeyCommand.Delete
        }

        else -> {
            null
        }
    }

/**
 * What [command] does to [segments] with breakpoint [selected] selected (-1 for none), moving by
 * [step] raw units; `null` when it does nothing, so the key stays free for the cell navigation.
 * A move keeps the point between its neighbours in time, and breakpoint 0 stays at time 0.
 */
internal fun envelopeKeyEdit(
    command: EnvelopeKeyCommand,
    segments: List<Segment>,
    selected: Int,
    step: Int,
): KeyEdit? {
    val has = selected in segments.indices
    return when (command) {
        EnvelopeKeyCommand.Add -> {
            insertAfter(segments, if (has) selected else segments.lastIndex)
        }

        EnvelopeKeyCommand.Delete -> {
            removeSegment(segments, selected)?.let { KeyEdit(it, minOf(selected, it.lastIndex)) }
        }

        EnvelopeKeyCommand.MoveEarlier,
        EnvelopeKeyCommand.MoveLater,
        EnvelopeKeyCommand.MoveUp,
        EnvelopeKeyCommand.MoveDown,
        -> {
            if (has) moveBy(segments, selected, command, step) else null
        }

        else -> {
            selectEdit(command, segments.lastIndex, selected)
        }
    }
}

private fun selectEdit(
    command: EnvelopeKeyCommand,
    last: Int,
    selected: Int,
): KeyEdit? {
    val has = selected in 0..last
    val target =
        when (command) {
            EnvelopeKeyCommand.SelectFirst -> 0
            EnvelopeKeyCommand.SelectLast -> last
            EnvelopeKeyCommand.SelectNext -> if (has) selected + 1 else 0
            else -> if (has) selected - 1 else last
        }
    return KeyEdit(null, target).takeIf { target in 0..last }
}

private fun moveBy(
    segments: List<Segment>,
    index: Int,
    command: EnvelopeKeyCommand,
    step: Int,
): KeyEdit? {
    val deltaPosition =
        when (command) {
            EnvelopeKeyCommand.MoveEarlier -> -step
            EnvelopeKeyCommand.MoveLater -> step
            else -> 0
        }
    val deltaLevel =
        when (command) {
            EnvelopeKeyCommand.MoveUp -> step
            EnvelopeKeyCommand.MoveDown -> -step
            else -> 0
        }
    val current = segments[index]
    val level = (current.targetLevel + deltaLevel).coerceIn(0, SynthLimits.U16_MAX)
    var moved = current.copy(targetLevel = level)
    if (deltaPosition != 0 && index > 0) {
        val position = (current.duration + deltaPosition).coerceIn(positionBounds(segments, index))
        moved = moved.copy(duration = position)
    }
    return if (moved == current) {
        null
    } else {
        KeyEdit(segments.toMutableList().apply { set(index, moved) }, index)
    }
}

/** A point halfway between breakpoint [index] and the next one (or the end of the voice). */
private fun insertAfter(
    segments: List<Segment>,
    index: Int,
): KeyEdit? {
    if (segments.size >= SynthLimits.MAX_SEGMENTS || index !in segments.indices) return null
    val from = if (index == 0) 0 else segments[index].duration
    val next = segments.getOrNull(index + 1)
    val to = next?.duration ?: SynthLimits.U16_MAX
    val level = ((segments[index].targetLevel + (next ?: segments[index]).targetLevel) / 2)
    val added = Segment((from + to) / 2, level)
    return KeyEdit(segments.toMutableList().apply { add(index + 1, added) }, index + 1)
}
