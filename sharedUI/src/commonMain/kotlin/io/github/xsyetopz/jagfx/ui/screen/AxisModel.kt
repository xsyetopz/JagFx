package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.ui.canvas.ENVELOPE_POSITION_SPAN
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import kotlin.math.ceil
import kotlin.math.floor

private const val PERCENT_PER_RAW = 655.35
private const val GATE_UNITS_PER_STEP = 256.0
private const val SAMPLES_PER_MS = 22.05
private const val DEFAULT_VALUE_TICKS = 5

/** [raw] in this unit: percent is `raw / 655.35`, a gate ms is `raw / 256 / 22.05`, Hz is `raw`. */
internal fun ValueUnit.fromRaw(raw: Double): Double =
    when (this) {
        ValueUnit.Hz -> raw
        ValueUnit.Percent -> raw / PERCENT_PER_RAW
        ValueUnit.Ms -> raw / GATE_UNITS_PER_STEP / SAMPLES_PER_MS
    }

/**
 * The raw value of an envelope point at [level] (of 65536): `startValue` at 0 and `endValue` at
 * 65536, linear between, as the voice setup scales the generator output.
 */
internal fun Envelope.rawValueAt(level: Int): Double =
    startValue + (endValue - startValue).toDouble() * level / ENVELOPE_POSITION_SPAN

/**
 * Times in ms, in ascending order, of the ruler ticks of a voice [durationMs] long when the plot
 * [plotWidth] px wide shows [window] (fractions of the voice) with at least [minGapPx] px between
 * ticks.
 */
internal fun timeTicks(
    durationMs: Int,
    window: ClosedFloatingPointRange<Float>,
    plotWidth: Float,
    minGapPx: Float,
): List<Int> {
    if (durationMs <= 0) return emptyList()
    val startMs = window.start * durationMs
    val endMs = window.endInclusive * durationMs
    val step = rulerStep(ceil(endMs - startMs).toInt(), plotWidth, minGapPx)
    return (ceil(startMs / step).toInt() * step..floor(endMs).toInt() step step).toList()
}

/**
 * A value-axis tick: [fraction] of the plot height from the bottom (0 is `startValue`, 1 is
 * `endValue`) and the [value] there in the unit of the slot.
 */
internal data class ValueTick(
    val fraction: Float,
    val value: Double,
)

/**
 * [count] evenly spaced ticks from the start to the end value of [envelope], in the unit of
 * [slot] (at least 2); empty when the slot has no start and end values.
 */
internal fun valueTicks(
    slot: SignalChainSlot,
    envelope: Envelope,
    count: Int = DEFAULT_VALUE_TICKS,
): List<ValueTick> {
    val unit = slot.range?.unit ?: return emptyList()
    return List(count) {
        val fraction = it.toFloat() / (count - 1)
        val level = (fraction * ENVELOPE_POSITION_SPAN).toInt()
        ValueTick(fraction, unit.fromRaw(envelope.rawValueAt(level)))
    }
}

/**
 * What the cell shows for one point.
 *
 * @property timeMs Time of the point in ms of the voice.
 * @property value The value in [unit]; the raw level when the slot has no unit.
 * @property unit The unit of [value], or `null` for the filter curve.
 * @property position Raw position in 1/65536 of the voice; 0 for breakpoint 0, which the game does
 *     not read.
 * @property level Raw target level.
 */
internal data class PointReadout(
    val timeMs: Float,
    val value: Double,
    val unit: ValueUnit?,
    val position: Int,
    val level: Int,
)

/** The readout of breakpoint [index] of [envelope] in [slot] for a voice [durationMs] long. */
internal fun pointReadout(
    slot: SignalChainSlot,
    envelope: Envelope,
    index: Int,
    durationMs: Int,
): PointReadout? {
    val segment = envelope.segments.getOrNull(index) ?: return null
    val position = if (index == 0) 0 else segment.duration
    val unit = slot.range?.unit
    val value =
        unit?.fromRaw(envelope.rawValueAt(segment.targetLevel)) ?: segment.targetLevel.toDouble()
    return PointReadout(
        positionToMs(position.toFloat(), durationMs),
        value,
        unit,
        position,
        segment.targetLevel,
    )
}
