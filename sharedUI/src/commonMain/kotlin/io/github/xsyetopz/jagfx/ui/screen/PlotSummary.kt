package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.ui.canvas.FilterFrequencyScale
import io.github.xsyetopz.jagfx.ui.canvas.FilterResponse
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.poles
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.abs
import kotlin.math.roundToInt

private const val RESPONSE_POINTS = 256
private const val MS_PER_SECOND = 1000.0
private const val PERCENT = 100.0
private const val ONE = 1
private const val MAX_RAW = 65535.0

/**
 * Text summary of envelope plot of [slot], for screen readers:
 * `Pitch envelope, 4 points, 200 Hz to 880 Hz over 1200 ms`.
 * Range runs from lowest to highest value of points.
 */
internal fun envelopeSummary(
    strings: Strings,
    slot: SignalChainSlot,
    envelope: Envelope?,
    durationMs: Int,
): String {
    val title = strings[titleOf(slot)]
    if (envelope == null || envelope.segments.isEmpty()) {
        return strings.format(UiString.PlotSummaryEnvelopeEmpty, title)
    }
    val unit = slot.range?.unit
    val values =
        envelope.segments.indices.mapNotNull { pointReadout(slot, envelope, it, durationMs)?.value }
    return strings.format(
        UiString.PlotSummaryEnvelope,
        title,
        envelope.segments.size,
        formatDecimal(values.min(), 2) + unit.suffix(),
        formatDecimal(values.max(), 2) + unit.suffix(),
        durationMs,
    )
}

/**
 * State of envelope plot: selected point and its readout, or `null` when no point of
 * [envelope] is selected.
 */
internal fun envelopeState(
    strings: Strings,
    slot: SignalChainSlot,
    envelope: Envelope?,
    selectedIndex: Int,
    durationMs: Int,
): String? {
    val readout =
        envelope?.let { pointReadout(slot, it, selectedIndex, durationMs) } ?: return null
    return strings.format(
        UiString.PlotSummaryPointSelected,
        selectedIndex + ONE,
        envelope.segments.size,
        readoutText(readout),
    )
}

/** Summary of output wave: its length and peak level, or note that nothing is rendered. */
internal fun outputSummary(
    strings: Strings,
    samples: FloatArray?,
): String {
    if (samples == null || samples.isEmpty()) {
        return strings[UiString.PlotSummaryOutputEmpty]
    }
    val peak = samples.maxOf { abs(it) }
    val lengthMs = samples.size / FilterFrequencyScale.SAMPLE_RATE * MS_PER_SECOND
    return strings.format(
        UiString.PlotSummaryOutput,
        lengthMs.roundToInt(),
        (peak * PERCENT).roundToInt(),
    )
}

/** Summary of pole-zero plot: `Pole-zero plot, 2 poles, 1 zero`. */
internal fun poleZeroSummary(
    strings: Strings,
    filter: Filter?,
): String {
    if (filter == null) return strings[UiString.PlotSummaryPoleZeroEmpty]
    val poles = filter.feedback.size
    val zeros = filter.feedforward.size
    return strings.format(
        UiString.PlotSummaryPoleZero,
        strings.format(
            if (poles == ONE) UiString.PlotSummaryPoleOne else UiString.PlotSummaryPoleMany,
            poles,
        ),
        strings.format(
            if (zeros == ONE) UiString.PlotSummaryZeroOne else UiString.PlotSummaryZeroMany,
            zeros,
        ),
    )
}

/**
 * State of pole-zero plot: selected marker as `Pole 2 of 3, end: 440 Hz, 50 dB`, or
 * `null` when nothing is selected or [selected] is not marker of [filter].
 */
internal fun poleState(
    strings: Strings,
    filter: Filter?,
    selected: PoleRef?,
): String? {
    val poles = selected?.let { filter?.poles(it.channel) }
    if (selected == null || poles == null || selected.pole !in poles.indices) return null
    val pole = poles[selected.pole]
    val coefficient = if (selected.phaseIndex == 0) pole.start else pole.end
    return strings.format(
        if (selected.channel == 0) UiString.PlotStateZero else UiString.PlotStatePole,
        selected.pole + ONE,
        poles.size,
        strings[if (selected.phaseIndex == 0) UiString.PlotStateStart else UiString.PlotStateEnd],
        FilterFrequencyScale.rawPhaseToFrequency(coefficient.phase).roundToInt(),
        formatDecimal(coefficient.magnitude * PERCENT / MAX_RAW, 1),
    )
}

/** Summary of frequency response: strongest peak, in dB and Hz, at start of voice. */
internal fun responseSummary(
    strings: Strings,
    filter: Filter?,
): String {
    if (filter == null || (filter.feedforward.isEmpty() && filter.feedback.isEmpty())) {
        return strings[UiString.PlotSummaryResponseEmpty]
    }
    val response = FilterResponse.magnitudeDb(filter, 0.0, RESPONSE_POINTS)
    val peak = response.indices.maxBy { response[it] }
    val hz =
        FilterFrequencyScale.normalizedXToFrequency(peak.toDouble() / (RESPONSE_POINTS - ONE))
    return strings.format(
        UiString.PlotSummaryResponse,
        formatDecimal(response[peak], 1),
        hz.roundToInt(),
    )
}
