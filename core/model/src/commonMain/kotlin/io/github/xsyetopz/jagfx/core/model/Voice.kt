package io.github.xsyetopz.jagfx.core.model

/**
 * One oscillator of a voice.
 *
 * @property amplitude Partial gain.
 * @property pitchOffset Pitch offset in 1/10 semitone.
 * @property delay Start delay in the voice.
 */
public data class Partial(
    val amplitude: Percent,
    val pitchOffset: Int,
    val delay: Milliseconds,
) {
    init {
        requireIn(1..SynthLimits.USMART16_MAX, amplitude.value, "Partial.amplitude")
        requireIn(SynthLimits.smart16, pitchOffset, "Partial.pitchOffset")
        requireIn(SynthLimits.usmart16, delay.value, "Partial.delay")
    }
}

/** Recursive echo: [feedback] of the signal returns after [delay]. */
public data class Echo(
    val delay: Milliseconds,
    val feedback: Percent,
) {
    init {
        requireIn(SynthLimits.usmart16, delay.value, "Echo.delay")
        requireIn(SynthLimits.usmart16, feedback.value, "Echo.feedback")
    }

    public companion object {
        public val None: Echo = Echo(Milliseconds(0), Percent(0))
    }
}

/** One synthesizer channel of a [Patch]. */
public data class Voice(
    val frequencyEnvelope: Envelope,
    val amplitudeEnvelope: Envelope,
    val pitchLfo: Lfo?,
    val amplitudeLfo: Lfo?,
    val gate: Gate?,
    val partials: List<Partial>,
    val echo: Echo,
    val duration: Milliseconds,
    val offset: Milliseconds,
    val filter: Filter?,
) {
    init {
        requireIn(0..SynthLimits.MAX_PARTIALS, partials.size, "Voice.partials.size")
        requireIn(SynthLimits.u16, duration.value, "Voice.duration")
        requireIn(SynthLimits.u16, offset.value, "Voice.offset")
    }
}
