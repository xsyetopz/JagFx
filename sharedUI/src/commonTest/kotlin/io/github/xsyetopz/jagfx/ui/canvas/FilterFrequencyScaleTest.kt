package io.github.xsyetopz.jagfx.ui.canvas

import kotlin.test.Test
import kotlin.test.assertEquals

class FilterFrequencyScaleTest {
    @Test
    fun normalizedXRoundTripsThroughFrequency() {
        for (x in listOf(0.0, 0.1, 0.5, 0.9, 1.0)) {
            val f = FilterFrequencyScale.normalizedXToFrequency(x)
            assertEquals(x, FilterFrequencyScale.frequencyToNormalizedX(f), 1e-12)
        }
        assertEquals(32.703197, FilterFrequencyScale.normalizedXToFrequency(-1.0), 1e-9)
        assertEquals(8372.018, FilterFrequencyScale.normalizedXToFrequency(2.0), 1e-3)
    }

    @Test
    fun axisSpansTheEightOctavesOfThePhaseFromC1ToC9() {
        assertEquals(32.703, FilterFrequencyScale.rawPhaseToFrequency(0), 1e-3)
        assertEquals(8372.0, FilterFrequencyScale.rawPhaseToFrequency(65535), 1.0)
        assertEquals(0.0, FilterFrequencyScale.rawPhaseToNormalizedX(0), 1e-9)
        assertEquals(0.5, FilterFrequencyScale.rawPhaseToNormalizedX(32768), 1e-6)
        assertEquals(32768, FilterFrequencyScale.normalizedXToRawPhase(0.5))
    }

    @Test
    fun rawPhaseRoundTripsThroughNormalizedX() {
        for (raw in listOf(0, 1000, 32768, 60000, 65535)) {
            val x = FilterFrequencyScale.rawPhaseToNormalizedX(raw)
            val expected =
                FilterFrequencyScale.normalizedXToRawPhase(
                    FilterFrequencyScale.frequencyToNormalizedX(
                        FilterFrequencyScale.rawPhaseToFrequency(raw),
                    ),
                )
            assertEquals(expected, FilterFrequencyScale.normalizedXToRawPhase(x))
        }
        // Every raw phase lies on the axis, so it round trips.
        assertEquals(
            32768,
            FilterFrequencyScale.normalizedXToRawPhase(
                FilterFrequencyScale.rawPhaseToNormalizedX(32768),
            ),
        )
        assertEquals(0, FilterFrequencyScale.normalizedXToRawPhase(0.0))
        assertEquals(65535, FilterFrequencyScale.normalizedXToRawPhase(1.0))
    }

    @Test
    fun snapsRawPhaseToTheNearestSemitone() {
        val a4 = SemitoneScale.semitoneToFrequency(45)
        assertEquals(440.0, a4, 0.01)
        val snapped = SemitoneScale.snapRawPhaseToSemitone(37000)
        val semitones =
            12 *
                kotlin.math.log2(
                    FilterFrequencyScale.rawPhaseToFrequency(snapped) /
                        FilterFrequencyScale.BASE_FREQUENCY_HZ,
                )
        assertEquals(kotlin.math.round(semitones), semitones, 1e-3)
    }
}
