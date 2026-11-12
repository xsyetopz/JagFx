package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.canvas.ENVELOPE_POSITION_SPAN
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TEN = 10.0

/** One-line description of cell of [slot], shown in its tooltip. */
internal fun descriptionOf(slot: SignalChainSlot): UiString =
    when (slot) {
        SignalChainSlot.Pitch -> UiString.CellDescPitch
        SignalChainSlot.VibratoRate -> UiString.CellDescVibratoRate
        SignalChainSlot.VibratoDepth -> UiString.CellDescVibratoDepth
        SignalChainSlot.Volume -> UiString.CellDescVolume
        SignalChainSlot.TremoloRate -> UiString.CellDescTremoloRate
        SignalChainSlot.TremoloDepth -> UiString.CellDescTremoloDepth
        SignalChainSlot.GapOff -> UiString.CellDescGapOff
        SignalChainSlot.GapOn -> UiString.CellDescGapOn
        SignalChainSlot.Filter -> UiString.CellDescFilter
        SignalChainSlot.PoleZero -> UiString.CellDescPoleZero
        SignalChainSlot.Output -> UiString.CellDescOutput
        SignalChainSlot.Bode -> UiString.CellDescBode
    }

/** Label after number in this unit; empty for filter curve, which has none. */
internal fun ValueUnit?.suffix(): String =
    when (this) {
        ValueUnit.Hz -> " Hz"
        ValueUnit.Percent -> "%"
        ValueUnit.Ms -> " ms"
        null -> ""
    }

/** [value] rounded to [decimals] places, without trailing zeros: `220`, `87.5`, `-0.25`. */
internal fun formatDecimal(
    value: Double,
    decimals: Int,
): String {
    val factor = TEN.pow(decimals)
    val scaled = (value * factor).roundToLong()
    val whole = abs(scaled) / factor.toLong()
    val fraction = (abs(scaled) % factor.toLong()).toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (scaled < 0) "-" else ""
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}

/** One line for point: `500 ms · 220 Hz · 32768/65536 · 40000`. */
internal fun readoutText(readout: PointReadout): String =
    listOf(
        "${formatDecimal(readout.timeMs.toDouble(), 1)} ms",
        formatDecimal(readout.value, 2) + readout.unit.suffix(),
        "${readout.position}/${ENVELOPE_POSITION_SPAN.toInt()}",
        readout.level.toString(),
    ).joinToString(" · ")

/** Short label of point on plot: `12.5 ms · 300 Hz`. */
internal fun pointLabelText(readout: PointReadout): String =
    "${formatDecimal(readout.timeMs.toDouble(), 1)} ms · " +
        formatDecimal(readout.value, 2) + readout.unit.suffix()

/** Label [text] of breakpoint [index]. */
internal data class PointLabel(
    val index: Int,
    val text: String,
)

/** Label of every breakpoint of [envelope] in [slot] for voice [durationMs] long. */
internal fun pointLabels(
    slot: SignalChainSlot,
    envelope: Envelope,
    durationMs: Int,
): List<PointLabel> =
    envelope.segments.indices.mapNotNull { index ->
        pointReadout(
            slot,
            envelope,
            index,
            durationMs,
        )?.let { PointLabel(index, pointLabelText(it)) }
    }

/** Columns of point table. */
internal enum class PointField { Time, Value, Position, Level }

/** What typed entry in point table does. */
internal sealed interface FieldEdit {
    /** Entry is valid; [segment] replaces point. */
    data class Applied(
        val segment: Segment,
    ) : FieldEdit

    /** Entry is number outside [min] to [max], which are shown in field's unit. */
    data class OutOfRange(
        val min: String,
        val max: String,
    ) : FieldEdit

    /** Entry is not number. */
    data object NotANumber : FieldEdit
}

private val RawRange = 0..SynthLimits.U16_MAX

/** Text of [field] for breakpoint [index], as table shows it. */
internal fun fieldText(
    field: PointField,
    slot: SignalChainSlot,
    envelope: Envelope,
    index: Int,
    durationMs: Int,
): String {
    val segment = envelope.segments[index]
    return when (field) {
        PointField.Position -> {
            segment.duration.toString()
        }

        PointField.Level -> {
            segment.targetLevel.toString()
        }

        PointField.Time -> {
            val position = if (index == 0) 0 else segment.duration
            formatDecimal(positionToMs(position.toFloat(), durationMs).toDouble(), 1)
        }

        PointField.Value -> {
            val unit = slot.range?.unit
            val value =
                unit?.fromRaw(envelope.rawValueAt(segment.targetLevel))
                    ?: segment.targetLevel.toDouble()
            formatDecimal(value, 2)
        }
    }
}

/**
 * Effect of [text] typed into [field] of breakpoint [index]. Time or value entry converts
 * to raw position or level, rounded to nearest; point only changes when that raw
 * number is in 0 to 65535.
 */
internal fun editField(
    field: PointField,
    text: String,
    slot: SignalChainSlot,
    envelope: Envelope,
    index: Int,
    durationMs: Int,
): FieldEdit {
    val typed = text.trim().replace(',', '.').toDoubleOrNull() ?: return FieldEdit.NotANumber
    val segment = envelope.segments[index]
    val unit = slot.range?.unit
    val span = envelope.endValue.toDouble() - envelope.startValue
    return when (field) {
        PointField.Position -> {
            rawEdit(typed) { segment.copy(duration = it) }
        }

        PointField.Level -> {
            rawEdit(typed) { segment.copy(targetLevel = it) }
        }

        PointField.Time -> {
            if (durationMs <= 0) {
                FieldEdit.NotANumber
            } else {
                val raw = typed / durationMs * ENVELOPE_POSITION_SPAN
                val max = positionToMs(SynthLimits.U16_MAX.toFloat(), durationMs)
                rawEdit(raw, "0", formatDecimal(max.toDouble(), 1)) {
                    segment.copy(duration = it)
                }
            }
        }

        PointField.Value -> {
            if (unit == null) {
                rawEdit(typed) { segment.copy(targetLevel = it) }
            } else if (span == 0.0) {
                FieldEdit.NotANumber
            } else {
                val raw = (unit.toRaw(typed) - envelope.startValue) / span * ENVELOPE_POSITION_SPAN
                val ends =
                    listOf(0, SynthLimits.U16_MAX).map { unit.fromRaw(envelope.rawValueAt(it)) }
                rawEdit(raw, formatDecimal(ends.min(), 2), formatDecimal(ends.max(), 2)) {
                    segment.copy(targetLevel = it)
                }
            }
        }
    }
}

private fun rawEdit(
    raw: Double,
    min: String = RawRange.first.toString(),
    max: String = RawRange.last.toString(),
    apply: (Int) -> Segment,
): FieldEdit {
    val rounded = raw.roundToInt()
    return if (rounded in
        RawRange
    ) {
        FieldEdit.Applied(apply(rounded))
    } else {
        FieldEdit.OutOfRange(min, max)
    }
}
