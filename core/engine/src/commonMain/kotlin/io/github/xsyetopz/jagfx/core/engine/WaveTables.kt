package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.math.pow
import kotlin.math.sin

/** The 48-bit linear congruential generator of `java.util.Random` (spec §8.2). */
internal class JavaLcg(
    seed: Long,
) {
    private var state: Long = (seed xor MULTIPLIER) and MASK

    /** `java.util.Random.nextInt()`. */
    fun nextInt(): Int {
        state = (state * MULTIPLIER + INCREMENT) and MASK
        return (state ushr STATE_SHIFT).toInt()
    }

    private companion object {
        const val MULTIPLIER = 0x5DEECE66DL
        const val INCREMENT = 0xBL
        const val MASK = (1L shl 48) - 1
        const val STATE_SHIFT = 16
    }
}

/** Oscillator tables and the oscillator function (spec §8.2, §8.3). */
internal object WaveTables {
    const val SIZE: Int = 32768
    const val PHASE_MASK: Int = SIZE - 1

    private const val SINE_DIVISOR = 5215.1903
    private const val SINE_SCALE = 16384
    private const val HALF_CYCLE = 16384
    private const val AMPLITUDE_SHIFT = 14
    private const val NOISE_PHASE_DIVISOR = 2607
    private const val NOISE_SEED = 0L
    private const val NOISE_BIT = 2
    private const val PITCH_RATIO = 1.0057929410678534

    val sine: IntArray = IntArray(SIZE) { i -> (sin(i / SINE_DIVISOR) * SINE_SCALE).toInt() }

    val noise: IntArray =
        JavaLcg(NOISE_SEED).let { lcg -> IntArray(SIZE) { (lcg.nextInt() and NOISE_BIT) - 1 } }

    /** `mult(k)`: the frequency ratio of a pitch offset of [k] tenths of a semitone. */
    fun pitchRatio(k: Int): Double = PITCH_RATIO.pow(k.toDouble())

    /** `Wave(A, ph, w)`. */
    fun wave(
        amplitude: Int,
        phase: Int,
        waveform: Waveform,
    ): Int =
        when (waveform) {
            Waveform.Square -> if (phase and PHASE_MASK < HALF_CYCLE) amplitude else -amplitude

            Waveform.Sine -> (sine[phase and PHASE_MASK] * amplitude) shr AMPLITUDE_SHIFT

            Waveform.Saw -> (((phase and PHASE_MASK) * amplitude) shr AMPLITUDE_SHIFT) - amplitude

            Waveform.Noise -> noise[(phase / NOISE_PHASE_DIVISOR) and PHASE_MASK] * amplitude

            // Off, and every id the game client defines no shape for.
            else -> 0
        }
}
