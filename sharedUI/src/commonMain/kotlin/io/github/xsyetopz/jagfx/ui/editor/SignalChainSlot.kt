package io.github.xsyetopz.jagfx.ui.editor

/** Display unit of start and end values of envelope. */
internal enum class ValueUnit {
    Hz,
    Percent,
    Ms,
}

/**
 * Start and end value domain of envelope target.
 *
 * [displayMin] and [displayMax] bound musical range drawn on canvas, in [unit]:
 * percent is `raw / 655.35`, gate millisecond is `raw / 256 / 22.05`, and frequency in Hz is
 * raw value. Stored value may lie outside it, because format holds any `i32`.
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
 * Panel of signal chain.
 *
 * First nine entries are envelopes, in [envelopeChain] order. [PoleZero], [Output] and [Bode]
 * are views without envelope.
 *
 * @property range Start and end value domain, or `null` when slot has no start and end
 *     values.
 * @property usesLogFrequencyScale `true` when canvas draws values on log-frequency axis.
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

    /** Segments of filter modulation envelope. It has no waveform and no start or end. */
    Filter(null),
    PoleZero(null),
    Output(null),
    Bode(null),
    ;

    /** `true` for slots that edit envelope. */
    val isEnvelope: Boolean get() = this in envelopeChain

    /** `true` when slot has waveform selector and start and end values. */
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
