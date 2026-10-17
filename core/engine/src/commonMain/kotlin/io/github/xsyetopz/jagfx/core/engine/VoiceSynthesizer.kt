package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

/** Voice synthesis (spec §8.5 to §8.10). */
internal object VoiceSynthesizer {
    private const val MIN_DURATION_MS = 10
    private const val PHASE_SCALE = 32.768
    private const val VOLUME_SHIFT = 14
    private const val PERCENT = 100
    private const val Q15_SHIFT = 15
    private const val Q16_SHIFT = 16
    private const val LFO_OFFSET = 32768
    private const val GATE_SHIFT = 8
    private const val GATE_STEP = 256

    /** The voice result: clamped samples, or an empty array for a voice that is too short. */
    fun synthesize(voice: Voice): IntArray {
        val duration = voice.duration.value
        val length = samplesOf(duration)
        if (duration < MIN_DURATION_MS || length <= 0) return IntArray(0)
        val scale = length.toDouble() / duration

        val buf = IntArray(length)
        oscillate(voice, buf, gateMask(voice.gate, length), scale)
        echo(voice, buf, scale)
        voice.filter?.let { VoiceFilter.apply(buf, it) }
        for (n in buf.indices) {
            buf[n] =
                buf[n].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        }
        return buf
    }

    private fun gateMask(
        gate: Gate?,
        length: Int,
    ): BooleanArray? {
        if (gate == null) return null
        val g0 = gate.gapOff
        val off = EnvelopeGenerator.of(g0, length)
        val on = EnvelopeGenerator.of(gate.gapOn, length)
        val open = BooleanArray(length)
        var muted = true
        var counter = 0
        for (n in 0 until length) {
            val v0 = off.next()
            val v1 = on.next()
            val threshold =
                g0.startValue +
                    (((g0.endValue - g0.startValue) * (if (muted) v0 else v1)) shr GATE_SHIFT)
            counter += GATE_STEP
            if (counter >= threshold) {
                counter = 0
                muted = !muted
            }
            open[n] = !muted
        }
        return open
    }

    private fun oscillate(
        voice: Voice,
        buf: IntArray,
        open: BooleanArray?,
        scale: Double,
    ) {
        val length = buf.size
        val frequency = voice.frequencyEnvelope
        val waveform = frequency.waveform
        val partials = voice.partials
        val count = partials.size
        val delay = IntArray(count) { (partials[it].delay.value * scale).toInt() }
        val volume = IntArray(count) { (partials[it].amplitude.value shl VOLUME_SHIFT) / PERCENT }
        val semitoneStep =
            IntArray(count) {
                (
                    frequency.span.toDouble() * PHASE_SCALE *
                        WaveTables.pitchRatio(partials[it].pitchOffset) / scale
                ).toInt()
            }
        val startStep = stepOf(frequency.startValue, scale)
        val phase = IntArray(count)

        val frequencyGen = EnvelopeGenerator.ofBase(frequency, voice, length)
        val amplitudeGen = EnvelopeGenerator.ofBase(voice.amplitudeEnvelope, voice, length)
        val pitchLfo = voice.pitchLfo?.let { LfoState(it, length, scale) }
        val amplitudeLfo = voice.amplitudeLfo?.let { LfoState(it, length, scale) }

        for (n in 0 until length) {
            var f = frequencyGen.next()
            var a = amplitudeGen.next()
            if (pitchLfo != null) f += pitchLfo.next() shr 1
            if (amplitudeLfo !=
                null
            ) {
                a = (a * ((amplitudeLfo.next() shr 1) + LFO_OFFSET)) shr Q15_SHIFT
            }
            for (k in 0 until count) {
                val j = n + delay[k]
                if (j >= length) continue
                if (open == null || open[j]) {
                    buf[j] += WaveTables.wave((a * volume[k]) shr Q15_SHIFT, phase[k], waveform)
                }
                phase[k] += ((f * semitoneStep[k]) shr Q16_SHIFT) + startStep
            }
        }
    }

    private fun echo(
        voice: Voice,
        buf: IntArray,
        scale: Double,
    ) {
        val delay = voice.echo.delay.value
        val feedback = voice.echo.feedback.value
        if (delay <= 0 || feedback <= 0) return
        val e = (delay * scale).toInt()
        for (n in e until buf.size) buf[n] += buf[n - e] * feedback / PERCENT
    }

    private fun stepOf(
        value: Int,
        scale: Double,
    ): Int = (value.toDouble() * PHASE_SCALE / scale).toInt()

    /** An active LFO pair. [next] gives `Wave(m, lfoPhase, Rt.Waveform)` and advances the phase. */
    private class LfoState(
        lfo: Lfo,
        length: Int,
        scale: Double,
    ) {
        private val waveform: Waveform = lfo.rate.waveform
        private val rate = EnvelopeGenerator.of(lfo.rate, length)
        private val depth = EnvelopeGenerator.of(lfo.depth, length)
        private val step = stepOf(lfo.rate.span, scale)
        private val base = stepOf(lfo.rate.startValue, scale)
        private var phase = 0

        fun next(): Int {
            val r = rate.next()
            val m = depth.next()
            val value = WaveTables.wave(m, phase, waveform)
            phase += base + ((r * step) shr Q16_SHIFT)
            return value
        }
    }

    private val Envelope.span: Int get() = endValue - startValue
}

private const val SAMPLE_RATE_HZ = 22050L
private const val MS_PER_SECOND = 1000L

/** `N(ms)`: the sample count of [ms] milliseconds (spec §8.1). */
internal fun samplesOf(ms: Int): Int = (ms * SAMPLE_RATE_HZ / MS_PER_SECOND).toInt()
