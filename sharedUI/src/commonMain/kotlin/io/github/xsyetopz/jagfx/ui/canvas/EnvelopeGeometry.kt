package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.math.round

/** Inset of the envelope plot from each canvas edge, in pixels. */
internal const val ENVELOPE_PADDING: Float = 4f

/** Distance in pixels within which a press hits a breakpoint. */
internal const val ENVELOPE_POINT_HIT_RADIUS: Float = 8f

/** Distance in pixels within which a press hits a line between two breakpoints. */
internal const val ENVELOPE_LINE_HIT_RADIUS: Float = 6f

private const val DEGENERATE_LENGTH_SQUARED = 0.001f
private const val LEVEL_SNAP_DIVISIONS = 4

/** Span of the time axis: a segment position of 65536 would be the end of the voice. */
internal const val ENVELOPE_POSITION_SPAN: Float = 65536f

/** Vertical scale of an envelope plot. */
internal enum class EnvelopeScaleMode {
    /** Levels 0..65535 fill the plot height. */
    FullScale,

    /** Same vertical mapping as [FullScale]. */
    Normalized,

    /** The range between the lowest and highest level of the envelope fills the plot height. */
    AutoScale,
}

/**
 * Maps the breakpoints of an envelope to canvas pixels and back.
 * Breakpoint `i` is the target level of segment `i` at its stored position, a fraction of the
 * voice in 1/65536; breakpoint 0 is at time 0, because the game client does not read its position.
 */
internal class EnvelopeGeometry(
    val segments: List<Segment>,
    val width: Float,
    val height: Float,
    viewport: Viewport = Viewport(),
    val scaleMode: EnvelopeScaleMode = EnvelopeScaleMode.FullScale,
) {
    /** Zoom level of the plot. */
    val zoom: Int = viewport.zoom

    /** Horizontal scroll offset of the plot, in pixels. */
    val scrollOffset: Float = viewport.scrollOffset

    /** Width of the whole plot at [zoom], in pixels. */
    val plotWidth: Float = ((width - 2 * ENVELOPE_PADDING) * zoom).coerceAtLeast(0f)

    /** Height of the plot, in pixels. */
    val plotHeight: Float = (height - 2 * ENVELOPE_PADDING).coerceAtLeast(0f)

    /** Level at the bottom of the plot. */
    val minValue: Int

    /** Level span between the bottom and the top of the plot; at least 1. */
    val range: Int

    /** Number of breakpoints: one per segment. */
    val pointCount: Int = segments.size

    private val xs = FloatArray(pointCount)
    private val ys = FloatArray(pointCount)

    init {
        if (scaleMode == EnvelopeScaleMode.AutoScale && segments.isNotEmpty()) {
            val low = segments.minOf { it.targetLevel }
            minValue = low
            range = (segments.maxOf { it.targetLevel } - low).coerceAtLeast(1)
        } else {
            minValue = 0
            range = SynthLimits.U16_MAX
        }
        for (i in segments.indices) {
            val position = if (i == 0) 0 else segments[i].duration
            xs[i] = ENVELOPE_PADDING + position / ENVELOPE_POSITION_SPAN * plotWidth - scrollOffset
            ys[i] = clampY(valueToY(segments[i].targetLevel))
        }
    }

    /** X of breakpoint [index]. */
    fun pointX(index: Int): Float = xs[index]

    /** Y of breakpoint [index], clamped to the plot. */
    fun pointY(index: Int): Float = ys[index]

    /** Unclamped y of the level [value]. */
    fun valueToY(value: Int): Float {
        val norm = (value - minValue).toFloat() / range
        return ENVELOPE_PADDING + plotHeight - norm * plotHeight
    }

    /** The index of the first breakpoint within [ENVELOPE_POINT_HIT_RADIUS] of ([x], [y]), or -1. */
    fun hitTest(
        x: Float,
        y: Float,
    ): Int {
        val radiusSquared = ENVELOPE_POINT_HIT_RADIUS * ENVELOPE_POINT_HIT_RADIUS
        for (i in 0 until pointCount) {
            val dx = x - xs[i]
            val dy = y - ys[i]
            if (dx * dx + dy * dy <= radiusSquared) return i
        }
        return -1
    }

    /**
     * The index of the breakpoint that ends the line passing within [ENVELOPE_LINE_HIT_RADIUS] of
     * ([x], [y]), or -1.
     */
    fun lineHitTest(
        x: Float,
        y: Float,
    ): Int {
        val radiusSquared = ENVELOPE_LINE_HIT_RADIUS * ENVELOPE_LINE_HIT_RADIUS
        for (i in 1 until pointCount) {
            val ax = xs[i - 1]
            val ay = ys[i - 1]
            val dx = xs[i] - ax
            val dy = ys[i] - ay
            val lengthSquared = dx * dx + dy * dy
            val t =
                if (lengthSquared < DEGENERATE_LENGTH_SQUARED) {
                    0f
                } else {
                    (((x - ax) * dx + (y - ay) * dy) / lengthSquared).coerceIn(0f, 1f)
                }
            val ex = x - (ax + t * dx)
            val ey = y - (ay + t * dy)
            if (ex * ex + ey * ey <= radiusSquared) return i
        }
        return -1
    }

    /** The segment position at canvas [x], in 1/65536 of the voice; 0 when the plot has no width. */
    fun xToPosition(x: Float): Float =
        if (plotWidth <= 0f) {
            0f
        } else {
            (x + scrollOffset - ENVELOPE_PADDING) / plotWidth * ENVELOPE_POSITION_SPAN
        }

    /** The level at canvas [y] for a scale of [min] and [range], clamped to 0..65535. */
    fun yToPeakLevel(
        y: Float,
        min: Int = minValue,
        range: Int = this.range,
    ): Int {
        val fraction = if (plotHeight <= 0f) 0f else 1f - (y - ENVELOPE_PADDING) / plotHeight
        return (fraction * range + min).toInt().coerceIn(0, SynthLimits.U16_MAX)
    }

    /**
     * [segments] with breakpoint [index] moved to canvas [x], kept between the positions of its
     * neighbours (the last one up to 65535). Breakpoint 0 stays at time 0.
     */
    fun movePoint(
        segments: List<Segment>,
        index: Int,
        x: Float,
    ): List<Segment> {
        if (plotWidth <= 0f || index !in 1..segments.lastIndex) return segments
        val position = xToPosition(x).toInt().coerceIn(positionBounds(segments, index))
        return segments.replace(index, segments[index].copy(duration = position))
    }

    /**
     * [segments] with a breakpoint inserted before breakpoint [endIndex] at canvas ([x], [y]),
     * kept between the positions of the line's two breakpoints. Returns `null` when the envelope
     * already has [SynthLimits.MAX_SEGMENTS] segments or [endIndex] does not end a line.
     */
    fun insertPointOnLine(
        segments: List<Segment>,
        endIndex: Int,
        x: Float,
        y: Float,
    ): List<Segment>? {
        if (segments.size >= SynthLimits.MAX_SEGMENTS || endIndex !in 1..segments.lastIndex) {
            return null
        }
        val start = if (endIndex == 1) 0 else segments[endIndex - 1].duration
        val end = segments[endIndex].duration.coerceAtLeast(start)
        val position = xToPosition(x).toInt().coerceIn(start, end)
        return segments.toMutableList().apply {
            add(endIndex, Segment(position, yToPeakLevel(y)))
        }
    }

    private fun clampY(y: Float): Float =
        y.coerceIn(ENVELOPE_PADDING, ENVELOPE_PADDING + plotHeight)
}

/** [level] rounded to the nearest of `4 * zoom` steps across [range] above [min]. */
internal fun snapLevel(
    level: Int,
    min: Int,
    range: Int,
    zoom: Int,
): Int {
    val step = range.toDouble() / (LEVEL_SNAP_DIVISIONS * zoom)
    if (step <= 0.0) return level
    return (round((level - min) / step) * step + min).toInt().coerceIn(0, SynthLimits.U16_MAX)
}

/**
 * [segments] with the position of breakpoint [index] rounded to the nearest multiple of [step],
 * kept between the positions of its neighbours. Breakpoint 0 is left as it is.
 */
internal fun snapPosition(
    segments: List<Segment>,
    index: Int,
    step: Float,
): List<Segment> {
    if (step <= 0f || index !in 1..segments.lastIndex) return segments
    val current = segments[index]
    val snapped =
        (round(current.duration / step) * step).toInt().coerceIn(positionBounds(segments, index))
    return segments.replace(index, current.copy(duration = snapped))
}

/** Positions that breakpoint [index] can take without passing a neighbour. */
private fun positionBounds(
    segments: List<Segment>,
    index: Int,
): IntRange {
    val low = if (index == 1) 0 else segments[index - 1].duration
    val high = segments.getOrNull(index + 1)?.duration ?: SynthLimits.U16_MAX
    return low..high.coerceAtLeast(low)
}

/** [segments] with the target level of segment [index] set to [level]. */
internal fun withTargetLevel(
    segments: List<Segment>,
    index: Int,
    level: Int,
): List<Segment> =
    segments.replace(
        index,
        segments[index].copy(targetLevel = level.coerceIn(0, SynthLimits.U16_MAX)),
    )

/** [segments] without segment [index], or `null` when only one segment would remain to remove. */
internal fun removeSegment(
    segments: List<Segment>,
    index: Int,
): List<Segment>? =
    if (segments.size <= 1 || index !in segments.indices) {
        null
    } else {
        segments.toMutableList().apply { removeAt(index) }
    }

/**
 * [segments] with a breakpoint appended at the end of the voice at the last level, so the sound
 * does not change; `null` at [SynthLimits.MAX_SEGMENTS] segments.
 */
internal fun appendSegment(segments: List<Segment>): List<Segment>? =
    if (segments.size >= SynthLimits.MAX_SEGMENTS) {
        null
    } else {
        segments + Segment(SynthLimits.U16_MAX, segments.lastOrNull()?.targetLevel ?: 0)
    }

private fun List<Segment>.replace(
    index: Int,
    segment: Segment,
): List<Segment> = toMutableList().apply { set(index, segment) }
