package io.github.xsyetopz.jagfx.ui.canvas

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

/** Equal-tempered semitone grid over the [FilterFrequencyScale] axis, with C1 as semitone 0. */
internal object SemitoneScale {
    const val SEMITONES_PER_OCTAVE: Int = 12

    private const val MIN_HZ = FilterFrequencyScale.MIN_FREQUENCY_HZ
    private const val MAX_HZ = FilterFrequencyScale.MAX_FREQUENCY_HZ
    private const val BASE_HZ = FilterFrequencyScale.BASE_FREQUENCY_HZ

    /** Frequency of [semitone] semitones above [FilterFrequencyScale.BASE_FREQUENCY_HZ] (C1). */
    fun semitoneToFrequency(semitone: Int): Double =
        BASE_HZ * 2.0.pow(semitone / SEMITONES_PER_OCTAVE.toDouble())

    /** Lowest semitone that can fall on the axis. */
    val firstVisibleSemitone: Int =
        floor(SEMITONES_PER_OCTAVE * log2(MIN_HZ / BASE_HZ)).toInt()

    /** Highest semitone that can fall on the axis. */
    val lastVisibleSemitone: Int =
        ceil(SEMITONES_PER_OCTAVE * log2(MAX_HZ / BASE_HZ)).toInt()

    /** [rawPhase] moved to the nearest semitone. */
    fun snapRawPhaseToSemitone(rawPhase: Int): Int {
        val octaves = log2(FilterFrequencyScale.rawPhaseToFrequency(rawPhase) / BASE_HZ)
        val semitone = round(SEMITONES_PER_OCTAVE * octaves).toInt()
        return FilterFrequencyScale.normalizedXToRawPhase(
            FilterFrequencyScale.frequencyToNormalizedX(semitoneToFrequency(semitone)),
        )
    }
}
