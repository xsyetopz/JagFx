package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Magnitude response of a [Filter].
 *
 * Interpolation runs on the raw integers and truncates them before conversion, as
 * `AudioFilter.GetAmplitude` does.
 */
internal object FilterResponse {
    const val DEFAULT_POINTS: Int = 200
    const val MAGNITUDE_DB_SCALE: Double = 0.0015258789
    const val GAIN_DB_SCALE: Double = 0.0030517578

    private const val MAX_COEFFICIENTS = 8
    private const val DECIBEL_FACTOR = 20.0
    private const val GAIN_BASE = 0.1
    private const val MIN_MAGNITUDE = 1e-10

    /**
     * Response in dB at [points] frequencies spaced logarithmically from C1 to C9, with
     * the poles interpolated from [Pole.start] (factor 0) to [Pole.end] (factor 1). All values
     * are 0 when [filter] is `null` or has no poles.
     */
    fun magnitudeDb(
        filter: Filter?,
        factor: Double,
        points: Int = DEFAULT_POINTS,
    ): DoubleArray {
        val response = DoubleArray(points)
        if (filter == null || (filter.feedforward.isEmpty() && filter.feedback.isEmpty())) {
            return response
        }
        val ff = coefficients(filter.feedforward, factor)
        val fb = coefficients(filter.feedback, factor)
        val ffOrder = filter.feedforward.size * 2
        val fbOrder = filter.feedback.size * 2
        val gain = filter.unityGain0 + (filter.unityGain1 - filter.unityGain0) * factor
        val inverseA0 = GAIN_BASE.pow(gain * GAIN_DB_SCALE / DECIBEL_FACTOR)
        val logMin = log10(FilterFrequencyScale.MIN_FREQUENCY_HZ)
        val logMax = log10(FilterFrequencyScale.MAX_FREQUENCY_HZ)
        for (i in 0 until points) {
            val frequency = 10.0.pow(logMin + (logMax - logMin) * i / (points - 1))
            val omega = 2.0 * PI * frequency / FilterFrequencyScale.SAMPLE_RATE
            val numerator = polynomialMagnitude(ff, ffOrder, omega)
            val denominator = polynomialMagnitude(fb, fbOrder, omega)
            val h = if (denominator > MIN_MAGNITUDE) inverseA0 * numerator / denominator else 0.0
            response[i] = DECIBEL_FACTOR * log10(max(h, MIN_MAGNITUDE))
        }
        return response
    }

    /** Z-plane radius of a raw pole magnitude. */
    fun rawMagnitudeToRadius(raw: Int): Double =
        1.0 - 10.0.pow(-(raw * MAGNITUDE_DB_SCALE) / DECIBEL_FACTOR)

    /** Z-plane angle in radians of a raw pole phase. */
    fun rawPhaseToAngle(raw: Int): Double {
        val frequency =
            2.0.pow(raw * FilterFrequencyScale.RAW_PHASE_TO_OCTAVES) *
                FilterFrequencyScale.BASE_FREQUENCY_HZ
        return frequency * 2.0 * PI / FilterFrequencyScale.SAMPLE_RATE
    }

    /** `|1 + sum c[k] z^-(k+1)|` for `k < order`, with `z^-1 = e^(-i omega)`. */
    private fun polynomialMagnitude(
        coefficients: DoubleArray,
        order: Int,
        omega: Double,
    ): Double {
        val zRe = cos(omega)
        val zIm = -sin(omega)
        var sumRe = 1.0
        var sumIm = 0.0
        var powRe = zRe
        var powIm = zIm
        for (k in 0 until order) {
            sumRe += coefficients[k] * powRe
            sumIm += coefficients[k] * powIm
            val nextRe = powRe * zRe - powIm * zIm
            powIm = powRe * zIm + powIm * zRe
            powRe = nextRe
        }
        return hypot(sumRe, sumIm)
    }

    /** The `z^-1` term `-2 r cos(theta)` of one second-order section. */
    private fun sectionTerm(
        radius: Double,
        theta: Double,
    ): Double = -(radius + radius) * cos(theta)

    /** Expanded product of the second-order sections `1 - 2 r cos(theta) z^-1 + r^2 z^-2`. */
    private fun coefficients(
        poles: List<Pole>,
        factor: Double,
    ): DoubleArray {
        val c = DoubleArray(MAX_COEFFICIENTS)
        if (poles.isEmpty()) return c
        val first = poles[0]
        val amp = radius(first, factor)
        val phase = angle(first, factor)
        c[0] = sectionTerm(amp, phase)
        c[1] = amp * amp
        for (section in 1 until poles.size) {
            val ampP = radius(poles[section], factor)
            val phaseP = angle(poles[section], factor)
            val term1 = sectionTerm(ampP, phaseP)
            val term2 = ampP * ampP
            c[section * 2 + 1] = c[section * 2 - 1] * term2
            c[section * 2] = c[section * 2 - 1] * term1 + c[section * 2 - 2] * term2
            for (k in section * 2 - 1 downTo 2) {
                c[k] += c[k - 1] * term1 + c[k - 2] * term2
            }
            c[1] += c[0] * term1 + term2
            c[0] += term1
        }
        return c
    }

    private fun radius(
        pole: Pole,
        factor: Double,
    ): Double {
        val raw0 = pole.start.magnitude
        val raw1 = pole.end.magnitude
        return rawMagnitudeToRadius((raw0 + (raw1 - raw0) * factor).toInt())
    }

    private fun angle(
        pole: Pole,
        factor: Double,
    ): Double {
        val raw0 = pole.start.phase
        val raw1 = pole.end.phase
        return rawPhaseToAngle((raw0 + (raw1 - raw0) * factor).toInt())
    }
}
