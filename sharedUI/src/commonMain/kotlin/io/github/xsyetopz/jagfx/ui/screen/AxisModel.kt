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

/** [raw] in this unit: percent is `raw / 655.35`, gate ms is `raw / 256 / 22.05`, Hz is `raw`. */
internal fun ValueUnit.fromRaw(raw: Double): Double =
    when (this) {
        ValueUnit.Hz -> raw
        ValueUnit.Percent -> raw / PERCENT_PER_RAW
        ValueUnit.Ms -> raw / GATE_UNITS_PER_STEP / SAMPLES_PER_MS
    }

/** Raw value of [value] in this unit; inverse of [fromRaw]. */
internal fun ValueUnit.toRaw(value: Double): Double =
    when (this) {
        ValueUnit.Hz -> value
        ValueUnit.Percent -> value * PERCENT_PER_RAW
        ValueUnit.Ms -> value * GATE_UNITS_PER_STEP * SAMPLES_PER_MS
    }

/**
 * Raw value of envelope point at [level] (of 65536): `startValue` at 0 and `endValue` at
 * 65536, linear between, as voice setup scales generator output.
 */
internal fun Envelope.rawValueAt(level: Int): Double =
    startValue + (endValue - startValue).toDouble() * level / ENVELOPE_POSITION_SPAN

/**
 * Times in ms, in ascending order, of ruler ticks of voice [durationMs] long when plot
 * [plotWidth] px wide shows [window] (fractions of voice) with at least [minGapPx] px between
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
 * Value-axis tick: [fraction] of plot height from bottom (0 is `startValue`, 1 is
 * `endValue`) and [value] there in unit of slot.
 */
internal data class ValueTick(
    val fraction: Float,
    val value: Double,
)

/**
 * [count] evenly spaced ticks from start to end value of [envelope], in unit of
 * [slot] (at least 2); empty when slot has no start and end values.
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
 * What cell shows for one point.
 *
 * @property timeMs Time of point in ms of voice.
 * @property value Value in [unit]; raw level when slot has no unit.
 * @property unit Unit of [value], or `null` for filter curve.
 * @property position Raw position in 1/65536 of voice; 0 for breakpoint 0, which game does
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

/** Readout of breakpoint [index] of [envelope] in [slot] for voice [durationMs] long. */
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
