package io.github.xsyetopz.jagfx.ui.editor

/** Display unit of the start and end values of an envelope. */
internal enum class ValueUnit {
    Hz,
    Percent,
    Ms,
}

/**
 * Start and end value domain of an envelope target.
 *
 * [displayMin] and [displayMax] bound the musical range drawn on the canvas, in [unit]: a
 * percent is `raw / 655.35`, a gate millisecond is `raw / 256 / 22.05`, and a frequency in Hz is
 * the raw value. The stored value may lie outside it, because the format holds any `i32`.
 */
internal data class ValueRange(
    val unit: ValueUnit,
    val displayMin: Double,
    val displayMax: Double,
)

private const val NYQUIST_HZ = 11025
private val FrequencyRange =
    ValueRange(
        ValueUnit.Hz,
        displayMin = 0.0,
        displayMax = NYQUIST_HZ.toDouble(),
    )
private val PercentRange =
    ValueRange(
        ValueUnit.Percent,
        displayMin = -100.0,
        displayMax = 100.0,
    )
private val GateRange =
    ValueRange(
        ValueUnit.Ms,
        displayMin = 0.0,
        displayMax = 11.61,
    )

/**
 * A panel of the signal chain.
 *
 * The first nine entries are envelopes, in [envelopeChain] order. [PoleZero], [Output] and [Bode]
 * are views without an envelope.
 *
 * @property range Start and end value domain, or `null` when the slot has no start and end
 *     values.
 * @property usesLogFrequencyScale `true` when the canvas draws values on a log-frequency axis.
 */
internal enum class SignalChainSlot(
    val range: ValueRange?,
    val usesLogFrequencyScale: Boolean = false,
) {
    Pitch(FrequencyRange, usesLogFrequencyScale = true),
    VibratoRate(FrequencyRange, usesLogFrequencyScale = true),
    VibratoDepth(PercentRange),
    Volume(PercentRange),
    TremoloRate(FrequencyRange, usesLogFrequencyScale = true),
    TremoloDepth(PercentRange),
    GapOff(GateRange),
    GapOn(GateRange),

    /** Segments of the filter modulation envelope. It has no waveform and no start or end. */
    Filter(null),
    PoleZero(null),
    Output(null),
    Bode(null),
    ;

    /** `true` for the slots that edit an envelope. */
    val isEnvelope: Boolean get() = this in envelopeChain

    /** `true` when the slot has a waveform selector and start and end values. */
    val hasWaveform: Boolean get() = range != null

    val usesPercentScale: Boolean get() = range?.unit == ValueUnit.Percent

    val usesGateDurationScale: Boolean get() = range?.unit == ValueUnit.Ms

    companion object {
        /** Envelope slots in signal-chain order, for keyboard navigation between canvases. */
        val envelopeChain: List<SignalChainSlot> =
            listOf(
                Pitch,
                VibratoRate,
                VibratoDepth,
                Volume,
                TremoloRate,
                TremoloDepth,
                GapOff,
                GapOn,
                Filter,
            )
    }
}
