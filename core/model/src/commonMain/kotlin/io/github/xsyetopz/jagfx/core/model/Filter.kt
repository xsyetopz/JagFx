package io.github.xsyetopz.jagfx.core.model

/** Phase and magnitude of one filter pole at one filter phase. */
public data class Coefficient(
    val phase: Int,
    val magnitude: Int,
) {
    init {
        requireIn(SynthLimits.u16, phase, "Coefficient.phase")
        requireIn(SynthLimits.u16, magnitude, "Coefficient.magnitude")
    }
}

/** One filter pole: its coefficient at the start ([start]) and at the end ([end]) of the voice. */
public data class Pole(
    val start: Coefficient,
    val end: Coefficient,
) {
    val isModulated: Boolean get() = start != end
}

/**
 * Pole-zero filter of a voice.
 *
 * @property feedforward Poles of direction 0.
 * @property feedback Poles of direction 1.
 * @property modulationEnvelope Interpolation curve from [Pole.start] to [Pole.end].
 */
public data class Filter(
    val unityGain0: Int,
    val unityGain1: Int,
    val feedforward: List<Pole>,
    val feedback: List<Pole>,
    val modulationEnvelope: List<Segment>?,
) {
    init {
        requireIn(SynthLimits.u16, unityGain0, "Filter.unityGain0")
        requireIn(SynthLimits.u16, unityGain1, "Filter.unityGain1")
        requireIn(0..SynthLimits.MAX_POLES, feedforward.size, "Filter.feedforward.size")
        requireIn(0..SynthLimits.MAX_POLES, feedback.size, "Filter.feedback.size")
        if (modulationEnvelope != null) {
            requireIn(
                0..SynthLimits.MAX_SEGMENTS,
                modulationEnvelope.size,
                "Filter.modulationEnvelope.size",
            )
        }
    }

    /** `Mask(F)`: bit `4d + p` is set when pole `p` of direction `d` is modulated. */
    val modulationMask: Int
        get() = maskOf(feedforward, shift = 0) or maskOf(feedback, shift = POLE_BITS)

    /** `true` when the filter changes over the voice, so the format stores [modulationEnvelope]. */
    val needsModulationEnvelope: Boolean
        get() = modulationMask != 0 || unityGain0 != unityGain1

    private companion object {
        const val POLE_BITS = 4

        fun maskOf(
            poles: List<Pole>,
            shift: Int,
        ): Int {
            var mask = 0
            poles.forEachIndexed { p, pole ->
                if (pole.isModulated) {
                    mask =
                        mask or (1 shl (shift + p))
                }
            }
            return mask
        }
    }
}
