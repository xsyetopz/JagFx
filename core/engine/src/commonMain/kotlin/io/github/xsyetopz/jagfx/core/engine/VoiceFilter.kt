package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

/**
 * Pole-zero filter of a voice (spec §8.9). Every `fl32` operation is a [Double] operation followed
 * by [fround], so all targets, Kotlin/JS included, give the same `f32` results.
 */
internal object VoiceFilter {
    private const val FIXED_SCALE = 65536.0
    private const val MINUS_TWO = -2.0
    private const val UNMODULATED = 65536
    private const val Q16_SHIFT = 16
    private const val DB_DIVISOR = 20.0
    private const val TENTH = 0.1
    private const val C1_HZ = 32.703197
    private const val TWO_PI = 6.283185307179586
    private const val SAMPLE_RATE = 22050.0
    private const val MAGNITUDE_SCALE = 0.0015258789
    private const val PHASE_SCALE = 1.2207031e-4
    private const val GAIN_SCALE = 0.0030517578
    private const val DECIMAL_BASE = 10.0
    private val magnitudeScale = fround(MAGNITUDE_SCALE)
    private val phaseScale = fround(PHASE_SCALE)
    private val gainScale = fround(GAIN_SCALE)
    private val ln10 = ln(DECIMAL_BASE)

    fun apply(
        buf: IntArray,
        filter: Filter,
    ) {
        val length = buf.size
        val kf = 2 * filter.feedforward.size
        val kb = 2 * filter.feedback.size
        if (kf + kb == 0 || length < kf + kb) return

        val modulation = filter.modulationEnvelope?.let { EnvelopeGenerator.of(0, it, length) }
        val coefficients = Coefficients(filter)
        val input = buf.copyOf()
        for (n in 0 until length) {
            coefficients.update(modulation?.next() ?: UNMODULATED)
            val ff = coefficients.ff
            val fb = coefficients.fb
            var y = 0L
            if (n < length - kf) {
                y = (input[n + kf].toLong() * coefficients.invA0Q16) shr Q16_SHIFT
                for (k in 0 until kf) y += (input[n + kf - 1 - k].toLong() * ff[k]) shr Q16_SHIFT
            } else {
                for (k in n + kf - length until kf) {
                    y += (input[n + kf - 1 - k].toLong() * ff[k]) shr Q16_SHIFT
                }
            }
            for (k in 0 until min(n, kb)) y -= (buf[n - 1 - k].toLong() * fb[k]) shr Q16_SHIFT
            buf[n] = y.toInt()
        }
    }

    /** Fixed-point coefficients for one modulation value, recomputed when the value changes. */
    private class Coefficients(
        private val filter: Filter,
    ) {
        val ff = IntArray(2 * filter.feedforward.size)
        val fb = IntArray(2 * filter.feedback.size)
        var invA0Q16 = 0
            private set
        private val poly = DoubleArray(maxOf(ff.size, fb.size))
        private var value = 0
        private var computed = false
        private var t1 = 0.0
        private var t2 = 0.0

        fun update(v: Int) {
            if (computed && v == value) return
            computed = true
            value = v
            val x = fround(fround(v.toDouble()) / FIXED_SCALE)

            val gain = lerp(filter.unityGain0, filter.unityGain1, x)
            val gainDb = fround(gain * gainScale)
            invA0Q16 = fround(fround(TENTH.pow(gainDb / DB_DIVISOR)) * FIXED_SCALE).toInt()

            val invA0 = fround(fround(invA0Q16.toDouble()) / FIXED_SCALE)
            polynomial(filter.feedforward, x)
            for (k in ff.indices) ff[k] = fround(fround(poly[k] * invA0) * FIXED_SCALE).toInt()
            polynomial(filter.feedback, x)
            for (k in fb.indices) fb[k] = fround(poly[k] * FIXED_SCALE).toInt()
        }

        /** Writes the `f32` polynomial `c` of [poles] to `poly[0 until 2 * poles.size]`. */
        private fun polynomial(
            poles: List<Pole>,
            x: Double,
        ) {
            if (poles.isEmpty()) return
            val c = poly
            pole(poles[0], x)
            c[0] = t1
            c[1] = t2
            for (s in 1 until poles.size) {
                pole(poles[s], x)
                c[2 * s + 1] = fround(c[2 * s - 1] * t2)
                c[2 * s] = fround(fround(c[2 * s - 1] * t1) + fround(c[2 * s - 2] * t2))
                for (i in 2 * s - 1 downTo 2) {
                    c[i] = fround(c[i] + fround(fround(c[i - 1] * t1) + fround(c[i - 2] * t2)))
                }
                c[1] = fround(c[1] + fround(fround(c[0] * t1) + t2))
                c[0] = fround(c[0] + t1)
            }
        }

        /** Sets [t1] and [t2] of [pole] at modulation value [x]. */
        private fun pole(
            pole: Pole,
            x: Double,
        ) {
            val r = radius(pole, x)
            t1 = fround(fround(MINUS_TWO * r) * fround(cos(theta(pole, x))))
            t2 = fround(r * r)
        }

        private fun radius(
            pole: Pole,
            x: Double,
        ): Double {
            val m = lerp(pole.start.magnitude, pole.end.magnitude, x)
            val db = fround(m * magnitudeScale)
            return fround(1.0 - fround(exp(-db / DB_DIVISOR * ln10)))
        }

        private fun theta(
            pole: Pole,
            x: Double,
        ): Double {
            val psi = lerp(pole.start.phase, pole.end.phase, x)
            val octaves = fround(psi * phaseScale)
            return fround(2.0.pow(octaves) * C1_HZ * TWO_PI / SAMPLE_RATE)
        }

        private fun lerp(
            a: Int,
            b: Int,
            x: Double,
        ): Double {
            val fa = fround(a.toDouble())
            return fround(fa + fround(x * fround(fround(b.toDouble()) - fa)))
        }
    }
}
