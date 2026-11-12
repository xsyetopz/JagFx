package io.github.xsyetopz.jagfx.core.model

import kotlin.jvm.JvmInline

/**
 * Oscillator shape. [id] is the byte value in the binary format.
 *
 * The game client defines shapes for [Square], [Sine], [Saw], and [Noise], and renders [Off] and
 * every other id as silence. Game files store some of those other ids, so [id] keeps any byte value.
 * Editors keep such ids but do not offer them as choices.
 */
@JvmInline
public value class Waveform(
    public val id: Int,
) {
    init {
        requireIn(SynthLimits.u8, id, "Waveform.id")
    }

    /** `true` if the game client defines no shape for [id] and renders it as silence. */
    public val isUndefined: Boolean get() = id > Noise.id

    override fun toString(): String = names.getOrElse(id) { "Undefined($id)" }

    public companion object {
        public val Off: Waveform = Waveform(0)
        public val Square: Waveform = Waveform(1)
        public val Sine: Waveform = Waveform(2)
        public val Saw: Waveform = Waveform(3)
        public val Noise: Waveform = Waveform(4)

        /** The waveforms an editor offers: the ones the game client defines, and [Off]. */
        public val selectable: List<Waveform> = listOf(Off, Square, Sine, Saw, Noise)

        private val names = listOf("Off", "Square", "Sine", "Saw", "Noise")
    }
}

/**
 * One envelope breakpoint.
 *
 * @property duration End time of the segment, in 1/65536 of the voice length.
 * @property targetLevel Envelope value at [duration].
 */
public data class Segment(
    val duration: Int,
    val targetLevel: Int,
) {
    init {
        requireIn(SynthLimits.u16, duration, "Segment.duration")
        requireIn(SynthLimits.u16, targetLevel, "Segment.targetLevel")
    }
}

/** A piecewise-linear control curve. */
public data class Envelope(
    val waveform: Waveform,
    val startValue: Int,
    val endValue: Int,
    val segments: List<Segment>,
) {
    init {
        requireIn(0..SynthLimits.MAX_SEGMENTS, segments.size, "Envelope.segments.size")
    }
}

/** Low-frequency oscillator: [rate] selects the LFO waveform and speed, [depth] its amount. */
public data class Lfo(
    val rate: Envelope,
    val depth: Envelope,
)

/** Gate thresholds for the muted state ([gapOff]) and the open state ([gapOn]). */
public data class Gate(
    val gapOff: Envelope,
    val gapOn: Envelope,
)
