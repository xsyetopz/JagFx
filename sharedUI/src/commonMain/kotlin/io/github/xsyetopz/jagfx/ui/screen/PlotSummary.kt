package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.ui.canvas.FilterFrequencyScale
import io.github.xsyetopz.jagfx.ui.canvas.FilterResponse
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.math.abs
import kotlin.math.roundToInt

private const val RESPONSE_POINTS = 256
private const val MS_PER_SECOND = 1000.0
private const val PERCENT = 100.0
private const val ONE = 1

/**
 * The text summary of the envelope plot of [slot], for screen readers:
 * `Pitch envelope, 4 points, 200 Hz to 880 Hz over 1200 ms`.
 * The range runs from the lowest to the highest value of the points.
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
 * The state of the envelope plot: the selected point and its readout, or `null` when no point of
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

/** Summary of the output wave: its length and peak level, or a note that nothing is rendered. */
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

/** Summary of the pole-zero plot: `Pole-zero plot, 2 poles, 1 zero`. */
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

/** Summary of the frequency response: the strongest peak, in dB and Hz, at the start of the voice. */
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
