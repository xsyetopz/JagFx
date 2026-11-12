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

private const val PERCENT_PER_RAW = 655.35
private const val GATE_UNITS_PER_STEP = 256.0
private const val SAMPLES_PER_MS = 22.05
private const val TEN = 10.0

/** The title the original tool gives the cell of [slot]; it stays English in every language. */
internal fun classicTitleOf(slot: SignalChainSlot): String =
    when (slot) {
        SignalChainSlot.Pitch -> "Freq Base"
        SignalChainSlot.VibratoRate -> "Freq Modulation rate"
        SignalChainSlot.VibratoDepth -> "Freq Modulation range"
        SignalChainSlot.Volume -> "Amp Base"
        SignalChainSlot.TremoloRate -> "Amp Modulation rate"
        SignalChainSlot.TremoloDepth -> "Amp Modulation range"
        SignalChainSlot.GapOff -> "Gap off"
        SignalChainSlot.GapOn -> "Gap on"
        SignalChainSlot.Filter -> "Transition Curve"
        SignalChainSlot.PoleZero -> "Graph of poles and zeros"
        SignalChainSlot.Output -> "Final Output"
        SignalChainSlot.Bode -> "Frequency Response"
    }

/** The one-line description of the cell of [slot], shown in its tooltip. */
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

/** The raw value of [value] in this unit; the inverse of [fromRaw]. */
internal fun ValueUnit.toRaw(value: Double): Double =
    when (this) {
        ValueUnit.Hz -> value
        ValueUnit.Percent -> value * PERCENT_PER_RAW
        ValueUnit.Ms -> value * GATE_UNITS_PER_STEP * SAMPLES_PER_MS
    }

/** The label after a number in this unit; empty for the filter curve, which has none. */
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

/** One line for a point: `500 ms · 220 Hz · 32768/65536 · 40000`. */
internal fun readoutText(readout: PointReadout): String =
    listOf(
        "${formatDecimal(readout.timeMs.toDouble(), 1)} ms",
        formatDecimal(readout.value, 2) + readout.unit.suffix(),
        "${readout.position}/${ENVELOPE_POSITION_SPAN.toInt()}",
        readout.level.toString(),
    ).joinToString(" · ")

/** The columns of the point table. */
internal enum class PointField { Time, Value, Position, Level }

/** What a typed entry in the point table does. */
internal sealed interface FieldEdit {
    /** The entry is valid; [segment] replaces the point. */
    data class Applied(
        val segment: Segment,
    ) : FieldEdit

    /** The entry is a number outside [min] to [max], which are shown in the field's unit. */
    data class OutOfRange(
        val min: String,
        val max: String,
    ) : FieldEdit

    /** The entry is not a number. */
    data object NotANumber : FieldEdit
}

private val RawRange = 0..SynthLimits.U16_MAX

/** The text of [field] for breakpoint [index], as the table shows it. */
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
 * The effect of [text] typed into [field] of breakpoint [index]. A time or value entry converts
 * to the raw position or level, rounded to the nearest; the point only changes when that raw
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
