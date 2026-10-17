package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Ruler steps in ms are 1, 2, and [HALF_DECADE] times a power of ten. */
private const val HALF_DECADE = 5
private val RulerMantissas = listOf(1, 2, HALF_DECADE)
private const val DECIMAL = 10.0

/** The time span in ms the tone bars share: the latest voice end or loop end, at least 1. */
internal fun timelineSpan(patch: Patch): Int {
    val voiceEnd = patch.voices.maxOf { (it?.offset?.value ?: 0) + (it?.duration?.value ?: 0) }
    return maxOf(voiceEnd, patch.loop.end.value, 1)
}

/** What a drag on a voice bar changes. */
internal enum class LaneDrag {
    /** The offset: the bar moves and keeps its length. */
    Move,

    /** The duration: the end of the bar moves. */
    Resize,
}

/**
 * The drag that a press at [x] starts on a bar from [barStart] to [barEnd], all in px.
 * A press within [edge] px of the end resizes; a press on a bar too short to have a middle
 * also resizes, so that a voice of length zero can still grow.
 */
internal fun laneDragAt(
    x: Float,
    barStart: Float,
    barEnd: Float,
    edge: Float,
): LaneDrag =
    if (x >= barEnd - edge ||
        barEnd - barStart < 2 * edge
    ) {
        LaneDrag.Resize
    } else {
        LaneDrag.Move
    }

/**
 * The offset and duration in ms after a [drag] of [deltaMs] on a voice that started at [offset]
 * and lasted [duration]. Both stay in the 16-bit range of the format.
 */
internal fun draggedTimes(
    drag: LaneDrag,
    offset: Int,
    duration: Int,
    deltaMs: Float,
): Pair<Int, Int> {
    val delta = deltaMs.roundToInt()
    val range = 0..SynthLimits.U16_MAX
    return when (drag) {
        LaneDrag.Move -> (offset + delta).coerceIn(range) to duration
        LaneDrag.Resize -> offset to (duration + delta).coerceIn(range)
    }
}

/**
 * The ruler step in ms for [spanMs] over [widthPx] px: the smallest step of 1, 2, or 5 times a
 * power of ten whose ticks are at least [minGapPx] px apart.
 */
internal fun rulerStep(
    spanMs: Int,
    widthPx: Float,
    minGapPx: Float,
): Int {
    if (widthPx <= 0f) return spanMs.coerceAtLeast(1)
    val minStep = spanMs * minGapPx / widthPx
    var power = DECIMAL.pow(floor(log10(minStep.coerceAtLeast(1f).toDouble()))).toInt()
    while (true) {
        for (mantissa in RulerMantissas) {
            val step = mantissa * power
            if (step >= minStep) return step
        }
        power *= DECIMAL.toInt()
    }
}
