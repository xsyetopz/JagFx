package io.github.xsyetopz.jagfx.ui.widgets

/** Pure value mapping of [scrub]. */
internal object ScrubMath {
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

    /** Value of double click: [defaultValue] clamped to [range], or minimum without one. */
    fun resetValue(
        defaultValue: Float?,
        range: ClosedFloatingPointRange<Float>,
    ): Float = defaultValue?.coerceIn(range) ?: range.start
}
