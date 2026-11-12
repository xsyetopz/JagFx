package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.input.key.Key
import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.SynthLimits

/**
 * The result of a key in the pole-zero plot: [selection] is the marker to select, and
 * [coefficient] its new value when the key moved it, `null` when it only changed the selection.
 */
internal data class PoleKeyEdit(
    val selection: PoleRef,
    val coefficient: Coefficient?,
)

/**
 * What [key] does with [selected] among [refs]. An arrow without [nudge] (Shift held) selects the
 * previous (Left, Up) or next (Right, Down) marker and wraps around; with nothing selected, next
 * picks the first and previous the last. With [nudge] it moves the selected marker one snap step:
 * a semitone for Left and Right, 1/32 of the magnitude range for Down and Up. [coefficientOf]
 * reads a marker's current value. Returns `null` for any other key, when [refs] is empty, or when
 * there is nothing selected to nudge.
 */
internal fun poleKeyEdit(
    key: Key,
    nudge: Boolean,
    refs: List<PoleRef>,
    selected: PoleRef?,
    coefficientOf: (PoleRef) -> Coefficient,
): PoleKeyEdit? {
    val direction = keyDirection(key)
    return when {
        direction == null || refs.isEmpty() -> null
        !nudge -> PoleKeyEdit(cycled(refs, selected, direction.step), null)
        selected == null -> null
        else -> PoleKeyEdit(selected, nudged(coefficientOf(selected), direction))
    }
}

private enum class PoleKeyDirection(
    val step: Int,
    val semitones: Int,
    val magnitudeSteps: Int,
) {
    Left(-1, -1, 0),
    Right(1, 1, 0),
    Up(-1, 0, 1),
    Down(1, 0, -1),
}

private fun keyDirection(key: Key): PoleKeyDirection? =
    when (key) {
        Key.DirectionLeft -> PoleKeyDirection.Left
        Key.DirectionRight -> PoleKeyDirection.Right
        Key.DirectionUp -> PoleKeyDirection.Up
        Key.DirectionDown -> PoleKeyDirection.Down
        else -> null
    }

private fun cycled(
    refs: List<PoleRef>,
    selected: PoleRef?,
    step: Int,
): PoleRef {
    val index = refs.indexOf(selected)
    return when {
        index >= 0 -> refs[(index + step).mod(refs.size)]
        step > 0 -> refs.first()
        else -> refs.last()
    }
}

private fun nudged(
    from: Coefficient,
    direction: PoleKeyDirection,
): Coefficient {
    val step = SynthLimits.U16_MAX / PoleZeroGeometry.MAGNITUDE_SNAP_DIVISIONS
    return Coefficient(
        if (direction.semitones == 0) {
            from.phase
        } else {
            SemitoneScale.stepRawPhase(from.phase, direction.semitones)
        },
        if (direction.magnitudeSteps == 0) {
            from.magnitude
        } else {
            PoleZeroGeometry.snapRawMagnitude(from.magnitude + direction.magnitudeSteps * step)
        },
    )
}
