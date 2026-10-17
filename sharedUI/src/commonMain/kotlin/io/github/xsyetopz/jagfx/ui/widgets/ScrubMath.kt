package io.github.xsyetopz.jagfx.ui.widgets

/** Pure value mapping of [scrub]. */
internal object ScrubMath {
    /** Full drag range in pixels is this many pixels per [valueRangeSpan][ClosedFloatingPointRange]. */
    private const val DRAG_PIXELS_PER_RANGE = 100f

    /** Position of [value] in [range] as 0..1; an empty range maps to 0. */
    fun fraction(
        value: Float,
        range: ClosedFloatingPointRange<Float>,
    ): Float {
        val span = range.endInclusive - range.start
        return if (span <= 0f) 0f else ((value - range.start) / span).coerceIn(0f, 1f)
    }

    /**
     * Value after dragging [pixels] from [anchor]: one hundred pixels cover the whole range, right
     * and up increase. The result stays in [range].
     */
    fun dragValue(
        anchor: Float,
        pixels: Float,
        range: ClosedFloatingPointRange<Float>,
    ): Float {
        val span = range.endInclusive - range.start
        return (anchor + pixels * (span / DRAG_PIXELS_PER_RANGE)).coerceIn(range)
    }

    /** Value after one wheel notch: [direction] above zero adds [step], below zero subtracts it. */
    fun wheelValue(
        value: Float,
        direction: Float,
        step: Float,
        range: ClosedFloatingPointRange<Float>,
    ): Float =
        when {
            direction > 0f -> value + step
            direction < 0f -> value - step
            else -> value
        }.coerceIn(range)

    /** Value of a double click: [defaultValue] clamped to [range], or the minimum without one. */
    fun resetValue(
        defaultValue: Float?,
        range: ClosedFloatingPointRange<Float>,
    ): Float = defaultValue?.coerceIn(range) ?: range.start
}
