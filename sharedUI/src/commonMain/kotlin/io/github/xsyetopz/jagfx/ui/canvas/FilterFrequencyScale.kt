package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

/**
 * Log-frequency axis of the filter canvases: the eight octaves from C1 to C9 that a pole phase
 * can reach.
 *
 * A normalized x of 0 is [MIN_FREQUENCY_HZ] and 1 is [MAX_FREQUENCY_HZ].
 * A raw pole phase `p` is the frequency `BASE_FREQUENCY_HZ * 2^(p * RAW_PHASE_TO_OCTAVES)`, so x is
 * `p / 65536`.
 */
internal object FilterFrequencyScale {
    const val SAMPLE_RATE: Double = 22050.0
    const val BASE_FREQUENCY_HZ: Double = 32.703197
    const val OCTAVES: Int = 8
    const val MIN_FREQUENCY_HZ: Double = BASE_FREQUENCY_HZ
    const val MAX_FREQUENCY_HZ: Double = BASE_FREQUENCY_HZ * (1 shl OCTAVES)
    const val RAW_PHASE_TO_OCTAVES: Double = 1.2207031e-4
    private val logFrequencyRange = ln(MAX_FREQUENCY_HZ / MIN_FREQUENCY_HZ)

    /** Position of [frequencyHz] on the axis, clamped to 0..1. */
    fun frequencyToNormalizedX(frequencyHz: Double): Double {
        val clamped = frequencyHz.coerceIn(MIN_FREQUENCY_HZ, MAX_FREQUENCY_HZ)
        return (ln(clamped / MIN_FREQUENCY_HZ) / logFrequencyRange).coerceIn(0.0, 1.0)
    }

    /** Frequency at [normalizedX], clamped to 0..1. */
    fun normalizedXToFrequency(normalizedX: Double): Double =
        MIN_FREQUENCY_HZ * exp(normalizedX.coerceIn(0.0, 1.0) * logFrequencyRange)

    /** Frequency of a raw pole phase, clamped to the u16 range. */
    fun rawPhaseToFrequency(rawPhase: Int): Double =
        BASE_FREQUENCY_HZ *
            2.0.pow(rawPhase.coerceIn(0, SynthLimits.U16_MAX) * RAW_PHASE_TO_OCTAVES)

    /** Position of a raw pole phase on the axis. */
    fun rawPhaseToNormalizedX(rawPhase: Int): Double =
        frequencyToNormalizedX(rawPhaseToFrequency(rawPhase))

    /** Raw pole phase nearest to [normalizedX], with ties to even. */
    fun normalizedXToRawPhase(normalizedX: Double): Int {
        val octaves = log2(normalizedXToFrequency(normalizedX) / BASE_FREQUENCY_HZ)
        return round(octaves / RAW_PHASE_TO_OCTAVES).toInt().coerceIn(0, SynthLimits.U16_MAX)
    }
}
