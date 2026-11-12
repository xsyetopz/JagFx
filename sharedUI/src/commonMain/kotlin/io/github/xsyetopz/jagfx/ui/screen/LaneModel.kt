package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Ruler steps in ms are 1, 2, and [HALF_DECADE] times power of ten. */
private const val HALF_DECADE = 5
private val RulerMantissas = listOf(1, 2, HALF_DECADE)
private const val DECIMAL = 10.0

/** Time span in ms tone bars share: latest voice end or loop end, at least 1. */
internal fun timelineSpan(patch: Patch): Int {
    val voiceEnd = patch.voices.maxOf { (it?.offset?.value ?: 0) + (it?.duration?.value ?: 0) }
    return maxOf(voiceEnd, patch.loop.end.value, 1)
}

/** What drag on voice bar changes. */
internal enum class LaneDrag {
    /** Offset: bar moves and keeps its length. */
    Move,

    /** Duration: end of bar moves. */
    Resize,
}

/**
 * Drag that press at [x] starts on bar from [barStart] to [barEnd], all in px.
 * Press within [edge] px of end resizes; press on bar too short to have middle
 * also resizes, so that voice of length zero can still grow.
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
 * Offset and duration in ms after [drag] of [deltaMs] on voice that started at [offset]
 * and lasted [duration]. Both stay in 16-bit range of format.
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
 * Ruler step in ms for [spanMs] over [widthPx] px: smallest step of 1, 2, or 5 times
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
