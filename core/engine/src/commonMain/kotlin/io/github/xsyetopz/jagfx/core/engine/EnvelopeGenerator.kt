package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice

/**
 * Envelope generator `G` (spec §8.4) over the effective segment list `Q(E)`, for a voice of
 * [length] samples. Each [next] call is one `eval(length)`.
 */
internal class EnvelopeGenerator private constructor(
    private val startValue: Int,
    private val durations: IntArray,
    private val targets: IntArray,
    private val length: Int,
) {
    private var threshold = 1
    private var pos = 0
    private var delta = 0
    private var amp = startValue shl AMP_SHIFT
    private var ticks = 0

    fun next(): Int {
        if (durations.isEmpty()) return startValue
        if (ticks >= threshold) advance()
        amp += delta
        ticks++
        return (amp - delta) shr AMP_SHIFT
    }

    private fun advance() {
        amp = targets[pos] shl AMP_SHIFT
        pos = minOf(pos + 1, durations.size - 1)
        threshold = (durations[pos].toLong() * length / DURATION_SCALE).toInt()
        delta =
            if (threshold > ticks) {
                ((targets[pos] shl AMP_SHIFT) - amp) / (threshold - ticks)
            } else {
                0
            }
    }

    companion object {
        private const val AMP_SHIFT = 15
        private const val DURATION_SCALE = 65536L

        /** Generator with `Q(E) = segments` (rule 2). */
        fun of(
            startValue: Int,
            segments: List<Segment>,
            length: Int,
        ): EnvelopeGenerator =
            EnvelopeGenerator(
                startValue,
                IntArray(segments.size) { segments[it].duration },
                IntArray(segments.size) { segments[it].targetLevel },
                length,
            )

        /** Generator for an envelope other than the frequency or amplitude envelope (rule 2). */
        fun of(
            envelope: Envelope,
            length: Int,
        ): EnvelopeGenerator = of(envelope.startValue, envelope.segments, length)

        /** Generator for the frequency or amplitude envelope of [voice] (rules 1 and 2). */
        fun ofBase(
            envelope: Envelope,
            voice: Voice,
            length: Int,
        ): EnvelopeGenerator =
            if (envelope.segments.isEmpty() && envelope.startValue != envelope.endValue) {
                EnvelopeGenerator(
                    envelope.startValue,
                    intArrayOf(voice.duration.value),
                    intArrayOf(envelope.endValue),
                    length,
                )
            } else {
                of(envelope, length)
            }
    }
}
