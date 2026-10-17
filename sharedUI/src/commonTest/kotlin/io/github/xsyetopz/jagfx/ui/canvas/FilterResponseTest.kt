package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The response axis runs from C1 to C9, the frequencies a pole phase can reach.
private const val C1 = 32.703197
private const val C9 = C1 * 256

class FilterResponseTest {
    private fun pole(
        phase: Int,
        magnitude: Int,
        endPhase: Int = phase,
        endMagnitude: Int = magnitude,
    ) = Pole(Coefficient(phase, magnitude), Coefficient(endPhase, endMagnitude))

    private fun filter(
        feedforward: List<Pole> = emptyList(),
        feedback: List<Pole> = emptyList(),
    ) = Filter(0, 0, feedforward, feedback, null)

    private fun frequency(
        i: Int,
        points: Int,
    ): Double = 10.0.pow(log10(C1) + (log10(C9) - log10(C1)) * i / (points - 1))

    /** `|1 - r e^(i theta) z^-1| * |1 - r e^(-i theta) z^-1|` at `z = e^(i omega)`. */
    private fun sectionMagnitude(
        raw: Pole,
        omega: Double,
    ): Double {
        val r = FilterResponse.rawMagnitudeToRadius(raw.start.magnitude)
        val theta = FilterResponse.rawPhaseToAngle(raw.start.phase)
        val a = hypot(1 - r * cos(theta - omega), -r * sin(theta - omega))
        val b = hypot(1 - r * cos(-theta - omega), -r * sin(-theta - omega))
        return a * b
    }

    @Test
    fun noFilterAndNoPolesGiveZeroDecibels() {
        assertTrue(FilterResponse.magnitudeDb(null, 0.0, 64).all { it == 0.0 })
        val flat = FilterResponse.magnitudeDb(filter(), 0.0, 64)
        assertEquals(64, flat.size)
        assertTrue(flat.all { it == 0.0 })
    }

    @Test
    fun singleFeedbackPoleMatchesTheAnalyticResponse() {
        val p = pole(phase = 40000, magnitude = 30000)
        val points = 120
        val db = FilterResponse.magnitudeDb(filter(feedback = listOf(p)), 0.0, points)
        for (i in 0 until points) {
            val omega = 2 * PI * frequency(i, points) / 22050.0
            assertEquals(-20 * log10(sectionMagnitude(p, omega)), db[i], 1e-6)
        }
    }

    @Test
    fun singleFeedforwardPoleMatchesTheAnalyticResponse() {
        val p = pole(phase = 50000, magnitude = 20000)
        val points = 80
        val db = FilterResponse.magnitudeDb(filter(feedforward = listOf(p)), 0.0, points)
        for (i in 0 until points) {
            val omega = 2 * PI * frequency(i, points) / 22050.0
            assertEquals(20 * log10(sectionMagnitude(p, omega)), db[i], 1e-6)
        }
    }

    @Test
    fun interpolationFactorEndpointsUseStartAndEnd() {
        val moving = pole(phase = 30000, magnitude = 10000, endPhase = 50000, endMagnitude = 40000)
        val atStart = filter(feedback = listOf(pole(30000, 10000)))
        val atEnd = filter(feedback = listOf(pole(50000, 40000)))
        val modulated = filter(feedback = listOf(moving))
        assertTrue(
            FilterResponse.magnitudeDb(modulated, 0.0).contentEquals(
                FilterResponse.magnitudeDb(atStart, 0.0),
            ),
        )
        assertTrue(
            FilterResponse.magnitudeDb(modulated, 1.0).contentEquals(
                FilterResponse.magnitudeDb(atEnd, 1.0),
            ),
        )
    }

    @Test
    fun interpolationTruncatesRawValues() {
        // 10000 + 1 * 0.5 = 10000.5 truncates to 10000, the start value.
        val moving = filter(feedback = listOf(pole(30000, 10000, 30001, 10001)))
        val start = filter(feedback = listOf(pole(30000, 10000)))
        assertTrue(
            FilterResponse.magnitudeDb(moving, 0.5).contentEquals(
                FilterResponse.magnitudeDb(start, 0.0),
            ),
        )
    }

    @Test
    fun unityGainScalesTheResponse() {
        val gained = Filter(1000, 1000, emptyList(), listOf(pole(30000, 10000)), null)
        val plain = filter(feedback = listOf(pole(30000, 10000)))
        val a = FilterResponse.magnitudeDb(gained, 0.0, 16)
        val b = FilterResponse.magnitudeDb(plain, 0.0, 16)
        for (i in a.indices) assertEquals(b[i] - 1000 * 0.0030517578, a[i], 1e-9)
    }
}
